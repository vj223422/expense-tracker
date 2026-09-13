package com.expensetracker.app.feature.upipay

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext as ComposeLocalContext

/**
 * Compatibility bridge for the Scan & Pay screen. The screen historically referenced
 * LocalContext without importing Compose's LocalContext; keeping this bridge avoids
 * changing the generated UI code while delegating to the real Compose local.
 */
object LocalContext {
    val current: Context
        @Composable
        get() = ComposeLocalContext.current
}
