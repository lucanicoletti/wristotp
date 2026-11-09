package com.lucanicoletti.wristotp.settings

interface PhoneSettings {
    var pinRequired: Boolean
    var lastSync: LastSync?
}

data class LastSync(val epochSeconds: Long, val accountCount: Int, val deviceName: String)

class InMemoryPhoneSettings(
    override var pinRequired: Boolean = true,
    override var lastSync: LastSync? = null,
) : PhoneSettings
