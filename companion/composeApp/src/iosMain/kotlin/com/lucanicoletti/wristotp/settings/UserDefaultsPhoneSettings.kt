package com.lucanicoletti.wristotp.settings

import platform.Foundation.NSUserDefaults

class UserDefaultsPhoneSettings : PhoneSettings {
    private val defaults = NSUserDefaults.standardUserDefaults

    override var pinRequired: Boolean
        get() = defaults.objectForKey(KEY_PIN_REQUIRED) == null || defaults.boolForKey(KEY_PIN_REQUIRED)
        set(value) {
            defaults.setBool(value, forKey = KEY_PIN_REQUIRED)
        }

    override var lastSync: LastSync?
        get() {
            if (defaults.objectForKey(KEY_LAST_SYNC_TIME) == null) return null
            return LastSync(
                epochSeconds = defaults.integerForKey(KEY_LAST_SYNC_TIME),
                accountCount = defaults.integerForKey(KEY_LAST_SYNC_COUNT).toInt(),
                deviceName = defaults.stringForKey(KEY_LAST_SYNC_DEVICE).orEmpty(),
            )
        }
        set(value) {
            if (value == null) {
                defaults.removeObjectForKey(KEY_LAST_SYNC_TIME)
                defaults.removeObjectForKey(KEY_LAST_SYNC_COUNT)
                defaults.removeObjectForKey(KEY_LAST_SYNC_DEVICE)
            } else {
                defaults.setInteger(value.epochSeconds, forKey = KEY_LAST_SYNC_TIME)
                defaults.setInteger(value.accountCount.toLong(), forKey = KEY_LAST_SYNC_COUNT)
                defaults.setObject(value.deviceName, forKey = KEY_LAST_SYNC_DEVICE)
            }
        }

    private companion object {
        const val KEY_PIN_REQUIRED = "pinRequired"
        const val KEY_LAST_SYNC_TIME = "lastSyncTime"
        const val KEY_LAST_SYNC_COUNT = "lastSyncCount"
        const val KEY_LAST_SYNC_DEVICE = "lastSyncDevice"
    }
}
