package com.expensetracker.app.data.repository

import android.database.sqlite.SQLiteException
import com.expensetracker.app.data.entity.ExpenseEntity
import com.expensetracker.app.data.entity.OVERALL_BUDGET_KEY
import com.expensetracker.app.data.local.dao.BudgetLimitDao
import com.expensetracker.app.data.local.dao.ExpenseDao
import com.expensetracker.app.data.model.Expense
import com.expensetracker.app.data.model.ExpenseCategory
import com.expensetracker.app.data.notification.AlertTier
import com.expensetracker.app.data.notification.LimitAlert
import com.expensetracker.app.data.notification.LimitAlertEvaluator
import com.expensetracker.app.data.notification.NotificationHelper
import com.expensetracker.app.data.prefs.AppPreferences
import com.expensetracker.app.core.util.endEpochDay
import com.expensetracker.app.core.util.startEpochDay
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

class ExpenseRepositoryImpl(
    private val expenseDao: ExpenseDao,
    private val budgetLimitDao: BudgetLimitDao,
    private val notificationHelper: NotificationHelper,
    private val appPreferences: AppPreferences,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ExpenseRepository {

    override fun observeAllExpenses(): Flow<List<Expense>> =
        expenseDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeExpensesForMonth(yearMonth: YearMonth): Flow<List<Expense>> =
        expenseDao.observeBetween(yearMonth.startEpochDay, yearMonth.endEpochDay)
            .map { entities -> entities.map { it.toDomain() } }

    override fun observeCategoryTotals(yearMonth: YearMonth): Flow<Map<ExpenseCategory, Long>> =
        expenseDao.observeCategoryTotals(yearMonth.startEpochDay, yearMonth.endEpochDay)
            .map { totals -> totals.associate { it.category to it.totalMinor } }

    override fun observeMonthlyTotal(yearMonth: YearMonth): Flow<Long> =
        observeCategoryTotals(yearMonth).map { totals -> totals.values.sum() }

    override suspend fun addExpense(
        amountMinor: Long,
        category: ExpenseCategory,
        note: String,
        date: LocalDate,
    ): AddExpenseResult = withContext(ioDispatcher) {
        try {
            expenseDao.insert(
                ExpenseEntity(
                    amountMinor = amountMinor,
                    category = category,
                    note = note,
                    epochDay = date.toEpochDay(),
                    createdAtEpochMillis = System.currentTimeMillis(),
                ),
            )
            AddExpenseResult.Success(evaluateAndNotify(YearMonth.from(date), category))
        } catch (e: SQLiteException) {
            AddExpenseResult.Error("Couldn't save expense — local storage error.")
        }
    }

    override suspend fun deleteExpense(expense: Expense) = withContext(ioDispatcher) {
        expenseDao.delete(expense.toEntity())
    }

    /** Notifies at most once per (period, scope, tier) — see AppPreferences.getLastNotifiedTier. */
    private suspend fun evaluateAndNotify(yearMonth: YearMonth, changedCategory: ExpenseCategory): List<LimitAlert> {
        val alerts = mutableListOf<LimitAlert>()
        val periodPrefix = yearMonth.toString()

        budgetLimitDao.getByKey(changedCategory.name)?.let { limit ->
            val spent = expenseDao.getCategoryTotal(changedCategory, yearMonth.startEpochDay, yearMonth.endEpochDay)
            maybeAlert("${periodPrefix}_${changedCategory.name}", changedCategory, spent, limit.limitMinor)
                ?.let(alerts::add)
        }
        budgetLimitDao.getByKey(OVERALL_BUDGET_KEY)?.let { limit ->
            val spent = expenseDao.getOverallTotal(yearMonth.startEpochDay, yearMonth.endEpochDay)
            maybeAlert("${periodPrefix}_$OVERALL_BUDGET_KEY", null, spent, limit.limitMinor)
                ?.let(alerts::add)
        }
        return alerts
    }

    private suspend fun maybeAlert(
        periodKey: String,
        category: ExpenseCategory?,
        spentMinor: Long,
        limitMinor: Long,
    ): LimitAlert? {
        val tier = LimitAlertEvaluator.tierFor(spentMinor, limitMinor)
        if (tier == AlertTier.NONE) return null
        if (tier.ordinal <= appPreferences.getLastNotifiedTier(periodKey)) return null

        appPreferences.setLastNotifiedTier(periodKey, tier.ordinal)
        val alert = LimitAlert(category, tier, spentMinor, limitMinor)
        notificationHelper.notify(alert)
        return alert
    }
}
