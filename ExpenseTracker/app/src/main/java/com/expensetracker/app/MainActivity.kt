package com.expensetracker.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.expensetracker.app.navigation.ExpenseTrackerApp

class MainActivity : ComponentActivity() {

    private var openAddExpenseRequest by mutableLongStateOf(0L)
    private var openEditExpenseRequest by mutableLongStateOf(0L)
    private var notificationExpenseId by mutableStateOf<Long?>(null)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { requestSmsPermissionIfNeeded() }

    private val smsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* SMS automation simply stays disabled if declined */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestHighestRefreshRate()
        requestNotificationPermissionIfNeeded()
        handleNavigationIntent(intent)

        setContent {
            ExpenseTrackerApp(
                openAddExpenseRequest = openAddExpenseRequest,
                openEditExpenseRequest = openEditExpenseRequest,
                notificationExpenseId = notificationExpenseId,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNavigationIntent(intent)
    }

    private fun handleNavigationIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_OPEN_ADD_EXPENSE -> {
                openAddExpenseRequest++
                intent.action = null
            }
            ACTION_OPEN_EDIT_EXPENSE -> {
                val expenseId = intent.getLongExtra(EXTRA_EXPENSE_ID, -1L)
                if (expenseId > 0L) {
                    notificationExpenseId = expenseId
                    openEditExpenseRequest++
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

    companion object {
        const val ACTION_OPEN_ADD_EXPENSE = "com.expensetracker.app.action.OPEN_ADD_EXPENSE"
        const val ACTION_OPEN_EDIT_EXPENSE = "com.expensetracker.app.action.OPEN_EDIT_EXPENSE"
        const val EXTRA_EXPENSE_ID = "extra_expense_id"

        private var hasRequestedNotificationPermission = false
        private var hasRequestedSmsPermission = false
    }
}
