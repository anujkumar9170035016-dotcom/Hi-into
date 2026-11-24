package com.example.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.contacts.DeviceContactsReader
import com.example.data.onboarding.OnboardingStore
import com.example.data.onboarding.OtpRequestResult
import com.example.data.onboarding.OtpVerifyResult
import com.example.data.onboarding.PhoneAuthService
import com.example.data.repository.MessagingRepository
import com.example.data.repository.ServiceLocator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingStep {
    PHONE_INPUT,
    OTP_VERIFY,
    CONTACTS_PERMISSION
}

enum class ContactsPermissionStatus {
    IDLE,
    SYNCED,
    DENIED
}

data class CountryCodeOption(
    val flag: String,
    val name: String,
    val dialCode: String
)

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.PHONE_INPUT,
    val selectedCountry: CountryCodeOption = CountryCodeOption("🇺🇸", "United States", "+1"),
    val rawPhoneNumber: String = "",
    val formattedPhone: String = "",
    val challengeId: String = "",
    val demoOtpHint: String = "",
    val otpCode: String = "",
    val resendCooldownSeconds: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val contactsStatus: ContactsPermissionStatus = ContactsPermissionStatus.IDLE,
    val matchedContactsCount: Int = 0
)

class OnboardingViewModel(
    private val phoneAuthService: PhoneAuthService = ServiceLocator.phoneAuthService,
    private val onboardingStore: OnboardingStore = ServiceLocator.onboardingStore,
    private val contactsReader: DeviceContactsReader = ServiceLocator.deviceContactsReader,
    private val repository: MessagingRepository = ServiceLocator.messagingRepository
) : ViewModel() {

    val countryOptions = listOf(
        CountryCodeOption("🇺🇸", "United States", "+1"),
        CountryCodeOption("🇮🇳", "India", "+91"),
        CountryCodeOption("🇬🇧", "United Kingdom", "+44"),
        CountryCodeOption("🇨🇦", "Canada", "+1"),
        CountryCodeOption("🇦🇺", "Australia", "+61"),
        CountryCodeOption("🇩🇪", "Germany", "+49"),
        CountryCodeOption("🇯🇵", "Japan", "+81")
    )

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private var cooldownJob: Job? = null

    fun selectCountry(option: CountryCodeOption) {
        _uiState.update { it.copy(selectedCountry = option, errorMessage = null) }
    }

    fun updatePhoneNumber(input: String) {
        val cleaned = input.filter { it.isDigit() || it == '-' || it == ' ' }.take(18)
        _uiState.update { it.copy(rawPhoneNumber = cleaned, errorMessage = null) }
    }

    fun requestOtp() {
        val state = _uiState.value
        if (state.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = phoneAuthService.requestOtp(state.selectedCountry.dialCode, state.rawPhoneNumber)) {
                is OtpRequestResult.InvalidNumber -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
                is OtpRequestResult.Sent -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            step = OnboardingStep.OTP_VERIFY,
                            formattedPhone = result.formattedPhone,
                            challengeId = result.challengeId,
                            demoOtpHint = result.previewCodeForDemo,
                            otpCode = "",
                            errorMessage = null
                        )
                    }
                    startResendCooldown()
                }
            }
        }
    }

    fun resendOtp() {
        if (_uiState.value.resendCooldownSeconds > 0) return
        requestOtp()
    }

    fun editPhoneNumber() {
        cooldownJob?.cancel()
        _uiState.update {
            it.copy(
                step = OnboardingStep.PHONE_INPUT,
                otpCode = "",
                errorMessage = null,
                resendCooldownSeconds = 0
            )
        }
    }

    fun updateOtpCode(input: String) {
        val digits = input.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(otpCode = digits, errorMessage = null) }
    }

    fun autofillDemoOtp() {
        val demo = _uiState.value.demoOtpHint
        if (demo.length == 6) {
            _uiState.update { it.copy(otpCode = demo, errorMessage = null) }
        }
    }

    fun verifyOtp() {
        val state = _uiState.value
        if (state.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = phoneAuthService.verifyOtp(state.challengeId, state.otpCode)) {
                is OtpVerifyResult.Failed -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
                is OtpVerifyResult.Verified -> {
                    cooldownJob?.cancel()
                    val alreadyGranted = contactsReader.hasContactsPermission()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            step = OnboardingStep.CONTACTS_PERMISSION,
                            formattedPhone = result.formattedPhone,
                            errorMessage = null
                        )
                    }
                    if (alreadyGranted) {
                        syncContactsAfterGrant()
                    }
                }
            }
        }
    }

    fun onContactsPermissionResult(granted: Boolean) {
        if (granted) {
            syncContactsAfterGrant()
        } else {
            _uiState.update {
                it.copy(
                    contactsStatus = ContactsPermissionStatus.DENIED,
                    isLoading = false
                )
            }
        }
    }

    private fun syncContactsAfterGrant() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val entries = contactsReader.readSavedPhoneContacts()
            val matched = repository.syncDeviceContacts(entries)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    contactsStatus = ContactsPermissionStatus.SYNCED,
                    matchedContactsCount = matched
                )
            }
        }
    }

    fun finishOnboarding(contactsGranted: Boolean) {
        val phone = _uiState.value.formattedPhone.ifBlank {
            "${_uiState.value.selectedCountry.dialCode} ${_uiState.value.rawPhoneNumber}"
        }
        viewModelScope.launch {
            onboardingStore.completeOnboarding(
                phoneNumber = phone,
                contactsGranted = contactsGranted
            )
        }
    }

    private fun startResendCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (sec in 30 downTo 0) {
                _uiState.update { it.copy(resendCooldownSeconds = sec) }
                if (sec > 0) delay(1000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cooldownJob?.cancel()
    }
}
