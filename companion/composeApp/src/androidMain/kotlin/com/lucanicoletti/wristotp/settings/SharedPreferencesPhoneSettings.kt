package com.lucanicoletti.wristotp.settings

import android.content.Context

class SharedPreferencesPhoneSettings(context: Context) : PhoneSettings {
    private val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    override var pinRequired: Boolean
        get() = preferences.getBoolean(KEY_PIN_REQUIRED, true)
        set(value) {
            preferences.edit().putBoolean(KEY_PIN_REQUIRED, value).apply()
        }

    override var lastSync: LastSync?
        get() {
            if (!preferences.contains(KEY_LAST_SYNC_TIME)) return null
            return LastSync(
                epochSeconds = preferences.getLong(KEY_LAST_SYNC_TIME, 0),
                accountCount = preferences.getInt(KEY_LAST_SYNC_COUNT, 0),
                deviceName = preferences.getString(KEY_LAST_SYNC_DEVICE, null).orEmpty(),
            )
        }
        set(value) {
            preferences.edit().apply {
                if (value == null) {
                    remove(KEY_LAST_SYNC_TIME)
                    remove(KEY_LAST_SYNC_COUNT)
                    remove(KEY_LAST_SYNC_DEVICE)
                } else {
                    putLong(KEY_LAST_SYNC_TIME, value.epochSeconds)
                    putInt(KEY_LAST_SYNC_COUNT, value.accountCount)
                    putString(KEY_LAST_SYNC_DEVICE, value.deviceName)
                }
            }.apply()
        }

    private companion object {
        const val KEY_PIN_REQUIRED = "pinRequired"
        const val KEY_LAST_SYNC_TIME = "lastSyncTime"
        const val KEY_LAST_SYNC_COUNT = "lastSyncCount"
        const val KEY_LAST_SYNC_DEVICE = "lastSyncDevice"
    }
}
