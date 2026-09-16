package com.expensetracker.app.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.expensetracker.app.data.repository.ExpenseRepository
import com.expensetracker.app.data.repository.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BudgetCycleActionReceiver : BroadcastReceiver(), KoinComponent {
    private val expenseRepository: ExpenseRepository by inject()
    private val profileRepository: ProfileRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_START_BUDGET) return
        val expenseId = intent.getLongExtra(EXTRA_EXPENSE_ID, -1L)
        if (expenseId <= 0L) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val profileId = profileRepository.observeActiveProfileId().filterNotNull().first()
                expenseRepository.startBudgetCycle(profileId, expenseId)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_START_BUDGET = "com.expensetracker.app.action.START_BUDGET_CYCLE"
        const val EXTRA_EXPENSE_ID = "expense_id"
    }
}
