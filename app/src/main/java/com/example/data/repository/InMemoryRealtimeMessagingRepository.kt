package com.example.data.repository

import com.example.R
import com.example.domain.model.CallDirection
import com.example.domain.model.CallLog
import com.example.domain.model.CallType
import com.example.domain.model.ChatMessage
import com.example.domain.model.ChatThread
import com.example.domain.model.Contact
import com.example.domain.model.DeviceContactEntry
import com.example.domain.model.MessageContent
import com.example.domain.model.MessagePreviewKind
import com.example.domain.model.MessageStatus
import com.example.domain.model.MessageTransport
import com.example.domain.model.ReplyPreview
import com.example.domain.model.StatusSlide
import com.example.domain.model.StatusStory
import com.example.domain.model.UserPresence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class InMemoryRealtimeMessagingRepository : MessagingRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    private val _contacts = MutableStateFlow(seedContacts())
    override val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _messagesByChat = MutableStateFlow(seedMessages())
    private val _presenceByChat = MutableStateFlow(
        seedContacts().associate { it.id to it.presence }
    )

    private val _chats = MutableStateFlow(seedChats())
    override val chats: StateFlow<List<ChatThread>> = _chats.asStateFlow()

    private val _statusStories = MutableStateFlow(seedStatuses())
    override val statusStories: StateFlow<List<StatusStory>> = _statusStories.asStateFlow()

    private val _callLogs = MutableStateFlow(seedCallLogs())
    override val callLogs: StateFlow<List<CallLog>> = _callLogs.asStateFlow()

    override fun observeMessages(chatId: String): Flow<List<ChatMessage>> =
        _messagesByChat.map { it[chatId].orEmpty() }

    override fun observePresence(chatId: String): Flow<UserPresence> =
        _presenceByChat.map { map ->
            val chat = _chats.value.find { it.id == chatId }
            val contactId = chat?.contact?.id ?: chatId
            map[contactId] ?: UserPresence.LastSeen("recently")
        }

    override fun getChatById(chatId: String): ChatThread? =
        _chats.value.find { it.id == chatId }

    override fun getContactById(contactId: String): Contact? =
        _contacts.value.find { it.id == contactId }

    override suspend fun sendTextMessage(
        chatId: String,
        text: String,
        replyTo: ReplyPreview?,
        transport: MessageTransport
    ) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val newMsg = createOutgoingMessage(
            chatId = chatId,
            content = MessageContent.Text(trimmed),
            replyTo = replyTo,
            transport = transport
        )
        val preview = if (transport == MessageTransport.SMS) "SMS: $trimmed" else trimmed
        appendMessageAndUpdateThread(chatId, newMsg, preview, MessagePreviewKind.TEXT)
        if (transport == MessageTransport.INTERNET) {
            simulateRealTimeDeliveryAndReply(chatId, trimmed)
        }
    }

    override suspend fun sendAttachmentMessage(
        chatId: String,
        content: MessageContent,
        replyTo: ReplyPreview?
    ) {
        val (preview, kind) = when (content) {
            is MessageContent.Image -> (content.caption?.takeIf { it.isNotBlank() } ?: "Photo") to MessagePreviewKind.PHOTO
            is MessageContent.Video -> (content.caption?.takeIf { it.isNotBlank() } ?: "Video") to MessagePreviewKind.VIDEO
            is MessageContent.Document -> content.fileName to MessagePreviewKind.DOCUMENT
            is MessageContent.VoiceNote -> "Voice message (${formatDuration(content.durationSeconds)})" to MessagePreviewKind.VOICE
            is MessageContent.Text -> content.body to MessagePreviewKind.TEXT
        }
        val newMsg = createOutgoingMessage(chatId, content, replyTo)
        appendMessageAndUpdateThread(chatId, newMsg, preview, kind)
        simulateRealTimeDeliveryAndReply(chatId, preview)
    }

    override suspend fun sendVoiceNote(
        chatId: String,
        durationSeconds: Int,
        waveform: List<Float>,
        replyTo: ReplyPreview?
    ) {
        val content = MessageContent.VoiceNote(
            durationSeconds = durationSeconds.coerceAtLeast(1),
            waveform = waveform
        )
        val preview = "Voice message (${formatDuration(durationSeconds.coerceAtLeast(1))})"
        val newMsg = createOutgoingMessage(chatId, content, replyTo)
        appendMessageAndUpdateThread(chatId, newMsg, preview, MessagePreviewKind.VOICE)
        simulateRealTimeDeliveryAndReply(chatId, "voice note")
    }

    override suspend fun toggleReaction(chatId: String, messageId: String, emoji: String) {
        _messagesByChat.update { map ->
            val list = map[chatId].orEmpty().map { msg ->
                if (msg.id != messageId) msg
                else {
                    val updated = if (emoji in msg.reactions) {
                        msg.reactions - emoji
                    } else {
                        msg.reactions + emoji
                    }
                    msg.copy(reactions = updated)
                }
            }
            map + (chatId to list)
        }
    }

    override suspend fun toggleStarMessage(chatId: String, messageId: String) {
        _messagesByChat.update { map ->
            val list = map[chatId].orEmpty().map { msg ->
                if (msg.id == messageId) msg.copy(isStarred = !msg.isStarred) else msg
            }
            map + (chatId to list)
        }
    }

    override suspend fun deleteMessage(chatId: String, messageId: String) {
        _messagesByChat.update { map ->
            val list = map[chatId].orEmpty().map { msg ->
                if (msg.id == messageId) {
                    msg.copy(
                        isDeleted = true,
                        content = MessageContent.Text("You deleted this message"),
                        reactions = emptyList(),
                        replyTo = null
                    )
                } else msg
            }
            map + (chatId to list)
        }
    }

    override suspend fun markChatAsRead(chatId: String) {
        _chats.update { list ->
            list.map { thread ->
                if (thread.id == chatId) thread.copy(unreadCount = 0) else thread
            }
        }
    }

    override suspend fun markAllChatsAsRead() {
        _chats.update { list ->
            list.map { it.copy(unreadCount = 0) }
        }
    }

    override suspend fun togglePinChat(chatId: String) {
        _chats.update { list ->
            sortChats(
                list.map { thread ->
                    if (thread.id == chatId) thread.copy(isPinned = !thread.isPinned) else thread
                }
            )
        }
    }

    override suspend fun toggleMuteChat(chatId: String) {
        _chats.update { list ->
            list.map { thread ->
                if (thread.id == chatId) thread.copy(isMuted = !thread.isMuted) else thread
            }
        }
    }

    override suspend fun toggleArchiveChat(chatId: String) {
        _chats.update { list ->
            list.map { thread ->
                if (thread.id == chatId) thread.copy(isArchived = !thread.isArchived) else thread
            }
        }
    }

    override suspend fun clearChatMessages(chatId: String) {
        _messagesByChat.update { it + (chatId to emptyList()) }
        _chats.update { list ->
            list.map { thread ->
                if (thread.id == chatId) {
                    thread.copy(
                        lastMessagePreview = "Messages cleared",
                        lastMessageKind = MessagePreviewKind.TEXT,
                        lastMessageStatus = null,
                        unreadCount = 0
                    )
                } else thread
            }
        }
    }

    override suspend fun openOrCreateChat(contact: Contact): String {
        val existing = _chats.value.find { !it.isGroup && it.contact.id == contact.id }
        if (existing != null) return existing.id
        val newChatId = "chat_${contact.id}"
        val now = System.currentTimeMillis()
        val newThread = ChatThread(
            id = newChatId,
            contact = contact,
            isGroup = false,
            lastMessagePreview = "Tap to start chatting",
            lastMessageKind = MessagePreviewKind.TEXT,
            lastMessageStatus = null,
            lastMessageIsFromMe = false,
            lastTimestampLabel = timeFormat.format(Date(now)),
            lastActivityMillis = now,
            unreadCount = 0
        )
        _chats.update { sortChats(listOf(newThread) + it) }
        _messagesByChat.update { it + (newChatId to emptyList()) }
        return newChatId
    }

    override suspend fun markStatusViewed(statusId: String) {
        _statusStories.update { stories ->
            stories.map { story ->
                if (story.id == statusId) story.copy(isViewed = true) else story
            }
        }
    }

    override suspend fun addMyStatus(slide: StatusSlide) {
        _statusStories.update { stories ->
            val mine = stories.firstOrNull { it.isMine }
            if (mine != null) {
                stories.map {
                    if (it.isMine) it.copy(slides = it.slides + slide, isViewed = false) else it
                }
            } else {
                val me = Contact(
                    id = "me",
                    name = "My status",
                    phoneNumber = "+1 (555) 010-2400",
                    about = "Available",
                    initials = "ME",
                    avatarColorIndex = 0
                )
                listOf(
                    StatusStory(
                        id = "status_mine",
                        contact = me,
                        isMine = true,
                        isViewed = false,
                        slides = listOf(slide)
                    )
                ) + stories
            }
        }
    }

    override suspend fun logCall(contact: Contact, callType: CallType, durationSeconds: Int) {
        val duration = if (durationSeconds > 0) formatDuration(durationSeconds) else null
        val newLog = CallLog(
            id = "call_${UUID.randomUUID()}",
            contact = contact,
            callType = callType,
            direction = CallDirection.OUTGOING,
            timestampLabel = "Just now",
            durationLabel = duration
        )
        _callLogs.update { listOf(newLog) + it }
    }

    override suspend fun syncDeviceContacts(entries: List<DeviceContactEntry>): Int {
        if (entries.isEmpty()) return 0
        val current = _contacts.value.toMutableList()
        var matchedCount = 0

        entries.forEach { entry ->
            val existingIdx = current.indexOfFirst { existing ->
                val existingNorm = existing.phoneNumber.filter { it.isDigit() }.takeLast(10)
                existingNorm.isNotEmpty() && existingNorm == entry.normalizedDigits
            }
            if (existingIdx >= 0) {
                current[existingIdx] = current[existingIdx].copy(isMatchedFromPhoneBook = true)
                matchedCount++
            } else {
                val initials = entry.displayName
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .joinToString("") { it.first().uppercase() }
                    .ifEmpty { "#" }
                val newContact = Contact(
                    id = "device_${entry.normalizedDigits}",
                    name = entry.displayName,
                    phoneNumber = entry.phoneNumber,
                    about = "Phone contact • Online chat or SMS/tel: fallback",
                    initials = initials,
                    avatarColorIndex = (entry.displayName.hashCode().let { if (it < 0) -it else it }) % 8,
                    presence = UserPresence.LastSeen("on PingBridge / SMS"),
                    isMatchedFromPhoneBook = true
                )
                current.add(newContact)
                matchedCount++
            }
        }
        _contacts.value = current
        return matchedCount
    }

    private fun createOutgoingMessage(
        chatId: String,
        content: MessageContent,
        replyTo: ReplyPreview?,
        transport: MessageTransport = MessageTransport.INTERNET
    ): ChatMessage {
        val now = System.currentTimeMillis()
        val initialStatus = if (transport == MessageTransport.SMS) {
            MessageStatus.SENT_SMS
        } else {
            MessageStatus.SENDING
        }
        return ChatMessage(
            id = "msg_${UUID.randomUUID()}",
            chatId = chatId,
            senderId = "me",
            senderName = "You",
            isFromMe = true,
            timestampMillis = now,
            timeLabel = timeFormat.format(Date(now)),
            dateGroupLabel = "TODAY",
            content = content,
            status = initialStatus,
            transport = transport,
            replyTo = replyTo
        )
    }

    private fun appendMessageAndUpdateThread(
        chatId: String,
        message: ChatMessage,
        preview: String,
        kind: MessagePreviewKind
    ) {
        _messagesByChat.update { map ->
            val current = map[chatId].orEmpty()
            map + (chatId to (current + message))
        }
        _chats.update { threads ->
            sortChats(
                threads.map { thread ->
                    if (thread.id != chatId) thread
                    else thread.copy(
                        lastMessagePreview = preview,
                        lastMessageKind = kind,
                        lastMessageStatus = message.status,
                        lastMessageIsFromMe = message.isFromMe,
                        lastTimestampLabel = message.timeLabel,
                        lastActivityMillis = message.timestampMillis,
                        isArchived = false
                    )
                }
            )
        }
    }

    private fun simulateRealTimeDeliveryAndReply(chatId: String, outgoingText: String) {
        val thread = _chats.value.find { it.id == chatId } ?: return
        val contactId = thread.contact.id
        val originalPresence = thread.contact.presence

        scope.launch {
            delay(300)
            updateLastMessageStatus(chatId, MessageStatus.SENT)
            delay(450)
            updateLastMessageStatus(chatId, MessageStatus.DELIVERED)
            delay(650)
            updateLastMessageStatus(chatId, MessageStatus.READ)

            // Show authentic typing indicator and send natural reply
            delay(500)
            _presenceByChat.update { it + (contactId to UserPresence.Typing) }
            delay(1600)
            _presenceByChat.update { it + (contactId to originalPresence) }

            val replyBody = buildContextualReply(thread.contact.name, thread.isGroup, outgoingText)
            val now = System.currentTimeMillis()
            val replySender = if (thread.isGroup) {
                thread.groupParticipants.firstOrNull { it != "You" } ?: thread.contact.name
            } else {
                thread.contact.name
            }
            val incomingMsg = ChatMessage(
                id = "msg_${UUID.randomUUID()}",
                chatId = chatId,
                senderId = contactId,
                senderName = replySender,
                isFromMe = false,
                timestampMillis = now,
                timeLabel = timeFormat.format(Date(now)),
                dateGroupLabel = "TODAY",
                content = MessageContent.Text(replyBody),
                status = MessageStatus.READ
            )
            appendMessageAndUpdateThread(
                chatId = chatId,
                message = incomingMsg,
                preview = if (thread.isGroup) "$replySender: $replyBody" else replyBody,
                kind = MessagePreviewKind.TEXT
            )
        }
    }

    private fun updateLastMessageStatus(chatId: String, newStatus: MessageStatus) {
        _messagesByChat.update { map ->
            val current = map[chatId].orEmpty()
            val updated = current.mapIndexed { idx, msg ->
                if (msg.isFromMe && msg.status != MessageStatus.READ && idx >= current.size - 2) {
                    msg.copy(status = newStatus)
                } else msg
            }
            map + (chatId to updated)
        }
        _chats.update { threads ->
            threads.map { thread ->
                if (thread.id == chatId && thread.lastMessageIsFromMe) {
                    thread.copy(lastMessageStatus = newStatus)
                } else thread
            }
        }
    }

    private fun sortChats(list: List<ChatThread>): List<ChatThread> =
        list.sortedWith(
            compareByDescending<ChatThread> { it.isPinned }
                .thenByDescending { it.lastActivityMillis }
        )

    private fun buildContextualReply(name: String, isGroup: Boolean, outgoing: String): String {
        val lower = outgoing.lowercase()
        return when {
            lower.contains("voice note") -> "Got your voice note! Sounds super clear — let's catch up soon 👍"
            lower.contains("photo") || lower.contains("image") -> "Love that photo! Thanks for sharing ✨"
            lower.contains("video") -> "Just watched the clip — looks awesome!"
            lower.contains("pdf") || lower.contains("doc") -> "Received the file, reviewing it right now."
            lower.contains("call") -> "Sure, free for a quick call whenever you are!"
            lower.contains("coffee") || lower.contains("lunch") || lower.contains("dinner") ->
                "Count me in! Let's lock in the time."
            lower.contains("?") ->
                if (isGroup) "Good question — I think Friday afternoon works best for everyone."
                else "Yes! Everything looks good on my end."
            else ->
                if (isGroup) "Sounds like a plan! Thanks for the update 🙌"
                else "Got it, thanks ${if (name.contains(" ")) "" else "so much "}— talk in a bit!"
        }
    }

    private fun formatDuration(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.getDefault(), "%d:%02d", mins, secs)
    }

    private fun seedContacts(): List<Contact> = listOf(
        Contact(
            id = "c_maya",
            name = "Maya Lin",
            phoneNumber = "+1 (415) 890-3312",
            about = "Product designer & trail runner 🏔️",
            initials = "ML",
            avatarColorIndex = 0,
            presence = UserPresence.Online,
            isFavorite = true
        ),
        Contact(
            id = "c_liam",
            name = "Liam Carter",
            phoneNumber = "+1 (415) 312-9045",
            about = "Building cool mobile things ⚡",
            initials = "LC",
            avatarColorIndex = 1,
            presence = UserPresence.LastSeen("today at 8:42 AM"),
            isFavorite = true
        ),
        Contact(
            id = "c_group_trail",
            name = "Weekend Trail Crew",
            phoneNumber = "Group",
            about = "Sunday sunrise hikes & post-hike tacos",
            initials = "WT",
            avatarColorIndex = 5,
            presence = UserPresence.Online,
            isFavorite = true
        ),
        Contact(
            id = "c_sofia",
            name = "Sofia Alvarez",
            phoneNumber = "+1 (650) 442-7819",
            about = "Espresso enthusiast ☕",
            initials = "SA",
            avatarColorIndex = 7,
            presence = UserPresence.Online,
            isFavorite = false
        ),
        Contact(
            id = "c_noah",
            name = "Noah Patel",
            phoneNumber = "+1 (408) 771-1903",
            about = "At the studio 🎧",
            initials = "NP",
            avatarColorIndex = 2,
            presence = UserPresence.LastSeen("yesterday at 11:18 PM"),
            isFavorite = false
        ),
        Contact(
            id = "c_group_design",
            name = "Design Sync",
            phoneNumber = "Group",
            about = "UI systems, prototypes & sprint reviews",
            initials = "DS",
            avatarColorIndex = 6,
            presence = UserPresence.Online,
            isFavorite = false
        ),
        Contact(
            id = "c_elena",
            name = "Elena Rostova",
            phoneNumber = "+1 (212) 559-8421",
            about = "Always traveling ✈️",
            initials = "ER",
            avatarColorIndex = 4,
            presence = UserPresence.LastSeen("today at 7:05 AM"),
            isFavorite = false
        ),
        Contact(
            id = "c_marcus",
            name = "Marcus Vance",
            phoneNumber = "+1 (310) 689-4120",
            about = "Photography & film",
            initials = "MV",
            avatarColorIndex = 3,
            presence = UserPresence.LastSeen("Nov 21"),
            isFavorite = false
        )
    )

    private fun seedChats(): List<ChatThread> {
        val contacts = seedContacts().associateBy { it.id }
        val now = System.currentTimeMillis()
        return listOf(
            ChatThread(
                id = "chat_c_maya",
                contact = contacts.getValue("c_maya"),
                lastMessagePreview = "Look at that ridge light from yesterday!",
                lastMessageKind = MessagePreviewKind.PHOTO,
                lastMessageStatus = null,
                lastMessageIsFromMe = false,
                lastTimestampLabel = "8:51 AM",
                lastActivityMillis = now - 5 * 60_000,
                unreadCount = 2,
                isPinned = true
            ),
            ChatThread(
                id = "chat_c_liam",
                contact = contacts.getValue("c_liam"),
                lastMessagePreview = "Sent over the architecture notes PDF",
                lastMessageKind = MessagePreviewKind.DOCUMENT,
                lastMessageStatus = MessageStatus.READ,
                lastMessageIsFromMe = true,
                lastTimestampLabel = "8:30 AM",
                lastActivityMillis = now - 25 * 60_000,
                unreadCount = 0,
                isPinned = true
            ),
            ChatThread(
                id = "chat_c_group_trail",
                contact = contacts.getValue("c_group_trail"),
                isGroup = true,
                groupParticipants = listOf("Maya", "Liam", "Sofia", "You"),
                lastMessagePreview = "Sofia: Packing extra thermoses for 6 AM!",
                lastMessageKind = MessagePreviewKind.TEXT,
                lastMessageStatus = null,
                lastMessageIsFromMe = false,
                lastTimestampLabel = "8:14 AM",
                lastActivityMillis = now - 40 * 60_000,
                unreadCount = 4
            ),
            ChatThread(
                id = "chat_c_sofia",
                contact = contacts.getValue("c_sofia"),
                lastMessagePreview = "Voice message (0:14)",
                lastMessageKind = MessagePreviewKind.VOICE,
                lastMessageStatus = MessageStatus.DELIVERED,
                lastMessageIsFromMe = true,
                lastTimestampLabel = "Yesterday",
                lastActivityMillis = now - 12 * 3600_000,
                unreadCount = 0
            ),
            ChatThread(
                id = "chat_c_noah",
                contact = contacts.getValue("c_noah"),
                lastMessagePreview = "Let's test the new mix tonight",
                lastMessageKind = MessagePreviewKind.TEXT,
                lastMessageStatus = MessageStatus.SENT,
                lastMessageIsFromMe = true,
                lastTimestampLabel = "Yesterday",
                lastActivityMillis = now - 18 * 3600_000,
                unreadCount = 0,
                isMuted = true
            ),
            ChatThread(
                id = "chat_c_group_design",
                contact = contacts.getValue("c_group_design"),
                isGroup = true,
                groupParticipants = listOf("Maya", "Noah", "Elena", "You"),
                lastMessagePreview = "Elena: Updated the bottom bar tokens",
                lastMessageKind = MessagePreviewKind.TEXT,
                lastMessageStatus = null,
                lastMessageIsFromMe = false,
                lastTimestampLabel = "Nov 22",
                lastActivityMillis = now - 30 * 3600_000,
                unreadCount = 1
            ),
            ChatThread(
                id = "chat_c_elena",
                contact = contacts.getValue("c_elena"),
                lastMessagePreview = "Landed in Lisbon! Calling you later",
                lastMessageKind = MessagePreviewKind.TEXT,
                lastMessageStatus = null,
                lastMessageIsFromMe = false,
                lastTimestampLabel = "Nov 21",
                lastActivityMillis = now - 48 * 3600_000,
                unreadCount = 0
            ),
            ChatThread(
                id = "chat_c_marcus",
                contact = contacts.getValue("c_marcus"),
                lastMessagePreview = "Thanks for sending the RAW presets!",
                lastMessageKind = MessagePreviewKind.TEXT,
                lastMessageStatus = MessageStatus.READ,
                lastMessageIsFromMe = true,
                lastTimestampLabel = "Nov 19",
                lastActivityMillis = now - 96 * 3600_000,
                unreadCount = 0,
                isArchived = true
            )
        )
    }

    private fun seedMessages(): Map<String, List<ChatMessage>> {
        val now = System.currentTimeMillis()
        return mapOf(
            "chat_c_maya" to listOf(
                ChatMessage(
                    id = "m1",
                    chatId = "chat_c_maya",
                    senderId = "c_maya",
                    senderName = "Maya Lin",
                    isFromMe = false,
                    timestampMillis = now - 86_400_000,
                    timeLabel = "6:12 PM",
                    dateGroupLabel = "YESTERDAY",
                    content = MessageContent.Text("Made it to the upper ridge just before sunset!"),
                    reactions = listOf("🔥")
                ),
                ChatMessage(
                    id = "m2",
                    chatId = "chat_c_maya",
                    senderId = "me",
                    senderName = "You",
                    isFromMe = true,
                    timestampMillis = now - 86_000_000,
                    timeLabel = "6:15 PM",
                    dateGroupLabel = "YESTERDAY",
                    content = MessageContent.Text("No way, the sky looks clear from downtown too. Send pics!"),
                    status = MessageStatus.READ
                ),
                ChatMessage(
                    id = "m3",
                    chatId = "chat_c_maya",
                    senderId = "c_maya",
                    senderName = "Maya Lin",
                    isFromMe = false,
                    timestampMillis = now - 360_000,
                    timeLabel = "8:49 AM",
                    dateGroupLabel = "TODAY",
                    content = MessageContent.Image(
                        caption = "Look at that ridge light from yesterday!",
                        drawableRes = R.drawable.img_status_sunset
                    ),
                    reactions = listOf("❤️", "🏔️")
                ),
                ChatMessage(
                    id = "m4",
                    chatId = "chat_c_maya",
                    senderId = "c_maya",
                    senderName = "Maya Lin",
                    isFromMe = false,
                    timestampMillis = now - 300_000,
                    timeLabel = "8:51 AM",
                    dateGroupLabel = "TODAY",
                    content = MessageContent.VoiceNote(
                        durationSeconds = 19,
                        waveform = listOf(0.25f, 0.6f, 0.85f, 0.45f, 0.9f, 0.7f, 0.35f, 0.8f, 0.95f, 0.5f, 0.4f, 0.75f, 0.6f, 0.3f, 0.55f, 0.4f)
                    )
                )
            ),
            "chat_c_liam" to listOf(
                ChatMessage(
                    id = "l1",
                    chatId = "chat_c_liam",
                    senderId = "c_liam",
                    senderName = "Liam Carter",
                    isFromMe = false,
                    timestampMillis = now - 3_600_000,
                    timeLabel = "8:12 AM",
                    dateGroupLabel = "TODAY",
                    content = MessageContent.Text("Hey! Did you finalize the WebSocket & repository layer spec?")
                ),
                ChatMessage(
                    id = "l2",
                    chatId = "chat_c_liam",
                    senderId = "me",
                    senderName = "You",
                    isFromMe = true,
                    timestampMillis = now - 2_400_000,
                    timeLabel = "8:26 AM",
                    dateGroupLabel = "TODAY",
                    content = MessageContent.Text("Yep! Clean Flow-based interface so we can swap Firebase or WebSockets anytime."),
                    status = MessageStatus.READ,
                    replyTo = ReplyPreview(
                        messageId = "l1",
                        senderName = "Liam Carter",
                        snippet = "Did you finalize the WebSocket & repository layer spec?",
                        isFromMe = false
                    ),
                    reactions = listOf("👍")
                ),
                ChatMessage(
                    id = "l3",
                    chatId = "chat_c_liam",
                    senderId = "me",
                    senderName = "You",
                    isFromMe = true,
                    timestampMillis = now - 1_500_000,
                    timeLabel = "8:30 AM",
                    dateGroupLabel = "TODAY",
                    content = MessageContent.Document(
                        fileName = "Realtime_Sync_Architecture_v2.pdf",
                        fileSize = "1.4 MB",
                        extension = "PDF"
                    ),
                    status = MessageStatus.READ
                )
            ),
            "chat_c_group_trail" to listOf(
                ChatMessage(
                    id = "t1",
                    chatId = "chat_c_group_trail",
                    senderId = "c_maya",
                    senderName = "Maya Lin",
                    isFromMe = false,
                    timestampMillis = now - 5_000_000,
                    timeLabel = "7:40 AM",
                    dateGroupLabel = "TODAY",
                    content = MessageContent.Text("Trailhead parking opens at 5:45 AM sharp!")
                ),
                ChatMessage(
                    id = "t2",
                    chatId = "chat_c_group_trail",
                    senderId = "c_liam",
                    senderName = "Liam Carter",
                    isFromMe = false,
                    timestampMillis = now - 3_800_000,
                    timeLabel = "7:58 AM",
                    dateGroupLabel = "TODAY",
                    content = MessageContent.Text("I'll bring headlamps and trail mix for everyone.")
                ),
                ChatMessage(
                    id = "t3",
                    chatId = "chat_c_group_trail",
                    senderId = "c_sofia",
                    senderName = "Sofia Alvarez",
                    isFromMe = false,
                    timestampMillis = now - 2_400_000,
                    timeLabel = "8:14 AM",
                    dateGroupLabel = "TODAY",
                    content = MessageContent.Text("Packing extra thermoses for 6 AM! ☕"),
                    reactions = listOf("🙌", "❤️")
                )
            ),
            "chat_c_sofia" to listOf(
                ChatMessage(
                    id = "s1",
                    chatId = "chat_c_sofia",
                    senderId = "c_sofia",
                    senderName = "Sofia Alvarez",
                    isFromMe = false,
                    timestampMillis = now - 45_000_000,
                    timeLabel = "4:20 PM",
                    dateGroupLabel = "YESTERDAY",
                    content = MessageContent.Image(
                        caption = "Found our new favorite coffee spot downtown",
                        drawableRes = R.drawable.img_status_coffee
                    )
                ),
                ChatMessage(
                    id = "s2",
                    chatId = "chat_c_sofia",
                    senderId = "me",
                    senderName = "You",
                    isFromMe = true,
                    timestampMillis = now - 43_200_000,
                    timeLabel = "4:45 PM",
                    dateGroupLabel = "YESTERDAY",
                    content = MessageContent.VoiceNote(
                        durationSeconds = 14,
                        waveform = listOf(0.4f, 0.8f, 0.55f, 0.9f, 0.65f, 0.35f, 0.7f, 0.85f, 0.5f, 0.6f, 0.3f, 0.75f, 0.45f, 0.25f)
                    ),
                    status = MessageStatus.DELIVERED
                )
            ),
            "chat_c_noah" to listOf(
                ChatMessage(
                    id = "n1",
                    chatId = "chat_c_noah",
                    senderId = "me",
                    senderName = "You",
                    isFromMe = true,
                    timestampMillis = now - 64_800_000,
                    timeLabel = "2:10 PM",
                    dateGroupLabel = "YESTERDAY",
                    content = MessageContent.Text("Let's test the new mix tonight"),
                    status = MessageStatus.SENT
                )
            ),
            "chat_c_group_design" to listOf(
                ChatMessage(
                    id = "d1",
                    chatId = "chat_c_group_design",
                    senderId = "c_elena",
                    senderName = "Elena Rostova",
                    isFromMe = false,
                    timestampMillis = now - 108_000_000,
                    timeLabel = "Nov 22",
                    dateGroupLabel = "NOV 22",
                    content = MessageContent.Text("Updated the bottom bar tokens and bubble tail geometry 🎨")
                )
            ),
            "chat_c_elena" to listOf(
                ChatMessage(
                    id = "e1",
                    chatId = "chat_c_elena",
                    senderId = "c_elena",
                    senderName = "Elena Rostova",
                    isFromMe = false,
                    timestampMillis = now - 172_800_000,
                    timeLabel = "Nov 21",
                    dateGroupLabel = "NOV 21",
                    content = MessageContent.Text("Landed in Lisbon! Calling you later 🇵🇹")
                )
            ),
            "chat_c_marcus" to listOf(
                ChatMessage(
                    id = "ma1",
                    chatId = "chat_c_marcus",
                    senderId = "me",
                    senderName = "You",
                    isFromMe = true,
                    timestampMillis = now - 345_600_000,
                    timeLabel = "Nov 19",
                    dateGroupLabel = "NOV 19",
                    content = MessageContent.Text("Thanks for sending the RAW presets!"),
                    status = MessageStatus.READ
                )
            )
        )
    }

    private fun seedStatuses(): List<StatusStory> {
        val contacts = seedContacts().associateBy { it.id }
        val me = Contact(
            id = "me",
            name = "My status",
            phoneNumber = "+1 (555) 010-2400",
            about = "Available",
            initials = "ME",
            avatarColorIndex = 0
        )
        return listOf(
            StatusStory(
                id = "status_mine",
                contact = me,
                isMine = true,
                isViewed = false,
                slides = emptyList()
            ),
            StatusStory(
                id = "status_maya",
                contact = contacts.getValue("c_maya"),
                isViewed = false,
                slides = listOf(
                    StatusSlide.Photo(
                        drawableRes = R.drawable.img_status_sunset,
                        caption = "Golden hour above the clouds 🏔️✨",
                        timeAgo = "24 minutes ago"
                    ),
                    StatusSlide.TextCard(
                        backgroundColorHex = 0xFF006B58,
                        caption = "Who's joining Sunday's 6 AM ridge hike?",
                        timeAgo = "18 minutes ago"
                    )
                )
            ),
            StatusStory(
                id = "status_sofia",
                contact = contacts.getValue("c_sofia"),
                isViewed = false,
                slides = listOf(
                    StatusSlide.Photo(
                        drawableRes = R.drawable.img_status_coffee,
                        caption = "Morning brainstorm + oat latte ☕",
                        timeAgo = "Today, 8:05 AM"
                    )
                )
            ),
            StatusStory(
                id = "status_elena",
                contact = contacts.getValue("c_elena"),
                isViewed = true,
                slides = listOf(
                    StatusSlide.TextCard(
                        backgroundColorHex = 0xFF1565C0,
                        caption = "Lisbon rooftops & pastel de nata 🇵🇹☀️",
                        timeAgo = "Yesterday, 7:40 PM"
                    )
                )
            )
        )
    }

    private fun seedCallLogs(): List<CallLog> {
        val contacts = seedContacts().associateBy { it.id }
        return listOf(
            CallLog(
                id = "call_1",
                contact = contacts.getValue("c_maya"),
                callType = CallType.VIDEO,
                direction = CallDirection.INCOMING,
                timestampLabel = "Today, 8:20 AM",
                durationLabel = "12:45"
            ),
            CallLog(
                id = "call_2",
                contact = contacts.getValue("c_sofia"),
                callType = CallType.VOICE,
                direction = CallDirection.MISSED,
                timestampLabel = "Yesterday, 9:15 PM",
                durationLabel = null
            ),
            CallLog(
                id = "call_3",
                contact = contacts.getValue("c_liam"),
                callType = CallType.VOICE,
                direction = CallDirection.OUTGOING,
                timestampLabel = "Yesterday, 3:04 PM",
                durationLabel = "6:18"
            ),
            CallLog(
                id = "call_4",
                contact = contacts.getValue("c_elena"),
                callType = CallType.VIDEO,
                direction = CallDirection.MISSED,
                timestampLabel = "Nov 21, 5:42 PM",
                durationLabel = null
            ),
            CallLog(
                id = "call_5",
                contact = contacts.getValue("c_noah"),
                callType = CallType.VOICE,
                direction = CallDirection.INCOMING,
                timestampLabel = "Nov 20, 1:10 PM",
                durationLabel = "24:02"
            )
        )
    }
}
