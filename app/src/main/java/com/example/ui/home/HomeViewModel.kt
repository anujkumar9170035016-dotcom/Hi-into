package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.contacts.DeviceContactsReader
import com.example.data.network.ConnectivityObserver
import com.example.data.onboarding.OnboardingStore
import com.example.data.repository.MessagingRepository
import com.example.data.repository.ServiceLocator
import com.example.data.telephony.NativeTelephonyBridge
import com.example.data.telephony.TelephonyDispatchResult
import com.example.domain.model.CallLog
import com.example.domain.model.CallType
import com.example.domain.model.ChatFilter
import com.example.domain.model.ChatThread
import com.example.domain.model.Contact
import com.example.domain.model.StatusSlide
import com.example.domain.model.StatusStory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class HomeTab(val label: String) {
    CHATS("Chats"),
    UPDATES("Updates"),
    CALLS("Calls")
}

data class HomeUiState(
    val selectedTab: HomeTab = HomeTab.CHATS,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val selectedFilter: ChatFilter = ChatFilter.ALL,
    val chats: List<ChatThread> = emptyList(),
    val archivedChats: List<ChatThread> = emptyList(),
    val totalUnreadChats: Int = 0,
    val contacts: List<Contact> = emptyList(),
    val myStatus: StatusStory? = null,
    val recentStatuses: List<StatusStory> = emptyList(),
    val viewedStatuses: List<StatusStory> = emptyList(),
    val callLogs: List<CallLog> = emptyList(),
    val selectedDetailChatId: String? = null,
    val isOnline: Boolean = true,
    val isSimulatedOffline: Boolean = false,
    val verifiedPhoneNumber: String? = null,
    val hasContactsPermission: Boolean = false
)

class HomeViewModel(
    private val repository: MessagingRepository = ServiceLocator.messagingRepository,
    private val connectivityObserver: ConnectivityObserver = ServiceLocator.connectivityObserver,
    private val telephonyBridge: NativeTelephonyBridge = ServiceLocator.telephonyBridge,
    private val onboardingStore: OnboardingStore = ServiceLocator.onboardingStore,
    private val deviceContactsReader: DeviceContactsReader = ServiceLocator.deviceContactsReader
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(HomeTab.CHATS)
    private val _isSearchActive = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow(ChatFilter.ALL)
    private val _selectedDetailChatId = MutableStateFlow<String?>(null)
    private val _hasContactsPermission = MutableStateFlow(deviceContactsReader.hasContactsPermission())

    private val onboardingMeta = combine(
        onboardingStore.verifiedPhoneNumber,
        _hasContactsPermission
    ) { verifiedPhone, hasPerm ->
        verifiedPhone to hasPerm
    }

    val uiState: StateFlow<HomeUiState> = combine(
        _selectedTab,
        _isSearchActive,
        _searchQuery,
        _selectedFilter,
        repository.chats,
        repository.contacts,
        repository.statusStories,
        repository.callLogs,
        _selectedDetailChatId,
        connectivityObserver.isOnline,
        connectivityObserver.simulatedOffline,
        onboardingMeta
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val tab = values[0] as HomeTab
        val searchActive = values[1] as Boolean
        val query = values[2] as String
        val filter = values[3] as ChatFilter
        val allChats = values[4] as List<ChatThread>
        val contacts = values[5] as List<Contact>
        val statuses = values[6] as List<StatusStory>
        val calls = values[7] as List<CallLog>
        val detailChatId = values[8] as String?
        val isOnline = values[9] as Boolean
        val simulatedOffline = values[10] as Boolean
        @Suppress("UNCHECKED_CAST")
        val onboardingPair = values[11] as Pair<String?, Boolean>

        val q = query.trim().lowercase()
        val activeChats = allChats.filter { !it.isArchived }
        val archived = allChats.filter { it.isArchived }

        val filteredChats = activeChats.filter { thread ->
            val matchesFilter = when (filter) {
                ChatFilter.ALL -> true
                ChatFilter.UNREAD -> thread.unreadCount > 0
                ChatFilter.FAVORITES -> thread.contact.isFavorite
                ChatFilter.GROUPS -> thread.isGroup
            }
            val matchesQuery = q.isEmpty() ||
                thread.contact.name.lowercase().contains(q) ||
                thread.lastMessagePreview.lowercase().contains(q)
            matchesFilter && matchesQuery
        }

        val myStatus = statuses.firstOrNull { it.isMine }
        val friendStatuses = statuses.filter { !it.isMine }.filter { story ->
            q.isEmpty() || story.contact.name.lowercase().contains(q)
        }

        val filteredCalls = calls.filter { log ->
            q.isEmpty() || log.contact.name.lowercase().contains(q)
        }

        HomeUiState(
            selectedTab = tab,
            isSearchActive = searchActive,
            searchQuery = query,
            selectedFilter = filter,
            chats = filteredChats,
            archivedChats = archived,
            totalUnreadChats = activeChats.count { it.unreadCount > 0 },
            contacts = contacts,
            myStatus = myStatus,
            recentStatuses = friendStatuses.filter { !it.isViewed },
            viewedStatuses = friendStatuses.filter { it.isViewed },
            callLogs = filteredCalls,
            selectedDetailChatId = detailChatId,
            isOnline = isOnline,
            isSimulatedOffline = simulatedOffline,
            verifiedPhoneNumber = onboardingPair.first,
            hasContactsPermission = onboardingPair.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun selectTab(tab: HomeTab) {
        _selectedTab.value = tab
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) _searchQuery.value = ""
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectFilter(filter: ChatFilter) {
        _selectedFilter.value = filter
    }

    fun selectDetailChat(chatId: String?) {
        _selectedDetailChatId.value = chatId
        if (chatId != null) {
            viewModelScope.launch { repository.markChatAsRead(chatId) }
        }
    }

    fun toggleSimulatedOffline() {
        connectivityObserver.setSimulatedOffline(!connectivityObserver.simulatedOffline.value)
    }

    fun syncDeviceContactsFromPermission(granted: Boolean, onComplete: (Int) -> Unit = {}) {
        val effectiveGranted = granted || deviceContactsReader.hasContactsPermission()
        _hasContactsPermission.value = effectiveGranted
        viewModelScope.launch {
            onboardingStore.updateContactsGranted(effectiveGranted)
            if (effectiveGranted) {
                val entries = deviceContactsReader.readSavedPhoneContacts()
                val matchedCount = repository.syncDeviceContacts(entries)
                onComplete(matchedCount)
            } else {
                onComplete(0)
            }
        }
    }

    fun refreshContactsIfPermitted() {
        val granted = deviceContactsReader.hasContactsPermission()
        _hasContactsPermission.value = granted
        if (granted) {
            viewModelScope.launch {
                val entries = deviceContactsReader.readSavedPhoneContacts()
                repository.syncDeviceContacts(entries)
            }
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            onboardingStore.resetOnboarding()
        }
    }

    fun handleCallRequest(
        contactId: String,
        requestedType: CallType,
        onInternetCall: (String, CallType) -> Unit,
        onNotice: (String) -> Unit
    ) {
        if (uiState.value.isOnline) {
            onInternetCall(contactId, requestedType)
            return
        }
        if (requestedType == CallType.VIDEO) {
            onNotice("Video calls require internet. Placing standard cellular call instead.")
        }
        val contact = repository.getContactById(contactId)
            ?: repository.chats.value.firstOrNull { it.contact.id == contactId }?.contact
            ?: return
        viewModelScope.launch {
            repository.logCall(contact, CallType.CELLULAR, 0)
        }
        when (val result = telephonyBridge.launchCellularDialer(contact.phoneNumber)) {
            TelephonyDispatchResult.Launched -> Unit
            is TelephonyDispatchResult.NoHandlerApp ->
                onNotice("Cellular dialer (tel:${result.targetLabel}) — no dialer app found on device")
            is TelephonyDispatchResult.InvalidDestination ->
                onNotice(result.reason)
        }
    }

    fun markChatAsRead(chatId: String) {
        viewModelScope.launch { repository.markChatAsRead(chatId) }
    }

    fun markAllChatsAsRead() {
        viewModelScope.launch { repository.markAllChatsAsRead() }
    }

    fun togglePinChat(chatId: String) {
        viewModelScope.launch { repository.togglePinChat(chatId) }
    }

    fun toggleMuteChat(chatId: String) {
        viewModelScope.launch { repository.toggleMuteChat(chatId) }
    }

    fun toggleArchiveChat(chatId: String) {
        viewModelScope.launch { repository.toggleArchiveChat(chatId) }
    }

    fun markStatusViewed(statusId: String) {
        viewModelScope.launch { repository.markStatusViewed(statusId) }
    }

    fun addTextStatus(text: String, backgroundColorHex: Long) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val now = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        viewModelScope.launch {
            repository.addMyStatus(
                StatusSlide.TextCard(
                    backgroundColorHex = backgroundColorHex,
                    caption = trimmed,
                    timeAgo = "Just now ($now)"
                )
            )
        }
    }

    fun addPhotoStatus(uriString: String, caption: String) {
        viewModelScope.launch {
            repository.addMyStatus(
                StatusSlide.Photo(
                    uriString = uriString,
                    caption = caption.ifBlank { "Shared a photo" },
                    timeAgo = "Just now"
                )
            )
        }
    }

    fun openChatWithContact(contact: Contact, onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            val chatId = repository.openOrCreateChat(contact)
            onChatReady(chatId)
        }
    }

    fun replyToStatus(contact: Contact, replyText: String, onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            val chatId = repository.openOrCreateChat(contact)
            repository.sendTextMessage(
                chatId = chatId,
                text = replyText,
                replyTo = com.example.domain.model.ReplyPreview(
                    messageId = "status_reply",
                    senderName = "${contact.name} • Status",
                    snippet = "Status update",
                    isFromMe = false
                )
            )
            onChatReady(chatId)
        }
    }
}
