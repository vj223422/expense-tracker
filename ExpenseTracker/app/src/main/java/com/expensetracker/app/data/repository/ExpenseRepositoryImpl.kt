package com.expensetracker.app.data.repository

import android.database.sqlite.SQLiteException
import com.expensetracker.app.core.util.endEpochDay
import com.expensetracker.app.core.util.startEpochDay
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

class ExpenseRepositoryImpl(private val expenseDao: ExpenseDao, private val budgetLimitDao: BudgetLimitDao, private val notificationHelper: NotificationHelper, private val appPreferences: AppPreferences, private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) : ExpenseRepository {
    override fun observeAllExpenses(profileId: Long): Flow<List<Expense>> = expenseDao.observeAll(profileId).map { it.map(ExpenseEntity::toDomain) }
    override fun observeExpensesForMonth(profileId: Long, yearMonth: YearMonth): Flow<List<Expense>> = expenseDao.observeBetween(profileId, yearMonth.startEpochDay, yearMonth.endEpochDay).map { it.map(ExpenseEntity::toDomain) }
    override fun observeExpensesForBudgetPeriod(profileId: Long, yearMonth: YearMonth): Flow<List<Expense>> = expenseDao.observeBudgetPeriod(profileId, yearMonth.toString()).map { it.map(ExpenseEntity::toDomain) }
    override fun observeCategoryTotals(profileId: Long, yearMonth: YearMonth): Flow<Map<ExpenseCategory, Long>> = expenseDao.observeCategoryTotalsForBudget(profileId, yearMonth.toString()).map { totals -> totals.associate { it.category to it.totalMinor } }
    override fun observeMonthlyTotal(profileId: Long, yearMonth: YearMonth): Flow<Long> = observeCategoryTotals(profileId, yearMonth).map { it.values.sum() }
    override fun observeIncomeTotal(profileId: Long, yearMonth: YearMonth): Flow<Long> = expenseDao.observeIncomeTotalForBudget(profileId, yearMonth.toString())

    override suspend fun addExpense(profileId: Long, amountMinor: Long, category: ExpenseCategory, note: String, date: LocalDate, isIncome: Boolean): AddExpenseResult = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val activeBudgetMonth = appPreferences.getActiveBudgetMonth(profileId) ?: YearMonth.from(date).toString()
        val activeCycleStart = appPreferences.getActiveBudgetCycleStart(profileId) ?: 0L
        val budgetMonth = if (activeCycleStart > 0L && now >= activeCycleStart) activeBudgetMonth else YearMonth.from(date).toString()
        val cycleStart = if (activeCycleStart > 0L && now >= activeCycleStart) activeCycleStart else 0L
        val expenseId = try {
            expenseDao.insert(ExpenseEntity(profileId = profileId, amountMinor = amountMinor, category = category, note = note, isIncome = isIncome, budgetMonth = budgetMonth, budgetCycleStartEpochMillis = cycleStart, epochDay = date.toEpochDay(), createdAtEpochMillis = now))
        } catch (_: SQLiteException) { return@withContext AddExpenseResult.Error("Couldn't save transaction — local storage error.") }
        if (isIncome) return@withContext AddExpenseResult.Success(emptyList(), expenseId)
        val alerts = try { evaluateAndNotify(profileId, budgetMonth, category) } catch (_: SQLiteException) { emptyList() }
        AddExpenseResult.Success(alerts, expenseId)
    }

    override suspend fun getExpenseById(id: Long, profileId: Long): Expense? = withContext(ioDispatcher) { expenseDao.getById(id, profileId)?.toDomain() }
    override suspend fun updateExpense(expense: Expense): AddExpenseResult = withContext(ioDispatcher) {
        try { expenseDao.update(expense.toEntity()) } catch (_: SQLiteException) { return@withContext AddExpenseResult.Error("Couldn't update expense — local storage error.") }
        if (expense.isIncome) return@withContext AddExpenseResult.Success(emptyList())
        val budgetMonth = expense.budgetMonth.ifBlank { YearMonth.from(expense.date).toString() }
        val alerts = try { evaluateAndNotify(expense.profileId, budgetMonth, expense.category) } catch (_: SQLiteException) { emptyList() }
        AddExpenseResult.Success(alerts)
    }
    override suspend fun deleteExpense(expense: Expense) = withContext(ioDispatcher) { expenseDao.delete(expense.toEntity()) }
    override suspend fun restoreExpense(expense: Expense): AddExpenseResult = withContext(ioDispatcher) {
        try { expenseDao.insert(expense.toEntity()) } catch (_: SQLiteException) { return@withContext AddExpenseResult.Error("Couldn't restore expense — local storage error.") }
        if (expense.isIncome) return@withContext AddExpenseResult.Success(emptyList())
        val budgetMonth = expense.budgetMonth.ifBlank { YearMonth.from(expense.date).toString() }
        val alerts = try { evaluateAndNotify(expense.profileId, budgetMonth, expense.category) } catch (_: SQLiteException) { emptyList() }
        AddExpenseResult.Success(alerts)
    }

    /** Starts a salary-to-salary cycle at the exact time the income was recorded. */
    override suspend fun startBudgetCycle(profileId: Long, incomeExpenseId: Long): Boolean = withContext(ioDispatcher) {
        val income = expenseDao.getById(incomeExpenseId, profileId) ?: return@withContext false
        if (!income.isIncome) return@withContext false
        val cycleStart = income.createdAtEpochMillis
        val cycleLabel = YearMonth.from(LocalDate.ofEpochDay(income.epochDay)).plusMonths(1).toString()
        val previousCycleLabel = YearMonth.parse(cycleLabel).minusMonths(1).toString()
        val previousActiveCycle = appPreferences.getActiveBudgetMonth(profileId)
        try {
            if (previousActiveCycle != null) {
                val previousBase = appPreferences.getCycleBaseBudget(profileId, previousCycleLabel) ?: 0L
                val previousIncome = expenseDao.getIncomeTotalForBudget(profileId, previousCycleLabel)
                val previousSpent = expenseDao.getOverallTotalForBudget(profileId, previousCycleLabel)
                val carryForward = (previousBase + previousIncome - previousSpent).coerceAtLeast(0L)
                appPreferences.setCarryForward(profileId, cycleLabel, carryForward)
                appPreferences.setCycleBaseBudget(profileId, cycleLabel, 0L)
            } else {
                val initialBase = budgetLimitDao.getByKey(profileId, OVERALL_BUDGET_KEY)?.limitMinor ?: 0L
                appPreferences.setCycleBaseBudget(profileId, cycleLabel, initialBase)
                appPreferences.setCarryForward(profileId, cycleLabel, 0L)
            }
            expenseDao.update(income.copy(budgetMonth = cycleLabel, budgetCycleStartEpochMillis = cycleStart))
            expenseDao.assignExpensesToBudgetFrom(profileId, cycleStart, cycleLabel, cycleStart)
            appPreferences.setActiveBudgetMonth(profileId, cycleLabel)
            appPreferences.setActiveBudgetCycleStart(profileId, cycleStart)
            true
        } catch (_: SQLiteException) { false }
    }

    private suspend fun evaluateAndNotify(profileId: Long, budgetMonth: String, changedCategory: ExpenseCategory): List<LimitAlert> {
        val alerts = mutableListOf<LimitAlert>()
        val periodPrefix = "${profileId}_$budgetMonth"
        budgetLimitDao.getByKey(profileId, changedCategory.name)?.let { limit ->
            maybeAlert(profileId, "${periodPrefix}_${changedCategory.name}", changedCategory, expenseDao.getCategoryTotalForBudget(profileId, changedCategory, budgetMonth), limit.limitMinor)?.let(alerts::add)
        }
        budgetLimitDao.getByKey(profileId, OVERALL_BUDGET_KEY)?.let { limit ->
            maybeAlert(profileId, "${periodPrefix}_$OVERALL_BUDGET_KEY", null, expenseDao.getOverallTotalForBudget(profileId, budgetMonth), limit.limitMinor)?.let(alerts::add)
        }
        return alerts
    }
    private suspend fun maybeAlert(profileId: Long, periodKey: String, category: ExpenseCategory?, spentMinor: Long, limitMinor: Long): LimitAlert? {
        val tier = LimitAlertEvaluator.tierFor(spentMinor, limitMinor)
        if (tier == AlertTier.NONE) return null
        val previousTier = appPreferences.getLastNotifiedTier(periodKey)
        val shouldNotify = tier == AlertTier.CRITICAL || tier.ordinal > previousTier
        if (!shouldNotify) return null
        if (tier.ordinal > previousTier) appPreferences.setLastNotifiedTier(periodKey, tier.ordinal)
        val alert = LimitAlert(profileId, category, tier, spentMinor, limitMinor)
        notificationHelper.notify(alert)
        return alert
    }
}
