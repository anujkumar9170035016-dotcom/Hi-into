package com.example.domain.model

import androidx.annotation.DrawableRes

sealed interface UserPresence {
    data object Online : UserPresence
    data class LastSeen(val label: String) : UserPresence
    data object Typing : UserPresence
    data object RecordingAudio : UserPresence
}

enum class MessageTransport {
    INTERNET,
    SMS
}

data class DeviceContactEntry(
    val deviceId: String,
    val displayName: String,
    val phoneNumber: String,
    val normalizedDigits: String
)

data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val about: String,
    val initials: String,
    val avatarColorIndex: Int,
    @DrawableRes val avatarRes: Int? = null,
    val presence: UserPresence = UserPresence.LastSeen("today at 9:15 AM"),
    val isFavorite: Boolean = false,
    val isMatchedFromPhoneBook: Boolean = false
)

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    SENT_SMS
}

enum class MessagePreviewKind {
    TEXT,
    PHOTO,
    VIDEO,
    VOICE,
    DOCUMENT
}

sealed interface MessageContent {
    data class Text(val body: String) : MessageContent
    data class Image(
        val caption: String? = null,
        @DrawableRes val drawableRes: Int? = null,
        val uriString: String? = null
    ) : MessageContent
    data class Video(
        val caption: String? = null,
        val durationLabel: String = "0:18",
        @DrawableRes val thumbnailRes: Int? = null,
        val uriString: String? = null
    ) : MessageContent
    data class Document(
        val fileName: String,
        val fileSize: String,
        val extension: String,
        val uriString: String? = null
    ) : MessageContent
    data class VoiceNote(
        val durationSeconds: Int,
        val waveform: List<Float>,
        val isPlayed: Boolean = false
    ) : MessageContent
}

data class ReplyPreview(
    val messageId: String,
    val senderName: String,
    val snippet: String,
    val isFromMe: Boolean
)

data class ChatMessage(
    val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val isFromMe: Boolean,
    val timestampMillis: Long,
    val timeLabel: String,
    val dateGroupLabel: String,
    val content: MessageContent,
    val status: MessageStatus = MessageStatus.READ,
    val transport: MessageTransport = MessageTransport.INTERNET,
    val replyTo: ReplyPreview? = null,
    val reactions: List<String> = emptyList(),
    val isStarred: Boolean = false,
    val isDeleted: Boolean = false
)

data class ChatThread(
    val id: String,
    val contact: Contact,
    val isGroup: Boolean = false,
    val groupParticipants: List<String> = emptyList(),
    val lastMessagePreview: String,
    val lastMessageKind: MessagePreviewKind = MessagePreviewKind.TEXT,
    val lastMessageStatus: MessageStatus? = null,
    val lastMessageIsFromMe: Boolean = false,
    val lastTimestampLabel: String,
    val lastActivityMillis: Long,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false
)

enum class ChatFilter(val label: String) {
    ALL("All"),
    UNREAD("Unread"),
    FAVORITES("Favorites"),
    GROUPS("Groups")
}

sealed interface StatusSlide {
    val caption: String
    val timeAgo: String

    data class Photo(
        @DrawableRes val drawableRes: Int? = null,
        val uriString: String? = null,
        override val caption: String,
        override val timeAgo: String
    ) : StatusSlide

    data class TextCard(
        val backgroundColorHex: Long,
        override val caption: String,
        override val timeAgo: String
    ) : StatusSlide
}

data class StatusStory(
    val id: String,
    val contact: Contact,
    val isMine: Boolean = false,
    val isViewed: Boolean = false,
    val slides: List<StatusSlide>
)

enum class CallType {
    VOICE,
    VIDEO,
    CELLULAR
}

enum class CallDirection {
    INCOMING,
    OUTGOING,
    MISSED
}

data class CallLog(
    val id: String,
    val contact: Contact,
    val callType: CallType,
    val direction: CallDirection,
    val timestampLabel: String,
    val durationLabel: String? = null
)

enum class CallPhase {
    RINGING,
    CONNECTING,
    CONNECTED,
    ENDED
}

data class ActiveCallUiState(
    val callId: String,
    val contact: Contact,
    val callType: CallType,
    val phase: CallPhase = CallPhase.RINGING,
    val elapsedSeconds: Int = 0,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isVideoEnabled: Boolean = true,
    val isFrontCamera: Boolean = true
)
