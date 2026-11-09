package com.lucanicoletti.wristotp.scan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun QrScanner(onScanned: (String) -> Unit, modifier: Modifier = Modifier)
