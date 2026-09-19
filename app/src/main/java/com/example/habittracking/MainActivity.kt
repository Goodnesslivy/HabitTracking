package com.example.habittracking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.habittracking.data.service.DailyReminderWorker
import com.example.habittracking.navigation.HabitTrackerNavGraph
import com.example.habittracking.ui.theme.HabitTrackingTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ FIXED: Enqueue the periodic daily reminder worker safely on app launch
        scheduleDailyReminder()

        setContent {
            HabitTrackingTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HabitTrackerNavGraph()
                }
            }
        }
    }

    private fun scheduleDailyReminder() {
        val reminderRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            24, TimeUnit.HOURS, // Runs every 24 hours
            1, TimeUnit.HOURS   // Flex interval window margin
        ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "daily_habit_reminder",
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing schedules intact across app opens
            reminderRequest
        )
    }
}
