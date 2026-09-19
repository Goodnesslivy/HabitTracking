package com.example.habittracking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracking.data.model.GoalFrequency
import com.example.habittracking.data.model.GoalType
import com.example.habittracking.data.model.Habit
import com.example.habittracking.data.model.TrackingMode
import com.example.habittracking.data.repository.AuthRepository // ✅ FIXED: Unified project package tracking import structures cleanly
import com.example.habittracking.data.repository.HabitRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

sealed class HabitCreateState {
    object Idle : HabitCreateState()
    object Saving : HabitCreateState()
    object Saved : HabitCreateState()
    data class Failed(val reason: String) : HabitCreateState()
}

/**
 * Everything the form needs to hold, mirroring the Habit fields that are only
 * meaningful in certain combinations.
 */
data class HabitFormState(
    val name: String = "",
    val color: String = "#4C6EF5",
    val icon: String = "🎯",
    val trackingMode: TrackingMode = TrackingMode.TALLY,
    val goalType: GoalType = GoalType.NONE,
    val goalFrequency: GoalFrequency = GoalFrequency.DAILY,
    val targetCount: String = "",
    val dueDateMillis: Long? = null,
    val hasTimer: Boolean = false,
    val hasJournal: Boolean = false,
    val hasList: Boolean = false
)

@HiltViewModel
class HabitCreateViewModel @Inject constructor(
    private val repository: HabitRepository,
    private val authRepository: AuthRepository // ✅ FIXED: Clean injection hook reference mapped safely
) : ViewModel() {

    private val _form = MutableStateFlow(HabitFormState())
    val form: StateFlow<HabitFormState> = _form

    private val _state = MutableStateFlow<HabitCreateState>(HabitCreateState.Idle)
    val state: StateFlow<HabitCreateState> = _state

    fun updateForm(transform: (HabitFormState) -> HabitFormState) {
        _form.value = transform(_form.value)
    }

    /**
     * When trackingMode flips to TALLY, goalType/goalFrequency/targetCount/dueDate
     * become meaningless — reset them here.
     */
    fun setTrackingMode(mode: TrackingMode) {
        _form.value = _form.value.copy(
            trackingMode = mode,
            goalType = if (mode == TrackingMode.TALLY) GoalType.NONE else _form.value.goalType.takeIf { it != GoalType.NONE } ?: GoalType.COUNT
        )
    }

    fun saveHabit() {
        val f = _form.value
        val userId = authRepository.currentUserId

        if (userId == null) {
            _state.value = HabitCreateState.Failed("You need to be signed in to add a habit")
            return
        }
        if (f.name.isBlank()) {
            _state.value = HabitCreateState.Failed("Give the habit a name")
            return
        }

        var targetCount: Int? = null
        var dueDate: Timestamp? = null

        if (f.trackingMode == TrackingMode.GOAL) {
            when (f.goalType) {
                GoalType.COUNT -> {
                    targetCount = f.targetCount.toIntOrNull()
                    if (targetCount == null || targetCount <= 0) {
                        _state.value = HabitCreateState.Failed("Enter a target count greater than 0")
                        return
                    }
                }
                GoalType.DUE_DATE -> {
                    if (f.dueDateMillis == null) {
                        _state.value = HabitCreateState.Failed("Pick a due date")
                        return
                    }
                    dueDate = Timestamp(Date(f.dueDateMillis))
                }
                GoalType.NONE -> {
                    _state.value = HabitCreateState.Failed("Choose how this goal is measured")
                    return
                }
            }
        }

        val habit = Habit(
            userId = userId,
            name = f.name.trim(),
            color = f.color,
            icon = f.icon,
            trackingMode = f.trackingMode,
            goalType = if (f.trackingMode == TrackingMode.GOAL) f.goalType else GoalType.NONE,
            goalFrequency = if (f.trackingMode == TrackingMode.GOAL && f.goalType == GoalType.COUNT) f.goalFrequency else null,
            targetCount = targetCount,
            dueDate = dueDate,
            hasTimer = f.hasTimer,
            hasJournal = f.hasJournal,
            hasList = f.hasList
        )

        viewModelScope.launch {
            _state.value = HabitCreateState.Saving
            val result = repository.addHabit(habit)
            _state.value = result.fold(
                onSuccess = { HabitCreateState.Saved },
                onFailure = { HabitCreateState.Failed(it.message ?: "Couldn't save — try again") }
            )
        }
    }

    fun resetState() {
        _state.value = HabitCreateState.Idle
    }
}
