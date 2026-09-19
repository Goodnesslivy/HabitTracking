package com.example.habittracking.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracking.data.model.ChecklistItem
import com.example.habittracking.data.model.Habit
import com.example.habittracking.data.model.HabitLog
import com.example.habittracking.data.repository.AuthRepository
import com.example.habittracking.data.repository.HabitRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

sealed class LogSaveState {
    object Idle : LogSaveState()
    object Saving : LogSaveState()
    object Saved : LogSaveState()
    data class Failed(val reason: String) : LogSaveState()
}

private const val JOURNAL_WORD_LIMIT = 150

@HiltViewModel
class LogEntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: HabitRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val habitId: String = checkNotNull(savedStateHandle["habitId"])
    private var existingLogId: String = ""

    private val _habit = MutableStateFlow<Habit?>(null)
    val habit: StateFlow<Habit?> = _habit

    private val _completed = MutableStateFlow(false)
    val completed: StateFlow<Boolean> = _completed

    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count

    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds

    private val _timerRunning = MutableStateFlow(false)
    val timerRunning: StateFlow<Boolean> = _timerRunning
    private var timerJob: Job? = null

    private val _journalText = MutableStateFlow("")
    val journalText: StateFlow<String> = _journalText

    val journalWordCount: StateFlow<Int> = _journalText
        .map { wordCount(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _listItems = MutableStateFlow<List<ChecklistItem>>(emptyList())
    val listItems: StateFlow<List<ChecklistItem>> = _listItems

    private val _suggestedItems = MutableStateFlow<List<ChecklistItem>>(emptyList())
    val suggestedItems: StateFlow<List<ChecklistItem>> = _suggestedItems

    private val _saveState = MutableStateFlow<LogSaveState>(LogSaveState.Idle)
    val saveState: StateFlow<LogSaveState> = _saveState

    init {
        loadExistingLog()
    }

    private fun loadExistingLog() {
        viewModelScope.launch {
            // ✅ FIXED: Wrapped the initialization sequence in a try-catch block
            // to trap any missing indexes or security restrictions safely.
            try {
                val h = repository.getHabitById(habitId)
                _habit.value = h

                val todayLog = repository.getLogForHabitOnDate(habitId, Date())
                if (todayLog != null) {
                    existingLogId = todayLog.id
                    _completed.value = todayLog.completed
                    _count.value = todayLog.count
                    _timerSeconds.value = todayLog.timerSeconds ?: 0
                    _journalText.value = todayLog.journalText ?: ""
                    _listItems.value = todayLog.listItems ?: emptyList()
                }

                if (h?.hasList == true && _listItems.value.isEmpty()) {
                    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }.time
                    val yesterdayLog = repository.getLogForHabitOnDate(habitId, yesterday)
                    _suggestedItems.value = yesterdayLog?.listItems?.map { it.copy(checked = false) } ?: emptyList()
                }
            } catch (e: Exception) {
                // Gracefully fallback to an idle error state instead of killing the app process
                _saveState.value = LogSaveState.Failed("Failed to sync today's log metadata from server.")
            }
        }
    }

    // --- Check-in (completed / count) ---

    fun checkIn() {
        _count.value += 1
        _completed.value = true
    }

    fun undoCheckIn() {
        if (_count.value > 0) _count.value -= 1
        if (_count.value == 0) _completed.value = false
    }

    // --- Timer ---

    fun toggleTimer() {
        _timerRunning.value = !_timerRunning.value
        if (_timerRunning.value) {
            timerJob = viewModelScope.launch {
                while (_timerRunning.value) {
                    delay(1000)
                    _timerSeconds.value += 1
                }
            }
        } else {
            timerJob?.cancel()
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        _timerRunning.value = false
        _timerSeconds.value = 0
    }

    // --- Journal ---

    fun updateJournalText(text: String) {
        val currentWords = wordCount(text)
        if (currentWords <= JOURNAL_WORD_LIMIT || text.length < _journalText.value.length) {
            _journalText.value = text
        }
    }

    private fun wordCount(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    }

    // --- Checklist ---

    fun addChecklistItem(text: String) {
        if (text.isBlank()) return
        _listItems.value = _listItems.value + ChecklistItem(text = text.trim(), checked = false)
    }

    fun toggleChecklistItem(index: Int) {
        _listItems.value = _listItems.value.mapIndexed { i, item ->
            if (i == index) item.copy(checked = !item.checked) else item
        }
    }

    fun removeChecklistItem(index: Int) {
        _listItems.value = _listItems.value.filterIndexed { i, _ -> i != index }
    }

    fun applySuggestedList() {
        _listItems.value = _suggestedItems.value
        _suggestedItems.value = emptyList()
    }

    fun dismissSuggestedList() {
        _suggestedItems.value = emptyList()
    }

    // --- Save ---

    fun saveLog(onDone: () -> Unit = {}) {
        val h = _habit.value
        val userId = authRepository.currentUserId

        if (h == null || userId == null) {
            _saveState.value = LogSaveState.Failed("Couldn't load habit — try again")
            return
        }

        if (wordCount(_journalText.value) > JOURNAL_WORD_LIMIT) {
            _saveState.value = LogSaveState.Failed("Journal exceeds the $JOURNAL_WORD_LIMIT word cap limit.")
            return
        }

        val log = HabitLog(
            id = existingLogId,
            habitId = habitId,
            userId = userId,
            date = Timestamp.now(),
            completed = _completed.value,
            count = _count.value,
            timerSeconds = if (h.hasTimer) _timerSeconds.value else null,
            journalText = if (h.hasJournal) _journalText.value else null,
            listItems = if (h.hasList) _listItems.value else null
        )

        viewModelScope.launch {
            _saveState.value = LogSaveState.Saving
            val result = repository.upsertLog(log)
            _saveState.value = result.fold(
                onSuccess = { LogSaveState.Saved.also { onDone() } },
                onFailure = { LogSaveState.Failed(it.message ?: "Couldn't save — try again") }
            )
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
