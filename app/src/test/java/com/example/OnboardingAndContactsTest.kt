package com.example

import com.example.data.contacts.AndroidDeviceContactsReader
import com.example.data.contacts.DeviceContactsReader
import com.example.data.onboarding.LocalOtpAuthService
import com.example.data.onboarding.OnboardingStore
import com.example.data.onboarding.OtpRequestResult
import com.example.data.onboarding.OtpVerifyResult
import com.example.data.repository.InMemoryRealtimeMessagingRepository
import com.example.domain.model.DeviceContactEntry
import com.example.ui.onboarding.ContactsPermissionStatus
import com.example.ui.onboarding.OnboardingStep
import com.example.ui.onboarding.OnboardingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingAndContactsTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun localOtpAuthServiceValidatesPhoneAndSixDigitOtp() = runTest {
        val authService = LocalOtpAuthService()

        val invalid = authService.requestOtp("+1", "123")
        assertTrue(invalid is OtpRequestResult.InvalidNumber)

        val sent = authService.requestOtp("+1", "(415) 555-0192")
        assertTrue(sent is OtpRequestResult.Sent)
        val challenge = sent as OtpRequestResult.Sent
        assertEquals("+1 4155550192", challenge.formattedPhone)
        assertEquals(6, challenge.previewCodeForDemo.length)

        val wrong = authService.verifyOtp(challenge.challengeId, "000000")
        if (challenge.previewCodeForDemo != "000000") {
            assertTrue(wrong is OtpVerifyResult.Failed)
        }

        val shortCode = authService.verifyOtp(challenge.challengeId, "123")
        assertTrue(shortCode is OtpVerifyResult.Failed)

        val verified = authService.verifyOtp(challenge.challengeId, challenge.previewCodeForDemo)
        assertTrue(verified is OtpVerifyResult.Verified)
        assertEquals("+1 4155550192", (verified as OtpVerifyResult.Verified).formattedPhone)
    }

    @Test
    fun normalizePhoneDigitsAndSyncDeviceContactsMatchAndAppend() = runTest {
        assertEquals("4158903312", AndroidDeviceContactsReader.normalizePhoneDigits("+1 (415) 890-3312"))
        assertEquals("6504427819", AndroidDeviceContactsReader.normalizePhoneDigits("650-442-7819"))

        val repo = InMemoryRealtimeMessagingRepository()
        val initialCount = repo.contacts.value.size

        val matched = repo.syncDeviceContacts(
            listOf(
                DeviceContactEntry(
                    deviceId = "d1",
                    displayName = "Maya Mobile",
                    phoneNumber = "415-890-3312",
                    normalizedDigits = "4158903312"
                ),
                DeviceContactEntry(
                    deviceId = "d2",
                    displayName = "Alex Rivera",
                    phoneNumber = "+1 510 555 0144",
                    normalizedDigits = "5105550144"
                )
            )
        )

        assertEquals(2, matched)
        val contacts = repo.contacts.value
        assertEquals(initialCount + 1, contacts.size)
        val maya = contacts.first { it.id == "c_maya" }
        assertTrue(maya.isMatchedFromPhoneBook)
        val alex = contacts.first { it.name == "Alex Rivera" }
        assertTrue(alex.isMatchedFromPhoneBook)
        assertEquals("+1 510 555 0144", alex.phoneNumber)
    }

    @Test
    fun onboardingViewModelHandlesOtpFlowAndGracefulContactsDenial() = runTest {
        val fakeStore = FakeOnboardingStore()
        val fakeReader = FakeDeviceContactsReader(
            permission = false,
            entries = listOf(
                DeviceContactEntry("d1", "Samira Khan", "+1 415 555 0188", "4155550188")
            )
        )
        val repo = InMemoryRealtimeMessagingRepository()
        val vm = OnboardingViewModel(
            phoneAuthService = LocalOtpAuthService(),
            onboardingStore = fakeStore,
            contactsReader = fakeReader,
            repository = repo
        )

        vm.updatePhoneNumber("4155550199")
        vm.requestOtp()
        testScheduler.advanceUntilIdle()

        assertEquals(OnboardingStep.OTP_VERIFY, vm.uiState.value.step)
        vm.autofillDemoOtp()
        assertEquals(6, vm.uiState.value.otpCode.length)

        vm.verifyOtp()
        testScheduler.advanceUntilIdle()
        assertEquals(OnboardingStep.CONTACTS_PERMISSION, vm.uiState.value.step)

        // User denies READ_CONTACTS -> handled gracefully without blocking
        vm.onContactsPermissionResult(granted = false)
        assertEquals(ContactsPermissionStatus.DENIED, vm.uiState.value.contactsStatus)

        vm.finishOnboarding(contactsGranted = false)
        testScheduler.advanceUntilIdle()
        assertTrue(fakeStore.isOnboardingComplete.first())
        assertFalse(fakeStore.contactsAccessGranted.first())
        assertEquals("+1 4155550199", fakeStore.verifiedPhoneNumber.first())
    }
}

private class FakeOnboardingStore : OnboardingStore {
    private val _complete = MutableStateFlow(false)
    private val _phone = MutableStateFlow<String?>(null)
    private val _contacts = MutableStateFlow(false)

    override val isOnboardingComplete: Flow<Boolean> = _complete
    override val verifiedPhoneNumber: Flow<String?> = _phone
    override val contactsAccessGranted: Flow<Boolean> = _contacts

    override suspend fun completeOnboarding(phoneNumber: String, contactsGranted: Boolean) {
        _phone.value = phoneNumber
        _contacts.value = contactsGranted
        _complete.value = true
    }

    override suspend fun updateContactsGranted(granted: Boolean) {
        _contacts.value = granted
    }

    override suspend fun resetOnboarding() {
        _complete.value = false
    }
}

private class FakeDeviceContactsReader(
    private val permission: Boolean,
    private val entries: List<DeviceContactEntry>
) : DeviceContactsReader {
    override fun hasContactsPermission(): Boolean = permission
    override suspend fun readSavedPhoneContacts(): List<DeviceContactEntry> = entries
}
