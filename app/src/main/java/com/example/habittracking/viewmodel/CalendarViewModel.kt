package com.example.habittracking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracking.data.model.Habit
import com.example.habittracking.data.repository.AuthRepository
import com.example.habittracking.data.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

data class HabitDot(val habitId: String, val habitName: String, val color: String, val icon: String)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: HabitRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val currentUserIdFlow = authRepository.currentUserIdFlow

    private val _visibleMonth = MutableStateFlow(YearMonth.now())
    val visibleMonth: StateFlow<YearMonth> = _visibleMonth

    val habits: StateFlow<List<Habit>> = currentUserIdFlow
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) flowOf(emptyList()) else repository.getHabitsForUser(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Map of each day in the visible month to the habits with activity that day. */
    val dotsByDate: StateFlow<Map<LocalDate, List<HabitDot>>> =
        combine(
            _visibleMonth,
            currentUserIdFlow
        ) { month, uid ->
            month to uid
        }.flatMapLatest { (month, uid) ->
            if (uid.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                val (start, end) = monthRange(month)
                repository.getLogsForUserInRange(uid, start, end)
            }
        }.combine(habits) { logs, habitList ->
            val habitsById = habitList.associateBy { it.id }
            logs
                .filter { it.completed || it.count > 0 }
                .groupBy { log ->
                    log.date.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                }
                .mapValues { (_, logsForDay) ->
                    logsForDay.mapNotNull { log ->
                        habitsById[log.habitId]?.let { h ->
                            HabitDot(habitId = h.id, habitName = h.name, color = h.color, icon = h.icon)
                        }
                    }
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setVisibleMonth(month: YearMonth) {
        _visibleMonth.value = month
    }

    private fun monthRange(month: YearMonth): Pair<Date, Date> {
        val start = month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        // ✅ FIXED: Configured the maximum nanosecond timestamp limit to capture late-night logged habits safely
        val end = month.atEndOfMonth().atTime(23, 59, 59, 999999999).atZone(ZoneId.systemDefault()).toInstant()
        return Date.from(start) to Date.from(end)
    }
}
