package com.lucanicoletti.wristotp.scan

import androidx.compose.runtime.Composable

@Composable
expect fun rememberImageQrImporter(onResult: (List<String>) -> Unit): () -> Unit
