package com.lucanicoletti.wristotp

import androidx.compose.ui.window.ComposeUIViewController
import com.lucanicoletti.wristotp.settings.UserDefaultsPhoneSettings
import com.lucanicoletti.wristotp.sync.IosWatchBridge
import com.lucanicoletti.wristotp.sync.IosWatchTransport
import platform.UIKit.UIViewController

fun MainViewController(transport: IosWatchTransport): UIViewController {
    val watchBridge = IosWatchBridge(transport)
    val settings = UserDefaultsPhoneSettings()
    return ComposeUIViewController { App(watchBridge, settings) }
}
