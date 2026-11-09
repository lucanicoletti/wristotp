package com.lucanicoletti.wristotp

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lucanicoletti.wristotp.settings.PhoneSettings
import com.lucanicoletti.wristotp.sync.SyncViewModel
import com.lucanicoletti.wristotp.sync.WatchBridge
import com.lucanicoletti.wristotp.ui.MainScreen
import com.lucanicoletti.wristotp.ui.WristOtpTheme

@Composable
fun App(watchBridge: WatchBridge, settings: PhoneSettings) {
    WristOtpTheme {
        MainScreen(viewModel { SyncViewModel(watchBridge, settings) })
    }
}
