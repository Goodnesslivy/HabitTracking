package com.example.habittracking.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.habittracking.data.repository.AuthRepository
import com.example.habittracking.data.repository.HabitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain

@OptIn(ExperimentalCoroutinesApi::class)
class LogEntryViewModelTest(
    private val repository: HabitRepository,
    private val authRepository: AuthRepository
) {
    // Custom function to manually run our 3 rubric validation tests
    fun runValidationTests(): String {
        val testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        val savedStateHandle = SavedStateHandle(mapOf("habitId" to "test_habit_id"))

        val results = StringBuilder()

        try {
            // Test 1: Empty String
            val vm1 = LogEntryViewModel(savedStateHandle, repository, authRepository)
            vm1.updateJournalText("   ")
            val check1 = vm1.journalWordCount.value == 0
            results.append("journalWordCount_emptyString: ${if (check1) "PASSED ✅" else "FAILED ❌"}\n")

            // Test 2: Standard Sentence
            val vm2 = LogEntryViewModel(savedStateHandle, repository, authRepository)
            vm2.updateJournalText("Building an outstanding habit tracking application stack")
            val check2 = vm2.journalWordCount.value == 7
            results.append("journalWordCount_standardSentence: ${if (check2) "PASSED ✅" else "FAILED ❌"}\n")

            // Test 3: Exceeding Word Cap
            val vm3 = LogEntryViewModel(savedStateHandle, repository, authRepository)
            val massiveText = List(160) { "word" }.joinToString(" ")
            vm3.updateJournalText(massiveText)
            val check3 = vm3.journalText.value == ""
            results.append("updateJournalText_exceedingWordCap: ${if (check3) "PASSED ✅" else "FAILED ❌"}\n")

        } catch (e: Exception) {
            results.append("An unexpected exception occurred: ${e.localizedMessage}\n")
        } finally {
            Dispatchers.resetMain()
        }

        return results.toString()
    }
}
