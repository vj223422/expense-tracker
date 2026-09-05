package com.expensetracker.app.di

import androidx.room.Room
import com.expensetracker.app.data.local.ExpenseDatabase
import com.expensetracker.app.data.notification.NotificationHelper
import com.expensetracker.app.data.prefs.AppPreferences
import com.expensetracker.app.data.repository.BudgetRepository
import com.expensetracker.app.data.repository.BudgetRepositoryImpl
import com.expensetracker.app.data.repository.ExpenseRepository
import com.expensetracker.app.data.repository.ExpenseRepositoryImpl
import com.expensetracker.app.feature.addexpense.AddExpenseViewModel
import com.expensetracker.app.feature.budgets.BudgetsViewModel
import com.expensetracker.app.feature.dashboard.DashboardViewModel
import com.expensetracker.app.feature.settings.SettingsViewModel
import com.expensetracker.app.feature.transactions.TransactionsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(androidContext(), ExpenseDatabase::class.java, ExpenseDatabase.DATABASE_NAME).build()
    }
    single { get<ExpenseDatabase>().expenseDao() }
    single { get<ExpenseDatabase>().budgetLimitDao() }
}

val dataModule = module {
    single { AppPreferences(androidContext()) }
    single { NotificationHelper(androidContext()) }
    single<ExpenseRepository> { ExpenseRepositoryImpl(get(), get(), get(), get()) }
    single<BudgetRepository> { BudgetRepositoryImpl(get()) }
}

val viewModelModule = module {
    viewModel { DashboardViewModel(get(), get()) }
    viewModel { TransactionsViewModel(get()) }
    viewModel { AddExpenseViewModel(get()) }
    viewModel { BudgetsViewModel(get(), get()) }
    viewModel { SettingsViewModel(get()) }
}

val appModules = listOf(databaseModule, dataModule, viewModelModule)
