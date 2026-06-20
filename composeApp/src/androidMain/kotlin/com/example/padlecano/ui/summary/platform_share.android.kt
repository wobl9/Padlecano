package com.example.padlecano.ui.summary

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

actual class PlatformShareHandler(
    private val context: Context,
) {
    actual fun copyToClipboard(text: String) {
        val clipboardManager: ClipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData: ClipData = ClipData.newPlainText("tournament_results", text)
        clipboardManager.setPrimaryClip(clipData)
    }
    actual fun sharePlainText(chooserTitle: String, text: String) {
        val sendIntent: Intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooserIntent: Intent = Intent.createChooser(sendIntent, chooserTitle)
        context.startActivity(chooserIntent)
    }
}

@Composable
actual fun rememberPlatformShareHandler(): PlatformShareHandler {
    val context: Context = LocalContext.current
    return remember(context) { PlatformShareHandler(context = context) }
}
