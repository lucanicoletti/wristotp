package com.lucanicoletti.wristotp

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.Build
import com.garmin.android.connectiq.ConnectIQ
import com.lucanicoletti.wristotp.settings.SharedPreferencesPhoneSettings
import com.lucanicoletti.wristotp.sync.ConnectIqWatchBridge

class WristOtpApplication : Application() {
    lateinit var watchBridge: ConnectIqWatchBridge
        private set
    lateinit var settings: SharedPreferencesPhoneSettings
        private set

    override fun onCreate() {
        super.onCreate()
        watchBridge = ConnectIqWatchBridge(this, connectType())
        watchBridge.start()
        settings = SharedPreferencesPhoneSettings(this)
    }

    private fun connectType(): ConnectIQ.IQConnectType {
        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val emulator = Build.FINGERPRINT.startsWith("generic") || Build.HARDWARE == "ranchu" || Build.HARDWARE == "goldfish"
        return if (debuggable && emulator) ConnectIQ.IQConnectType.TETHERED else ConnectIQ.IQConnectType.WIRELESS
    }
}
