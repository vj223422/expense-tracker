package com.expensetracker.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.expensetracker.app.navigation.ExpenseTrackerApp

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* alerts simply stay in-app if declined */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestHighestRefreshRate()
        requestNotificationPermissionIfNeeded()

        setContent {
            ExpenseTrackerApp()
        }
    }

    /**
     * Only asks once per process — without [hasRequestedNotificationPermission] this re-launches
     * on every onCreate, including every rotation, re-showing the system dialog each time until
     * the user permanently denies it.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || hasRequestedNotificationPermission) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        hasRequestedNotificationPermission = true
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    /**
     * Many devices (Samsung especially) default third-party apps to 60Hz even on 90/120Hz
     * hardware — Android only auto-matches refresh rate to content on some OEM skins, not all.
     * Explicitly requesting the display's highest-refresh-rate mode here is what actually makes
     * scrolling and the tab-switch/screen transitions render at that rate; on devices that
     * already auto-switch, this is a harmless no-op.
     */
    private fun requestHighestRefreshRate() {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        } ?: return

        val bestMode = display.supportedModes.maxByOrNull { it.refreshRate } ?: return
        window.attributes = window.attributes.apply {
            preferredDisplayModeId = bestMode.modeId
        }
    }

    private companion object {
        private var hasRequestedNotificationPermission = false
    }
}
