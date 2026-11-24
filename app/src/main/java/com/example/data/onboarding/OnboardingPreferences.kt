package com.example.data.onboarding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "pingbridge_onboarding"
)

interface OnboardingStore {
    val isOnboardingComplete: Flow<Boolean>
    val verifiedPhoneNumber: Flow<String?>
    val contactsAccessGranted: Flow<Boolean>

    suspend fun completeOnboarding(phoneNumber: String, contactsGranted: Boolean)
    suspend fun updateContactsGranted(granted: Boolean)
    suspend fun resetOnboarding()
}

class DataStoreOnboardingStore(
    private val context: Context
) : OnboardingStore {

    private object Keys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val VERIFIED_PHONE = stringPreferencesKey("verified_phone")
        val CONTACTS_GRANTED = booleanPreferencesKey("contacts_granted")
    }

    override val isOnboardingComplete: Flow<Boolean> =
        context.onboardingDataStore.data.map { prefs ->
            prefs[Keys.ONBOARDING_COMPLETE] ?: false
        }

    override val verifiedPhoneNumber: Flow<String?> =
        context.onboardingDataStore.data.map { prefs ->
            prefs[Keys.VERIFIED_PHONE]
        }

    override val contactsAccessGranted: Flow<Boolean> =
        context.onboardingDataStore.data.map { prefs ->
            prefs[Keys.CONTACTS_GRANTED] ?: false
        }

    override suspend fun completeOnboarding(phoneNumber: String, contactsGranted: Boolean) {
        context.onboardingDataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETE] = true
            prefs[Keys.VERIFIED_PHONE] = phoneNumber
            prefs[Keys.CONTACTS_GRANTED] = contactsGranted
        }
    }

    override suspend fun updateContactsGranted(granted: Boolean) {
        context.onboardingDataStore.edit { prefs ->
            prefs[Keys.CONTACTS_GRANTED] = granted
        }
    }

    override suspend fun resetOnboarding() {
        context.onboardingDataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETE] = false
        }
    }
}
