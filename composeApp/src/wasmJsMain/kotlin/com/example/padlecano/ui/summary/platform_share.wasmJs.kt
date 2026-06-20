package com.example.padlecano.ui.summary

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual class PlatformShareHandler {
    actual fun copyToClipboard(text: String) {
    }
    actual fun sharePlainText(chooserTitle: String, text: String) {
    }
}

@Composable
actual fun rememberPlatformShareHandler(): PlatformShareHandler {
    return remember { PlatformShareHandler() }
}
