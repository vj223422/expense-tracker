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

    override fun observeAllExpenses(profileId: Long): Flow<List<Expense>> =
        expenseDao.observeAll(profileId).map { entities -> entities.map { it.toDomain() } }

    override fun observeExpensesForMonth(profileId: Long, yearMonth: YearMonth): Flow<List<Expense>> =
        expenseDao.observeBetween(profileId, yearMonth.startEpochDay, yearMonth.endEpochDay)
            .map { entities -> entities.map { it.toDomain() } }

    override fun observeCategoryTotals(profileId: Long, yearMonth: YearMonth): Flow<Map<ExpenseCategory, Long>> =
        expenseDao.observeCategoryTotals(profileId, yearMonth.startEpochDay, yearMonth.endEpochDay)
            .map { totals -> totals.associate { it.category to it.totalMinor } }

    override fun observeMonthlyTotal(profileId: Long, yearMonth: YearMonth): Flow<Long> =
        observeCategoryTotals(profileId, yearMonth).map { totals -> totals.values.sum() }

    override suspend fun addExpense(
        profileId: Long,
        amountMinor: Long,
        category: ExpenseCategory,
        note: String,
        date: LocalDate,
    ): AddExpenseResult = withContext(ioDispatcher) {
        try {
            expenseDao.insert(
                ExpenseEntity(
                    profileId = profileId,
                    amountMinor = amountMinor,
                    category = category,
                    note = note,
                    epochDay = date.toEpochDay(),
                    createdAtEpochMillis = System.currentTimeMillis(),
                ),
            )
        } catch (e: SQLiteException) {
            return@withContext AddExpenseResult.Error("Couldn't save expense — local storage error.")
        }

        // The expense is already committed at this point, so a failure here is a lost/duplicate
        // alert at worst — it must never be reported back as a failed save, or the caller (and
        // user) may retry and insert the same expense twice.
        val alerts = try {
            evaluateAndNotify(profileId, YearMonth.from(date), category)
        } catch (e: SQLiteException) {
            emptyList()
        }
        AddExpenseResult.Success(alerts)
    }

    override suspend fun getExpenseById(id: Long, profileId: Long): Expense? = withContext(ioDispatcher) {
        expenseDao.getById(id, profileId)?.toDomain()
    }

    override suspend fun updateExpense(expense: Expense): AddExpenseResult = withContext(ioDispatcher) {
        try {
            expenseDao.update(expense.toEntity())
        } catch (e: SQLiteException) {
            return@withContext AddExpenseResult.Error("Couldn't update expense — local storage error.")
        }

        val alerts = try {
            evaluateAndNotify(expense.profileId, YearMonth.from(expense.date), expense.category)
        } catch (e: SQLiteException) {
            emptyList()
        }
        AddExpenseResult.Success(alerts)
    }

    override suspend fun deleteExpense(expense: Expense) = withContext(ioDispatcher) {
        expenseDao.delete(expense.toEntity())
    }

    override suspend fun restoreExpense(expense: Expense): AddExpenseResult = withContext(ioDispatcher) {
        try {
            // expense.toEntity() carries the original id/createdAtEpochMillis (unlike building a
            // fresh ExpenseEntity the way addExpense does), so the restored row keeps its original
            // identity and position instead of jumping to the top of its date group as "new".
            expenseDao.insert(expense.toEntity())
        } catch (e: SQLiteException) {
            return@withContext AddExpenseResult.Error("Couldn't restore expense — local storage error.")
        }

        val alerts = try {
            evaluateAndNotify(expense.profileId, YearMonth.from(expense.date), expense.category)
        } catch (e: SQLiteException) {
            emptyList()
        }
        AddExpenseResult.Success(alerts)
    }

    /** Notifies at most once per (profile, period, scope) for WARNING; see maybeAlert for CRITICAL. */
    private suspend fun evaluateAndNotify(profileId: Long, yearMonth: YearMonth, changedCategory: ExpenseCategory): List<LimitAlert> {
        val alerts = mutableListOf<LimitAlert>()
        val periodPrefix = "${profileId}_$yearMonth"

        budgetLimitDao.getByKey(profileId, changedCategory.name)?.let { limit ->
            val spent = expenseDao.getCategoryTotal(profileId, changedCategory, yearMonth.startEpochDay, yearMonth.endEpochDay)
            maybeAlert(profileId, "${periodPrefix}_${changedCategory.name}", changedCategory, spent, limit.limitMinor)
                ?.let(alerts::add)
        }
        budgetLimitDao.getByKey(profileId, OVERALL_BUDGET_KEY)?.let { limit ->
            val spent = expenseDao.getOverallTotal(profileId, yearMonth.startEpochDay, yearMonth.endEpochDay)
            maybeAlert(profileId, "${periodPrefix}_$OVERALL_BUDGET_KEY", null, spent, limit.limitMinor)
                ?.let(alerts::add)
        }
        return alerts
    }

    /**
     * WARNING notifies once per (period, scope) — suppressed once that tier is already recorded.
     * CRITICAL notifies unconditionally on every call: once spending is at or past 80% of the
     * limit, every further expense in that scope this month is worth a fresh alert, not just the
     * first crossing — including every expense after 100% is passed too.
     */
    private suspend fun maybeAlert(
        profileId: Long,
        periodKey: String,
        category: ExpenseCategory?,
        spentMinor: Long,
        limitMinor: Long,
    ): LimitAlert? {
        val tier = LimitAlertEvaluator.tierFor(spentMinor, limitMinor)
        if (tier == AlertTier.NONE) return null

        val previousTier = appPreferences.getLastNotifiedTier(periodKey)
        val shouldNotify = tier == AlertTier.CRITICAL || tier.ordinal > previousTier
        if (!shouldNotify) return null

        if (tier.ordinal > previousTier) {
            appPreferences.setLastNotifiedTier(periodKey, tier.ordinal)
        }
        val alert = LimitAlert(profileId, category, tier, spentMinor, limitMinor)
        notificationHelper.notify(alert)
        return alert
    }
}
