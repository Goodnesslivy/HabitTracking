package com.example.habittracking.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.habittracking.MainActivity
import com.example.habittracking.data.repository.AuthRepository
import com.example.habittracking.data.repository.HabitRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val habitRepository: HabitRepository,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val userId = authRepository.currentUserId ?: return Result.success()

        try {
            val habitsList = habitRepository.getHabitsForUser(userId).first()
            val todayLogs = habitRepository.getTodayLogsForUser(userId).first()

            if (habitsList.isEmpty()) return Result.success()

            val completedHabitIds = todayLogs.filter { it.completed || it.count > 0 }.map { it.habitId }
            val incompleteCount = habitsList.count { it.id !in completedHabitIds }

            if (incompleteCount > 0) {
                sendNotification(incompleteCount)
            }
        } catch (e: Exception) {
            return Result.retry()
        }

        return Result.success()
    }

    private fun sendNotification(incompleteCount: Int) {
        val channelId = "habit_reminders"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Daily Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Reminders for incomplete habits"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // ✅ FIXED: Create an explicit Intent targeting your MainActivity
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // ✅ FIXED: Wrap it in a PendingIntent with the required modern mutability flags
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Keep your streaks alive! 🎯")
            .setContentText("You still have $incompleteCount incomplete habits left for today.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent) // ✅ FIXED: Attach the routing trigger hook here
            .setAutoCancel(true) // ✅ FIXED: Automatically removes the notification after it is clicked
            .build()

        notificationManager.notify(1001, notification)
    }
}
