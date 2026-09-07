package com.expensetracker.app

import android.app.Application
import com.expensetracker.app.data.repository.ProfileRepository
import com.expensetracker.app.di.appModules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin

class App : Application(), KoinComponent {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(appModules)
        }

        // Guarantees at least one profile exists before any screen queries "the active profile" —
        // see ProfileRepository.ensureDefaultProfile. A fresh install has none yet.
        val profileRepository: ProfileRepository = get()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            profileRepository.ensureDefaultProfile()
        }
    }
}
