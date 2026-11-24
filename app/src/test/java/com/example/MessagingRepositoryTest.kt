package com.example

import com.example.data.repository.InMemoryRealtimeMessagingRepository
import com.example.domain.model.CallType
import com.example.domain.model.MessageContent
import com.example.domain.model.MessageStatus
import com.example.domain.model.MessageTransport
import com.example.domain.model.ReplyPreview
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessagingRepositoryTest {

    @Test
    fun sendTextAndToggleReactionUpdatesMessages() = runTest {
        val repo = InMemoryRealtimeMessagingRepository()
        val chatId = "chat_c_maya"
        val initialCount = repo.observeMessages(chatId).first().size

        repo.sendTextMessage(
            chatId = chatId,
            text = "See you at the trailhead!",
            replyTo = ReplyPreview(
                messageId = "m1",
                senderName = "Maya Lin",
                snippet = "Made it to the upper ridge",
                isFromMe = false
            )
        )

        val updated = repo.observeMessages(chatId).first()
        assertEquals(initialCount + 1, updated.size)
        val sentMsg = updated.last()
        assertEquals("See you at the trailhead!", (sentMsg.content as MessageContent.Text).body)
        assertEquals("Maya Lin", sentMsg.replyTo?.senderName)
        assertEquals(MessageTransport.INTERNET, sentMsg.transport)

        repo.toggleReaction(chatId, sentMsg.id, "❤️")
        val reacted = repo.observeMessages(chatId).first().last()
        assertTrue("❤️" in reacted.reactions)
    }

    @Test
    fun offlineSmsFallbackAndCellularCallLoggingWork() = runTest {
        val repo = InMemoryRealtimeMessagingRepository()
        val chatId = "chat_c_liam"
        repo.sendTextMessage(
            chatId = chatId,
            text = "Running 5 mins late, no data here",
            transport = MessageTransport.SMS
        )
        val smsMsg = repo.observeMessages(chatId).first().last()
        assertEquals(MessageTransport.SMS, smsMsg.transport)
        assertEquals(MessageStatus.SENT_SMS, smsMsg.status)

        val liam = repo.getContactById("c_liam")!!
        repo.logCall(liam, CallType.CELLULAR, 0)
        assertEquals(CallType.CELLULAR, repo.callLogs.value.first().callType)
    }

    @Test
    fun logCallAndMarkStatusViewedUpdateState() = runTest {
        val repo = InMemoryRealtimeMessagingRepository()
        val maya = repo.getContactById("c_maya")!!
        val initialCalls = repo.callLogs.value.size

        repo.logCall(maya, CallType.VIDEO, 95)
        assertEquals(initialCalls + 1, repo.callLogs.value.size)
        assertEquals("1:35", repo.callLogs.value.first().durationLabel)

        repo.markStatusViewed("status_maya")
        val mayaStatus = repo.statusStories.value.first { it.id == "status_maya" }
        assertTrue(mayaStatus.isViewed)
    }
}
