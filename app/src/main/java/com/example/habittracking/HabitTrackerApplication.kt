package com.example.habittracking

import android.app.Application
import androidx.work.Configuration
import androidx.work.ListenableWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.UUID
import javax.inject.Inject

@HiltAndroidApp
class HabitTrackerApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
