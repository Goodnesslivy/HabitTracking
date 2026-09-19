package com.example.habittracking.data.repository

import com.example.habittracking.data.model.AppUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val usersCollection = firestore.collection("users")

    /**
     * ✅ FIXED: Exposes a reactive stream of the current user ID.
     * Updates automatically whenever the user signs in or out, pushing the update
     * directly down to your dynamic ViewModel pipelines.
     */
    val currentUserIdFlow: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    val currentUserId: String?
        get() = auth.currentUser?.uid

    suspend fun signUp(displayName: String, email: String, password: String): Result<AppUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: throw IllegalStateException("No UID returned")

            val user = AppUser(userId = uid, email = email, displayName = displayName)
            usersCollection.document(uid).set(user).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<AppUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: throw IllegalStateException("No UID returned")
            val user = getUserProfile(uid) ?: throw IllegalStateException("User profile not found")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(uid: String): AppUser? {
        val snapshot = usersCollection.document(uid).get().await()
        return snapshot.toObject(AppUser::class.java)?.copy(userId = uid)
    }

    fun signOut() = auth.signOut()
}
