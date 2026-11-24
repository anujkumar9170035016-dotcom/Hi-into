package com.example.data.repository

import android.content.Context
import com.example.data.contacts.AndroidDeviceContactsReader
import com.example.data.contacts.DeviceContactsReader
import com.example.data.network.AndroidConnectivityObserver
import com.example.data.network.ConnectivityObserver
import com.example.data.onboarding.DataStoreOnboardingStore
import com.example.data.onboarding.LocalOtpAuthService
import com.example.data.onboarding.OnboardingStore
import com.example.data.onboarding.PhoneAuthService
import com.example.data.telephony.AndroidNativeTelephonyBridge
import com.example.data.telephony.NativeTelephonyBridge
import com.example.data.telephony.TelephonyDispatchResult
import com.example.domain.model.DeviceContactEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lightweight dependency locator.
 * Call [init] from MainActivity.onCreate so real Android connectivity, telephony, DataStore,
 * and ContactsContract bridges are wired before UI composition.
 */
object ServiceLocator {
    private var appContext: Context? = null

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    val messagingRepository: MessagingRepository by lazy {
        InMemoryRealtimeMessagingRepository()
    }

    val connectivityObserver: ConnectivityObserver by lazy {
        val ctx = appContext
        if (ctx != null) AndroidConnectivityObserver(ctx) else FallbackConnectivityObserver()
    }

    val telephonyBridge: NativeTelephonyBridge by lazy {
        val ctx = appContext
        if (ctx != null) AndroidNativeTelephonyBridge(ctx) else FallbackTelephonyBridge()
    }

    val onboardingStore: OnboardingStore by lazy {
        val ctx = appContext
        if (ctx != null) DataStoreOnboardingStore(ctx) else FallbackOnboardingStore()
    }

    val phoneAuthService: PhoneAuthService by lazy {
        LocalOtpAuthService()
    }

    val deviceContactsReader: DeviceContactsReader by lazy {
        val ctx = appContext
        if (ctx != null) AndroidDeviceContactsReader(ctx) else FallbackContactsReader()
    }
}

private class FallbackConnectivityObserver : ConnectivityObserver {
    private val _systemOnline = MutableStateFlow(true)
    private val _simulatedOffline = MutableStateFlow(false)
    override val isSystemOnline: StateFlow<Boolean> = _systemOnline.asStateFlow()
    override val simulatedOffline: StateFlow<Boolean> = _simulatedOffline.asStateFlow()
    override val isOnline: StateFlow<Boolean> = _systemOnline.asStateFlow()
    override fun setSimulatedOffline(offline: Boolean) {
        _simulatedOffline.value = offline
        _systemOnline.value = !offline
    }
}

private class FallbackTelephonyBridge : NativeTelephonyBridge {
    override fun hasTelephonyHardware(): Boolean = false
    override fun launchCellularDialer(phoneNumber: String): TelephonyDispatchResult =
        TelephonyDispatchResult.NoHandlerApp(phoneNumber)
    override fun launchSmsComposer(phoneNumber: String, messageBody: String): TelephonyDispatchResult =
        TelephonyDispatchResult.NoHandlerApp(phoneNumber)
}

private class FallbackOnboardingStore : OnboardingStore {
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

private class FallbackContactsReader : DeviceContactsReader {
    override fun hasContactsPermission(): Boolean = false
    override suspend fun readSavedPhoneContacts(): List<DeviceContactEntry> = emptyList()
}
