package com.example.ui.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.MessagingRepository
import com.example.data.repository.ServiceLocator
import com.example.domain.model.ActiveCallUiState
import com.example.domain.model.CallPhase
import com.example.domain.model.CallType
import com.example.domain.model.Contact
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class CallViewModel(
    private val repository: MessagingRepository = ServiceLocator.messagingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ActiveCallUiState?>(null)
    val uiState: StateFlow<ActiveCallUiState?> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun startCall(contactId: String, callType: CallType) {
        val contact: Contact = repository.getContactById(contactId)
            ?: repository.chats.value.firstOrNull { it.contact.id == contactId }?.contact
            ?: return

        timerJob?.cancel()
        _uiState.value = ActiveCallUiState(
            callId = UUID.randomUUID().toString(),
            contact = contact,
            callType = callType,
            phase = CallPhase.RINGING,
            elapsedSeconds = 0,
            isMuted = false,
            isSpeakerOn = callType == CallType.VIDEO,
            isVideoEnabled = callType == CallType.VIDEO,
            isFrontCamera = true
        )

        timerJob = viewModelScope.launch {
            delay(1300)
            _uiState.update { it?.copy(phase = CallPhase.CONNECTING) }
            delay(1000)
            _uiState.update { it?.copy(phase = CallPhase.CONNECTED) }
            while (true) {
                delay(1000)
                _uiState.update { state ->
                    if (state?.phase == CallPhase.CONNECTED) {
                        state.copy(elapsedSeconds = state.elapsedSeconds + 1)
                    } else state
                }
            }
        }
    }

    fun toggleMute() {
        _uiState.update { it?.copy(isMuted = !it.isMuted) }
    }

    fun toggleSpeaker() {
        _uiState.update { it?.copy(isSpeakerOn = !it.isSpeakerOn) }
    }

    fun toggleVideo() {
        _uiState.update { it?.copy(isVideoEnabled = !it.isVideoEnabled) }
    }

    fun flipCamera() {
        _uiState.update { it?.copy(isFrontCamera = !it.isFrontCamera) }
    }

    fun endCall(onComplete: () -> Unit) {
        timerJob?.cancel()
        val current = _uiState.value
        _uiState.update { it?.copy(phase = CallPhase.ENDED) }
        if (current != null) {
            viewModelScope.launch {
                repository.logCall(current.contact, current.callType, current.elapsedSeconds)
                onComplete()
            }
        } else {
            onComplete()
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
