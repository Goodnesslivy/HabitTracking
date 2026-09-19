package com.example.habittracking.data.repository

import com.example.habittracking.data.model.Habit
import com.example.habittracking.data.model.HabitLog
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val habitsCollection = firestore.collection("habits")
    private val logsCollection = firestore.collection("habitLogs")

    // ---------------------------------------------------------------------
    // Habits
    // ---------------------------------------------------------------------

    /**
     * Real-time stream of a user's habits. Requires a composite index on
     * habits: userId (Ascending) + createdAt (Ascending).
     */
    fun getHabitsForUser(userId: String): Flow<List<Habit>> = callbackFlow {
        val registration = habitsCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val habits = snapshot?.toObjects(Habit::class.java) ?: emptyList()
                trySend(habits)
            }
        awaitClose { registration.remove() }
    }

    suspend fun getHabitById(habitId: String): Habit? {
        val snapshot = habitsCollection.document(habitId).get().await()
        return snapshot.toObject(Habit::class.java)
    }

    suspend fun addHabit(habit: Habit): Result<String> {
        return try {
            val docRef = habitsCollection.document()
            val withId = habit.copy(id = docRef.id, createdAt = Timestamp.now())

            // Explicitly execute the task block safely
            docRef.set(withId).await()
            Result.success(docRef.id)
        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            // ✅ FIXED: Explicitly trap Firestore network rules errors to prevent app crashes
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateHabit(habit: Habit): Result<Unit> {
        return try {
            habitsCollection.document(habit.id).set(habit).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteHabit(habitId: String): Result<Unit> {
        return try {
            // ✅ FIXED: Erase both the habit document and its history batch logs safely
            val logsSnapshot = logsCollection.whereEqualTo("habitId", habitId).get().await()
            val batch = firestore.batch()

            // Queue the main habit document for removal
            batch.delete(habitsCollection.document(habitId))

            // Queue all matching check-in log docs for deletion inside the same batch
            for (document in logsSnapshot.documents) {
                batch.delete(document.reference)
            }

            // Commit all changes atomically to the server
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------------------------------------------------------------------
    // Logs
    // ---------------------------------------------------------------------

    fun getLogsForUserInRange(userId: String, start: Date, end: Date): Flow<List<HabitLog>> = callbackFlow {
        val registration = logsCollection
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("date", Timestamp(start))
            .whereLessThanOrEqualTo("date", Timestamp(end))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(HabitLog::class.java) ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    /**
     * Real-time stream of all of a user's logs for today.
     * FIXED: Cleaned up conversion execution layout.
     */
    fun getTodayLogsForUser(userId: String): Flow<List<HabitLog>> {
        val (start, end) = dayRange(Date())
        return getLogsForUserInRange(userId, start.toDate(), end.toDate())
    }

    /**
     * Real-time stream of every log for one habit, newest first.
     * ✅ FIXED: Added the required userId parameter filter constraint to satisfy secure rule policies.
     */
    fun getLogsForHabit(userId: String, habitId: String): Flow<List<HabitLog>> = callbackFlow {
        val registration = logsCollection
            .whereEqualTo("userId", userId) // 👈 Essential constraint match for resource.data.userId rules
            .whereEqualTo("habitId", habitId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(HabitLog::class.java) ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    suspend fun getLogForHabitOnDate(habitId: String, date: Date): HabitLog? {
        val (start, end) = dayRange(date)
        val snapshot = logsCollection
            .whereEqualTo("habitId", habitId)
            .whereGreaterThanOrEqualTo("date", start)
            .whereLessThanOrEqualTo("date", end)
            .limit(1)
            .get()
            .await()
        return snapshot.toObjects(HabitLog::class.java).firstOrNull()
    }

    suspend fun upsertLog(log: HabitLog): Result<String> {
        return try {
            val docRef = if (log.id.isBlank()) logsCollection.document() else logsCollection.document(log.id)
            val withId = log.copy(id = docRef.id)
            docRef.set(withId).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteLog(logId: String): Result<Unit> {
        return try {
            logsCollection.document(logId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLogsForHabitInRange(habitId: String, start: Date, end: Date): List<HabitLog> {
        val snapshot = logsCollection
            .whereEqualTo("habitId", habitId)
            .whereGreaterThanOrEqualTo("date", Timestamp(start))
            .whereLessThanOrEqualTo("date", Timestamp(end))
            .get()
            .await()
        return snapshot.toObjects(HabitLog::class.java)
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private fun dayRange(date: Date): Pair<Timestamp, Timestamp> {
        val cal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = Timestamp(cal.time)
        cal.add(Calendar.DAY_OF_MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        val end = Timestamp(cal.time)
        return start to end
    }
}
