package com.example.habittracking.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracking.data.model.GoalFrequency
import com.example.habittracking.data.model.GoalType
import com.example.habittracking.data.model.Habit
import com.example.habittracking.data.model.TrackingMode
import com.example.habittracking.data.repository.AuthRepository
import com.example.habittracking.data.repository.HabitRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

sealed class HabitEditState {
    object Idle : HabitEditState()
    object Loading : HabitEditState()
    object Saving : HabitEditState()
    object Saved : HabitEditState()
    data class Failed(val reason: String) : HabitEditState()
}

@HiltViewModel
class HabitEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: HabitRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val habitId: String = checkNotNull(savedStateHandle["habitId"])
    private var originalCreatedAt: Timestamp? = null

    // ✅ FIXED: Tied into HabitFormState seamlessly (Will compile without issues if HabitFormState lives in the same package)
    private val _form = MutableStateFlow(HabitFormState())
    val form: StateFlow<HabitFormState> = _form

    private val _state = MutableStateFlow<HabitEditState>(HabitEditState.Idle)
    val state: StateFlow<HabitEditState> = _state

    init {
        loadHabit()
    }

    private fun loadHabit() {
        viewModelScope.launch {
            _state.value = HabitEditState.Loading
            val habit = repository.getHabitById(habitId)
            if (habit != null) {
                originalCreatedAt = habit.createdAt
                _form.value = HabitFormState(
                    name = habit.name,
                    color = habit.color,
                    icon = habit.icon,
                    trackingMode = habit.trackingMode,
                    goalType = habit.goalType,
                    goalFrequency = habit.goalFrequency ?: GoalFrequency.DAILY,
                    targetCount = habit.targetCount?.toString() ?: "",
                    dueDateMillis = habit.dueDate?.toDate()?.time,
                    hasTimer = habit.hasTimer,
                    hasJournal = habit.hasJournal,
                    hasList = habit.hasList
                )
                _state.value = HabitEditState.Idle
            } else {
                _state.value = HabitEditState.Failed("Habit not found")
            }
        }
    }

    fun updateForm(transform: (HabitFormState) -> HabitFormState) {
        _form.value = transform(_form.value)
    }

    fun setTrackingMode(mode: TrackingMode) {
        _form.value = _form.value.copy(
            trackingMode = mode,
            goalType = if (mode == TrackingMode.TALLY) GoalType.NONE else _form.value.goalType.takeIf { it != GoalType.NONE } ?: GoalType.COUNT
        )
    }

    fun updateHabit() {
        val f = _form.value
        val userId = authRepository.currentUserId

        if (userId == null) {
            _state.value = HabitEditState.Failed("You need to be signed in to modify a habit")
            return
        }
        if (f.name.isBlank()) {
            _state.value = HabitEditState.Failed("Give the habit a name")
            return
        }

        var targetCount: Int? = null
        var dueDate: Timestamp? = null

        if (f.trackingMode == TrackingMode.GOAL) {
            when (f.goalType) {
                GoalType.COUNT -> {
                    targetCount = f.targetCount.toIntOrNull()
                    if (targetCount == null || targetCount <= 0) {
                        _state.value = HabitEditState.Failed("Enter a target count greater than 0")
                        return
                    }
                }
                GoalType.DUE_DATE -> {
                    if (f.dueDateMillis == null) {
                        _state.value = HabitEditState.Failed("Pick a due date")
                        return
                    }
                    dueDate = Timestamp(Date(f.dueDateMillis))
                }
                GoalType.NONE -> {
                    _state.value = HabitEditState.Failed("Choose how this goal is measured")
                    return
                }
            }
        }


        val updatedHabit = Habit(
            id = habitId,
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
            hasList = f.hasList,
            // ✅ FIXED: Added a defensive fallback timestamp to satisfy the compiler constraints
            createdAt = originalCreatedAt ?: Timestamp.now()
        )


        viewModelScope.launch {
            _state.value = HabitEditState.Saving
            val result = repository.updateHabit(updatedHabit)
            _state.value = result.fold(
                onSuccess = { HabitEditState.Saved },
                onFailure = { HabitEditState.Failed(it.message ?: "Couldn't save changes — try again") }
            )
        }
    }
}
