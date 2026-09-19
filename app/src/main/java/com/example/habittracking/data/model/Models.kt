package com.example.habittracking.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * users/{userId} in Firestore.
 */
data class AppUser(
    @DocumentId val userId: String = "", // ✅ FIXED: Added @DocumentId for automatic binding on fetch
    val email: String = "",
    val displayName: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

// ✅ FIXED: Explicitly document Enum structures to prevent Reflection mapping mismatches
enum class TrackingMode { TALLY, GOAL }
enum class GoalType { NONE, COUNT, DUE_DATE }
enum class GoalFrequency { DAILY, WEEKLY, MONTHLY }

/**
 * habits/{habitId} in Firestore.
 */
data class Habit(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val color: String = "#000000",
    val icon: String = "",
    val trackingMode: TrackingMode = TrackingMode.TALLY,
    val goalType: GoalType = GoalType.NONE,
    val goalFrequency: GoalFrequency? = null,
    val targetCount: Int? = null,
    val dueDate: Timestamp? = null,
    val hasTimer: Boolean = false,
    val hasJournal: Boolean = false,
    val hasList: Boolean = false,
    val createdAt: Timestamp = Timestamp.now()
)

/**
 * One freeform checklist item within a day's log.
 */
data class ChecklistItem(
    val text: String = "",
    val checked: Boolean = false
)

/**
 * habitLogs/{logId} in Firestore.
 */
data class HabitLog(
    @DocumentId val id: String = "",
    val habitId: String = "",
    val userId: String = "",
    val date: Timestamp = Timestamp.now(),
    val completed: Boolean = false,
    val count: Int = 0,
    val timerSeconds: Int? = null,
    val journalText: String? = null,
    val listItems: List<ChecklistItem>? = null
)
// Represents completion analytics for a specific day of the week
data class ChartDayData(
    val dayLabel: String,       // e.g., "Mon", "Tue"
    val completedCount: Int,
    val totalCount: Int
) {
    // Calculates the completion percentage fraction safely
    val percentage: Float get() = if (totalCount <= 0) 0f else completedCount.toFloat() / totalCount
}
