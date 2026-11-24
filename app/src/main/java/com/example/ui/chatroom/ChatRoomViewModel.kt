package com.example.ui.chatroom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.network.ConnectivityObserver
import com.example.data.repository.MessagingRepository
import com.example.data.repository.ServiceLocator
import com.example.data.telephony.NativeTelephonyBridge
import com.example.data.telephony.TelephonyDispatchResult
import com.example.domain.model.CallType
import com.example.domain.model.ChatMessage
import com.example.domain.model.ChatThread
import com.example.domain.model.MessageContent
import com.example.domain.model.MessageTransport
import com.example.domain.model.ReplyPreview
import com.example.domain.model.UserPresence
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

data class ChatRoomUiState(
    val chatId: String = "",
    val thread: ChatThread? = null,
    val messages: List<ChatMessage> = emptyList(),
    val presence: UserPresence = UserPresence.LastSeen("recently"),
    val isOnline: Boolean = true,
    val isSimulatedOffline: Boolean = false,
    val inputText: String = "",
    val replyingTo: ReplyPreview? = null,
    val isEmojiPickerOpen: Boolean = false,
    val isRecordingVoice: Boolean = false,
    val recordingSeconds: Int = 0,
    val recordingWaveform: List<Float> = emptyList(),
    val playingVoiceMessageId: String? = null,
    val voicePlayProgress: Float = 0f,
    val selectedMessageForActions: ChatMessage? = null,
    val isInChatSearchActive: Boolean = false,
    val inChatSearchQuery: String = ""
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ChatRoomViewModel(
    private val repository: MessagingRepository = ServiceLocator.messagingRepository,
    private val connectivityObserver: ConnectivityObserver = ServiceLocator.connectivityObserver,
    private val telephonyBridge: NativeTelephonyBridge = ServiceLocator.telephonyBridge
) : ViewModel() {

    private val _chatId = MutableStateFlow("")
    private val _inputText = MutableStateFlow("")
    private val _replyingTo = MutableStateFlow<ReplyPreview?>(null)
    private val _isEmojiPickerOpen = MutableStateFlow(false)
    private val _isRecordingVoice = MutableStateFlow(false)
    private val _recordingSeconds = MutableStateFlow(0)
    private val _recordingWaveform = MutableStateFlow<List<Float>>(emptyList())
    private val _playingVoiceMessageId = MutableStateFlow<String?>(null)
    private val _voicePlayProgress = MutableStateFlow(0f)
    private val _selectedMessage = MutableStateFlow<ChatMessage?>(null)
    private val _isInChatSearchActive = MutableStateFlow(false)
    private val _inChatSearchQuery = MutableStateFlow("")

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private var recordingJob: Job? = null
    private var voicePlayJob: Job? = null

    private val messagesFlow = _chatId.flatMapLatest { id ->
        if (id.isEmpty()) flowOf(emptyList()) else repository.observeMessages(id)
    }

    private val presenceFlow = _chatId.flatMapLatest { id ->
        if (id.isEmpty()) flowOf(UserPresence.LastSeen("recently"))
        else repository.observePresence(id)
    }

    val uiState: StateFlow<ChatRoomUiState> = combine(
        _chatId,
        repository.chats,
        messagesFlow,
        presenceFlow,
        connectivityObserver.isOnline,
        connectivityObserver.simulatedOffline,
        _inputText,
        _replyingTo,
        _isEmojiPickerOpen,
        _isRecordingVoice,
        _recordingSeconds,
        _recordingWaveform,
        _playingVoiceMessageId,
        _voicePlayProgress,
        _selectedMessage,
        _isInChatSearchActive,
        _inChatSearchQuery
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val id = values[0] as String
        val chats = values[1] as List<ChatThread>
        val rawMessages = values[2] as List<ChatMessage>
        val presence = values[3] as UserPresence
        val isOnline = values[4] as Boolean
        val simulatedOffline = values[5] as Boolean
        val input = values[6] as String
        val reply = values[7] as ReplyPreview?
        val emojiOpen = values[8] as Boolean
        val recording = values[9] as Boolean
        val recSecs = values[10] as Int
        val recWave = values[11] as List<Float>
        val playingId = values[12] as String?
        val playProgress = values[13] as Float
        val selectedMsg = values[14] as ChatMessage?
        val searchActive = values[15] as Boolean
        val searchQuery = values[16] as String

        val q = searchQuery.trim().lowercase()
        val filteredMessages = if (q.isEmpty()) rawMessages else {
            rawMessages.filter { msg ->
                when (val c = msg.content) {
                    is MessageContent.Text -> c.body.lowercase().contains(q)
                    is MessageContent.Image -> c.caption.orEmpty().lowercase().contains(q)
                    is MessageContent.Video -> c.caption.orEmpty().lowercase().contains(q)
                    is MessageContent.Document -> c.fileName.lowercase().contains(q)
                    is MessageContent.VoiceNote -> "voice".contains(q)
                }
            }
        }

        ChatRoomUiState(
            chatId = id,
            thread = chats.find { it.id == id },
            messages = filteredMessages,
            presence = presence,
            isOnline = isOnline,
            isSimulatedOffline = simulatedOffline,
            inputText = input,
            replyingTo = reply,
            isEmojiPickerOpen = emojiOpen,
            isRecordingVoice = recording,
            recordingSeconds = recSecs,
            recordingWaveform = recWave,
            playingVoiceMessageId = playingId,
            voicePlayProgress = playProgress,
            selectedMessageForActions = selectedMsg,
            isInChatSearchActive = searchActive,
            inChatSearchQuery = searchQuery
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChatRoomUiState()
    )

    fun loadChat(chatId: String) {
        if (_chatId.value == chatId) return
        _chatId.value = chatId
        _replyingTo.value = null
        _isEmojiPickerOpen.value = false
        _isInChatSearchActive.value = false
        _inChatSearchQuery.value = ""
        viewModelScope.launch { repository.markChatAsRead(chatId) }
    }

    fun toggleSimulatedOffline() {
        val next = !connectivityObserver.simulatedOffline.value
        connectivityObserver.setSimulatedOffline(next)
        if (next) {
            _isEmojiPickerOpen.value = false
            cancelVoiceRecording()
        }
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun toggleEmojiPicker() {
        _isEmojiPickerOpen.value = !_isEmojiPickerOpen.value
    }

    fun closeEmojiPicker() {
        _isEmojiPickerOpen.value = false
    }

    fun appendEmoji(emoji: String) {
        _inputText.value += emoji
    }

    fun sendTextMessage() {
        val id = _chatId.value
        val body = _inputText.value.trim()
        if (id.isEmpty() || body.isEmpty()) return
        val reply = _replyingTo.value
        val online = uiState.value.isOnline
        val contactPhone = uiState.value.thread?.contact?.phoneNumber.orEmpty()

        _inputText.value = ""
        _replyingTo.value = null

        viewModelScope.launch {
            if (online) {
                repository.sendTextMessage(id, body, reply, MessageTransport.INTERNET)
            } else {
                repository.sendTextMessage(id, body, reply, MessageTransport.SMS)
                when (val result = telephonyBridge.launchSmsComposer(contactPhone, body)) {
                    TelephonyDispatchResult.Launched -> {
                        _snackbarEvents.tryEmit("Opening SMS composer for $contactPhone")
                    }
                    is TelephonyDispatchResult.NoHandlerApp -> {
                        _snackbarEvents.tryEmit("Sent as SMS fallback (no default SMS app found for ${result.targetLabel})")
                    }
                    is TelephonyDispatchResult.InvalidDestination -> {
                        _snackbarEvents.tryEmit("Saved locally • ${result.reason}")
                    }
                }
            }
        }
    }

    fun handleCallClick(
        requestedType: CallType,
        onInternetCall: (String, CallType) -> Unit
    ) {
        val thread = uiState.value.thread ?: return
        val contact = thread.contact
        if (uiState.value.isOnline) {
            onInternetCall(contact.id, requestedType)
            return
        }

        if (requestedType == CallType.VIDEO) {
            _snackbarEvents.tryEmit("Video calls require internet. Tap voice call for cellular (tel:).")
            return
        }

        viewModelScope.launch {
            repository.logCall(contact, CallType.CELLULAR, 0)
        }
        when (val result = telephonyBridge.launchCellularDialer(contact.phoneNumber)) {
            TelephonyDispatchResult.Launched -> {
                _snackbarEvents.tryEmit("Dialing ${contact.phoneNumber} via cellular network")
            }
            is TelephonyDispatchResult.NoHandlerApp -> {
                _snackbarEvents.tryEmit("Cellular call (tel:${result.targetLabel}) — no phone dialer app found")
            }
            is TelephonyDispatchResult.InvalidDestination -> {
                _snackbarEvents.tryEmit(result.reason)
            }
        }
    }

    fun notifyOfflineMediaUnavailable() {
        _snackbarEvents.tryEmit("Offline mode: media & voice notes require internet. Text SMS is active.")
    }

    fun sendImageUri(uriString: String, caption: String? = null) {
        val id = _chatId.value
        if (id.isEmpty()) return
        if (!uiState.value.isOnline) {
            notifyOfflineMediaUnavailable()
            return
        }
        val reply = _replyingTo.value
        _replyingTo.value = null
        viewModelScope.launch {
            repository.sendAttachmentMessage(
                chatId = id,
                content = MessageContent.Image(caption = caption, uriString = uriString),
                replyTo = reply
            )
        }
    }

    fun sendVideoUri(uriString: String, caption: String? = null) {
        val id = _chatId.value
        if (id.isEmpty()) return
        if (!uiState.value.isOnline) {
            notifyOfflineMediaUnavailable()
            return
        }
        val reply = _replyingTo.value
        _replyingTo.value = null
        viewModelScope.launch {
            repository.sendAttachmentMessage(
                chatId = id,
                content = MessageContent.Video(caption = caption, uriString = uriString),
                replyTo = reply
            )
        }
    }

    fun sendDocument(fileName: String, fileSize: String, extension: String, uriString: String?) {
        val id = _chatId.value
        if (id.isEmpty()) return
        if (!uiState.value.isOnline) {
            notifyOfflineMediaUnavailable()
            return
        }
        val reply = _replyingTo.value
        _replyingTo.value = null
        viewModelScope.launch {
            repository.sendAttachmentMessage(
                chatId = id,
                content = MessageContent.Document(
                    fileName = fileName,
                    fileSize = fileSize,
                    extension = extension,
                    uriString = uriString
                ),
                replyTo = reply
            )
        }
    }

    fun sendQuickLocationNote() {
        val id = _chatId.value
        if (id.isEmpty()) return
        val reply = _replyingTo.value
        _replyingTo.value = null
        val transport = if (uiState.value.isOnline) MessageTransport.INTERNET else MessageTransport.SMS
        viewModelScope.launch {
            repository.sendTextMessage(
                chatId = id,
                text = "📍 Shared Location: Ferry Building Marketplace, San Francisco",
                replyTo = reply,
                transport = transport
            )
        }
    }

    fun startVoiceRecording() {
        if (!uiState.value.isOnline) {
            notifyOfflineMediaUnavailable()
            return
        }
        if (_isRecordingVoice.value) return
        _isRecordingVoice.value = true
        _recordingSeconds.value = 0
        _recordingWaveform.value = listOf(0.35f, 0.55f, 0.45f)
        recordingJob?.cancel()
        recordingJob = viewModelScope.launch {
            var tick = 0
            while (_isRecordingVoice.value) {
                delay(200)
                tick++
                if (tick % 5 == 0) {
                    _recordingSeconds.value += 1
                }
                val amplitude = 0.22f + Random.nextFloat() * 0.72f
                _recordingWaveform.value = (_recordingWaveform.value + amplitude).takeLast(20)
            }
        }
    }

    fun cancelVoiceRecording() {
        recordingJob?.cancel()
        _isRecordingVoice.value = false
        _recordingSeconds.value = 0
        _recordingWaveform.value = emptyList()
    }

    fun finishAndSendVoiceRecording() {
        val id = _chatId.value
        if (id.isEmpty()) {
            cancelVoiceRecording()
            return
        }
        val secs = _recordingSeconds.value.coerceAtLeast(1)
        val wave = _recordingWaveform.value.ifEmpty {
            listOf(0.3f, 0.7f, 0.5f, 0.85f, 0.6f, 0.4f, 0.75f, 0.5f)
        }
        val normalizedWave = if (wave.size < 14) {
            List(14) { idx -> wave[idx % wave.size] }
        } else wave.takeLast(16)
        val reply = _replyingTo.value
        _replyingTo.value = null
        cancelVoiceRecording()
        viewModelScope.launch {
            repository.sendVoiceNote(id, secs, normalizedWave, reply)
        }
    }

    fun togglePlayVoiceNote(messageId: String, durationSeconds: Int) {
        if (_playingVoiceMessageId.value == messageId) {
            voicePlayJob?.cancel()
            _playingVoiceMessageId.value = null
            _voicePlayProgress.value = 0f
            return
        }
        voicePlayJob?.cancel()
        _playingVoiceMessageId.value = messageId
        _voicePlayProgress.value = 0f
        voicePlayJob = viewModelScope.launch {
            val steps = (durationSeconds.coerceAtLeast(2)) * 10
            for (i in 1..steps) {
                delay(100)
                _voicePlayProgress.value = i / steps.toFloat()
            }
            _playingVoiceMessageId.value = null
            _voicePlayProgress.value = 0f
        }
    }

    fun startReply(message: ChatMessage) {
        if (message.isDeleted) return
        val snippet = when (val c = message.content) {
            is MessageContent.Text -> c.body
            is MessageContent.Image -> "📷 ${c.caption ?: "Photo"}"
            is MessageContent.Video -> "🎥 ${c.caption ?: "Video"}"
            is MessageContent.Document -> "📄 ${c.fileName}"
            is MessageContent.VoiceNote -> "🎤 Voice message (${c.durationSeconds}s)"
        }
        _replyingTo.value = ReplyPreview(
            messageId = message.id,
            senderName = if (message.isFromMe) "You" else message.senderName,
            snippet = snippet,
            isFromMe = message.isFromMe
        )
        _selectedMessage.value = null
    }

    fun cancelReply() {
        _replyingTo.value = null
    }

    fun selectMessageForActions(message: ChatMessage?) {
        _selectedMessage.value = message
    }

    fun toggleReaction(messageId: String, emoji: String) {
        val id = _chatId.value
        _selectedMessage.value = null
        if (id.isEmpty()) return
        viewModelScope.launch { repository.toggleReaction(id, messageId, emoji) }
    }

    fun toggleStarMessage(messageId: String) {
        val id = _chatId.value
        _selectedMessage.value = null
        if (id.isEmpty()) return
        viewModelScope.launch { repository.toggleStarMessage(id, messageId) }
    }

    fun deleteMessage(messageId: String) {
        val id = _chatId.value
        _selectedMessage.value = null
        if (id.isEmpty()) return
        viewModelScope.launch { repository.deleteMessage(id, messageId) }
    }

    fun toggleMuteChat() {
        val id = _chatId.value
        if (id.isEmpty()) return
        viewModelScope.launch { repository.toggleMuteChat(id) }
    }

    fun clearChat() {
        val id = _chatId.value
        if (id.isEmpty()) return
        viewModelScope.launch { repository.clearChatMessages(id) }
    }

    fun toggleInChatSearch(active: Boolean) {
        _isInChatSearchActive.value = active
        if (!active) _inChatSearchQuery.value = ""
    }

    fun updateInChatSearchQuery(query: String) {
        _inChatSearchQuery.value = query
    }
}
