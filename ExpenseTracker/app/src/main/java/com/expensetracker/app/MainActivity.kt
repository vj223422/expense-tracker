package com.expensetracker.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.expensetracker.app.data.crash.CrashReporter
import com.expensetracker.app.data.prefs.AppPreferences
import com.expensetracker.app.feature.addexpense.AddExpenseViewModel
import com.expensetracker.app.navigation.ExpenseTrackerApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private var notificationNavigationRequest by mutableLongStateOf(0L)
    private var notificationNavigationAction by mutableStateOf<String?>(null)
    private var notificationExpenseId by mutableStateOf<Long?>(null)
    private var isUnlocked by mutableStateOf(false)
    private var appLockChecked by mutableStateOf(false)
    private var biometricPromptShowing = false
    private var lastCrashReport by mutableStateOf<String?>(null)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { requestSmsPermissionIfNeeded() }
    private val smsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestHighestRefreshRate()
        lastCrashReport = CrashReporter.consumeLastCrash(this)
        handleNavigationIntent(intent)

        setContent {
            if (appLockChecked && isUnlocked) {
                ExpenseTrackerApp(
                    notificationNavigationRequest = notificationNavigationRequest,
                    notificationNavigationAction = notificationNavigationAction,
                    notificationExpenseId = notificationExpenseId,
                )
            } else {
                LockedAppScreen(onUnlock = ::authenticateApp)
            }

            lastCrashReport?.let { report ->
                AlertDialog(
                    onDismissRequest = { lastCrashReport = null },
                    title = { Text("Kanakku crashed") },
                    text = {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text(
                                "The previous crash was recorded. Send this information when reporting the problem:",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                report,
                                modifier = Modifier.padding(top = 12.dp),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = { lastCrashReport = null }) { Text("Close") }
                    },
                )
            }
        }

        requestNotificationPermissionIfNeeded()
        lifecycleScope.launch {
            val appLockEnabled = AppPreferences(this@MainActivity).appLockEnabled.first()
            appLockChecked = true
            if (appLockEnabled) authenticateApp() else isUnlocked = true
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNavigationIntent(intent)
    }

    private fun authenticateApp() {
        if (biometricPromptShowing) return
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val manager = BiometricManager.from(this)
        if (manager.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            isUnlocked = true
            return
        }
        biometricPromptShowing = true
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                biometricPromptShowing = false
                isUnlocked = true
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                biometricPromptShowing = false
                isUnlocked = false
            }
            override fun onAuthenticationFailed() {
                // Keep the lock screen visible; the biometric dialog remains available for retry.
            }
        })
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Kanakku")
            .setSubtitle("Use fingerprint or your device credentials")
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(promptInfo)
    }

    private fun handleNavigationIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_OPEN_ADD_EXPENSE -> {
                notificationNavigationAction = ACTION_OPEN_ADD_EXPENSE
                notificationExpenseId = null
                notificationNavigationRequest++
                intent.action = null
            }
            ACTION_OPEN_EDIT_EXPENSE -> {
                val expenseId = intent.getLongExtra(EXTRA_EXPENSE_ID, -1L)
                if (expenseId > 0L) {
                    AddExpenseViewModel.markNotificationEdit(expenseId)
                    notificationNavigationAction = ACTION_OPEN_EDIT_EXPENSE
                    notificationExpenseId = expenseId
                    notificationNavigationRequest++
                }
                intent.action = null
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || hasRequestedNotificationPermission) {
            requestSmsPermissionIfNeeded()
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            requestSmsPermissionIfNeeded()
            return
        }
        hasRequestedNotificationPermission = true
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun requestSmsPermissionIfNeeded() {
        if (hasRequestedSmsPermission) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) return
        hasRequestedSmsPermission = true
        smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
    }

    private fun requestHighestRefreshRate() {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display else {
            @Suppress("DEPRECATION") windowManager.defaultDisplay
        } ?: return
        val bestMode = display.supportedModes.maxByOrNull { it.refreshRate } ?: return
        window.attributes = window.attributes.apply { preferredDisplayModeId = bestMode.modeId }
    }

    companion object {
        const val ACTION_OPEN_ADD_EXPENSE = "com.expensetracker.app.action.OPEN_ADD_EXPENSE"
        const val ACTION_OPEN_EDIT_EXPENSE = "com.expensetracker.app.action.OPEN_EDIT_EXPENSE"
        const val EXTRA_EXPENSE_ID = "extra_expense_id"
        private var hasRequestedNotificationPermission = false
        private var hasRequestedSmsPermission = false
    }
}

@androidx.compose.runtime.Composable
private fun LockedAppScreen(onUnlock: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 16.dp))
        Text("Kanakku is locked", style = MaterialTheme.typography.headlineSmall)
        Text("Authenticate to view your expenses", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
        Button(onClick = onUnlock) { Text("Unlock") }
    }
}
