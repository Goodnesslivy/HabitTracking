package com.example.habittracking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracking.data.model.GoalFrequency
import com.example.habittracking.data.model.GoalType
import com.example.habittracking.data.model.Habit
import com.example.habittracking.data.model.HabitLog
import com.example.habittracking.data.model.TrackingMode
import com.example.habittracking.data.repository.AuthRepository
import com.example.habittracking.data.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

sealed class DashboardCard {
    abstract val habit: Habit

    data class TallyCard(
        override val habit: Habit,
        val todayCount: Int
    ) : DashboardCard()

    data class GoalProgressCard(
        override val habit: Habit,
        val currentCount: Int,
        val targetCount: Int,
        val frequency: GoalFrequency
    ) : DashboardCard() {
        val progress: Float get() = if (targetCount <= 0) 0f else (currentCount.toFloat() / targetCount).coerceIn(0f, 1f)
    }

    data class DueDateCard(
        override val habit: Habit,
        val totalLogged: Int,
        val daysRemaining: Long?
    ) : DashboardCard()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
     val repository: HabitRepository,
     val authRepository: AuthRepository
) : ViewModel() {

    private val currentUserIdFlow = authRepository.currentUserIdFlow

    val habits: StateFlow<List<Habit>> = currentUserIdFlow
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) flowOf(emptyList()) else repository.getHabitsForUser(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayLogsByHabitId: StateFlow<Map<String, HabitLog>> = currentUserIdFlow
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) flowOf(emptyList()) else repository.getTodayLogsForUser(uid)
        }
        .map { logs -> logs.associateBy { it.habitId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // ✅ FIXED: Eliminated the init block and volatile _periodSums state variables entirely.
    // This pipeline handles the calculations asynchronously and securely in the background.

    val dashboardCards: StateFlow<List<DashboardCard>> =
        combine(habits, todayLogsByHabitId, currentUserIdFlow) { habitList, todayLogs, uid ->
            Triple(habitList, todayLogs, uid)
        }.flatMapLatest { (habitList, todayLogs, uid) ->
            if (habitList.isEmpty() || uid.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                // ✅ FIXED: Pass uid down to secure the query listeners
                calculateDashboardCardsFlow(habitList, todayLogs, uid)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    private fun calculateDashboardCardsFlow(
        habitList: List<Habit>,
        todayLogs: Map<String, HabitLog>,
        userId: String // 👈 Receive userId from the pipeline flow
    ) = combine(
        *habitList.map { habit ->
            // ✅ FIXED: Pass the verified userId down into the repository filter stream
            repository.getLogsForHabit(userId, habit.id).map { periodLogs ->
                calculateSingleProgress(habit, periodLogs)
            }
        }.toTypedArray()
    ) { progressArray ->
        val progressMap = progressArray.toMap()
        habitList.map { habit ->
            val todayLog = todayLogs[habit.id]
            val calculatedSum = progressMap[habit.id] ?: 0

            buildCard(habit, todayLog, calculatedSum)
        }
    }


    private fun calculateSingleProgress(habit: Habit, logs: List<HabitLog>): Pair<String, Int> {
        if (habit.trackingMode != TrackingMode.GOAL) return habit.id to 0

        return when (habit.goalType) {
            GoalType.COUNT -> {
                val (start, _) = periodRange(habit.goalFrequency ?: GoalFrequency.DAILY)
                // Filter the live stream elements using pure memory lookups to prevent thread delays
                val periodSum = logs.filter { it.date.toDate().after(start) }.sumOf { it.count }
                habit.id to periodSum
            }
            GoalType.DUE_DATE -> {
                habit.id to logs.sumOf { it.count }
            }
            GoalType.NONE -> habit.id to 0
        }
    }

    private fun buildCard(habit: Habit, todayLog: HabitLog?, periodSum: Int): DashboardCard {
        return when {
            habit.trackingMode == TrackingMode.TALLY -> DashboardCard.TallyCard(
                habit = habit,
                todayCount = todayLog?.count ?: 0
            )
            habit.goalType == GoalType.COUNT -> DashboardCard.GoalProgressCard(
                habit = habit,
                currentCount = periodSum,
                targetCount = habit.targetCount ?: 0,
                frequency = habit.goalFrequency ?: GoalFrequency.DAILY
            )
            habit.goalType == GoalType.DUE_DATE -> DashboardCard.DueDateCard(
                habit = habit,
                totalLogged = periodSum,
                daysRemaining = habit.dueDate?.toDate()?.let { daysBetween(Date(), it) } ?: 0L
            )
            else -> DashboardCard.TallyCard(habit, todayLog?.count ?: 0)
        }
    }

    private fun periodRange(frequency: GoalFrequency): Pair<Date, Date> {
        val cal = Calendar.getInstance()
        when (frequency) {
            GoalFrequency.DAILY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            }
            GoalFrequency.WEEKLY -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            }
            GoalFrequency.MONTHLY -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            }
        }
        return cal.time to Date()
    }

    private fun daysBetween(from: Date, to: Date): Long {
        val diffMillis = to.time - from.time
        return diffMillis / (1000 * 60 * 60 * 24)
    }

    // Dynamic start date boundary helper for the past week
    private val sevenDaysAgoDate: Date
        get() = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -6) // Include today + past 6 days
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.time

    val weeklyLogs: StateFlow<List<HabitLog>> = currentUserIdFlow
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                // Queries all log documents up to right now
                repository.getLogsForUserInRange(uid, sevenDaysAgoDate, Date())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    val weeklyChartData: StateFlow<List<com.example.habittracking.data.model.ChartDayData>> =
        combine(habits, weeklyLogs) { habitList, logsList ->
            calculateRollingWeeklyAnalytics(habitList, logsList)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun calculateRollingWeeklyAnalytics(
        habitList: List<Habit>,
        logsList: List<HabitLog>
    ): List<com.example.habittracking.data.model.ChartDayData> {
        val cal = Calendar.getInstance()
        val dayFormat = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault()) // "Mon", "Tue", etc.

        // 1. Pre-generate the past 7 days chronologically to seed our map structure cleanly
        val rollingDaysList = (0..6).map { offset ->
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -6 + offset)
            }.time
        }

        // 2. Group logs dynamically by their formatted day label (e.g., "Fri" -> [logs])
        val logsByDayLabel = logsList.groupBy { log ->
            dayFormat.format(log.date.toDate())
        }

        // 3. Count expected entries based on what tracking configurations are active on those days
        return rollingDaysList.map { date ->
            val label = dayFormat.format(date)
            val logsForDay = logsByDayLabel[label] ?: emptyList()

            // Count how many individual habit nodes are logged as completed on this calendar date
            val completedCount = logsForDay.count { it.completed || it.count > 0 }

            // Total expected items matches your active habits length (fallback to completed length if larger)
            val totalExpectedCount = maxOf(habitList.size, completedCount)

            com.example.habittracking.data.model.ChartDayData(
                dayLabel = label,
                completedCount = completedCount,
                totalCount = totalExpectedCount
            )
        }
    }
    // 📁 Inside DashboardViewModel.kt

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            repository.deleteHabit(habitId).fold(
                onSuccess = { /* The live snapshot flow streams will automatically update the UI */ },
                onFailure = { error -> println("⚠️ Failed to delete habit: ${error.localizedMessage}") }
            )
        }
    }


}
