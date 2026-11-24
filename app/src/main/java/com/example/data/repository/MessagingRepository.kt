package com.example.data.repository

import com.example.domain.model.CallLog
import com.example.domain.model.CallType
import com.example.domain.model.ChatMessage
import com.example.domain.model.ChatThread
import com.example.domain.model.Contact
import com.example.domain.model.DeviceContactEntry
import com.example.domain.model.MessageContent
import com.example.domain.model.MessageTransport
import com.example.domain.model.ReplyPreview
import com.example.domain.model.StatusSlide
import com.example.domain.model.StatusStory
import com.example.domain.model.UserPresence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Clean real-time messaging repository boundary.
 * Swap [InMemoryRealtimeMessagingRepository] with a Firebase Firestore / WebSocket
 * implementation without changing UI or ViewModel contracts.
 */
interface MessagingRepository {
    val chats: StateFlow<List<ChatThread>>
    val contacts: StateFlow<List<Contact>>
    val statusStories: StateFlow<List<StatusStory>>
    val callLogs: StateFlow<List<CallLog>>

    fun observeMessages(chatId: String): Flow<List<ChatMessage>>
    fun observePresence(chatId: String): Flow<UserPresence>
    fun getChatById(chatId: String): ChatThread?
    fun getContactById(contactId: String): Contact?

    suspend fun sendTextMessage(
        chatId: String,
        text: String,
        replyTo: ReplyPreview? = null,
        transport: MessageTransport = MessageTransport.INTERNET
    )
    suspend fun sendAttachmentMessage(
        chatId: String,
        content: MessageContent,
        replyTo: ReplyPreview? = null
    )
    suspend fun sendVoiceNote(
        chatId: String,
        durationSeconds: Int,
        waveform: List<Float>,
        replyTo: ReplyPreview? = null
    )

    suspend fun toggleReaction(chatId: String, messageId: String, emoji: String)
    suspend fun toggleStarMessage(chatId: String, messageId: String)
    suspend fun deleteMessage(chatId: String, messageId: String)
    suspend fun markChatAsRead(chatId: String)
    suspend fun markAllChatsAsRead()
    suspend fun togglePinChat(chatId: String)
    suspend fun toggleMuteChat(chatId: String)
    suspend fun toggleArchiveChat(chatId: String)
    suspend fun clearChatMessages(chatId: String)
    suspend fun openOrCreateChat(contact: Contact): String

    suspend fun markStatusViewed(statusId: String)
    suspend fun addMyStatus(slide: StatusSlide)
    suspend fun logCall(contact: Contact, callType: CallType, durationSeconds: Int)
    suspend fun syncDeviceContacts(entries: List<DeviceContactEntry>): Int
}
