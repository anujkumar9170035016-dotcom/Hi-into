package com.example.data.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.domain.model.DeviceContactEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface DeviceContactsReader {
    fun hasContactsPermission(): Boolean
    suspend fun readSavedPhoneContacts(): List<DeviceContactEntry>
}

/**
 * Reads saved phone contacts via Android ContactsContract when READ_CONTACTS is granted.
 * Normalizes phone numbers so contacts can be matched for online PingBridge chat
 * or offline SMS / cellular tel: fallback.
 */
class AndroidDeviceContactsReader(
    private val context: Context
) : DeviceContactsReader {

    override fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

    override suspend fun readSavedPhoneContacts(): List<DeviceContactEntry> = withContext(Dispatchers.IO) {
        if (!hasContactsPermission()) return@withContext emptyList()

        val result = linkedMapOf<String, DeviceContactEntry>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (cursor.moveToNext()) {
                    val rawNumber = if (numberIdx >= 0) cursor.getString(numberIdx).orEmpty() else ""
                    val normalized = normalizePhoneDigits(rawNumber)
                    if (normalized.length < 7) continue

                    val name = if (nameIdx >= 0) cursor.getString(nameIdx)?.trim().orEmpty() else ""
                    val contactId = if (idIdx >= 0) cursor.getString(idIdx).orEmpty() else normalized
                    if (name.isBlank()) continue

                    if (!result.containsKey(normalized)) {
                        result[normalized] = DeviceContactEntry(
                            deviceId = contactId,
                            displayName = name,
                            phoneNumber = rawNumber.trim(),
                            normalizedDigits = normalized
                        )
                    }
                }
            }
        }
        result.values.toList()
    }

    companion object {
        fun normalizePhoneDigits(phone: String): String {
            val digits = phone.filter { it.isDigit() }
            return if (digits.length > 10) digits.takeLast(10) else digits
        }
    }
}
