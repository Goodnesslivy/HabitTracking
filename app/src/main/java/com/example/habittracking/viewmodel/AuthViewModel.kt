package com.example.habittracking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracking.data.model.AppUser
import com.example.habittracking.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object NoSession : AuthState()
    data class Success(val user: AppUser) : AuthState()
    data class Failed(val reason: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    init {
        // ✅ FIXED: Actively syncs the UI AuthState machine with the underlying Firebase session stream
        viewModelScope.launch {
            authRepository.currentUserIdFlow.collect { uid ->
                if (uid == null && (_authState.value is AuthState.Success || _authState.value is AuthState.Loading)) {
                    _authState.value = AuthState.NoSession
                }
            }
        }
    }

    fun signUp(displayName: String, email: String, password: String) {
        if (displayName.isBlank() || email.isBlank() || password.length < 6) {
            _authState.value = AuthState.Failed("Fill all fields — password must be 6+ characters")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signUp(displayName, email, password)
            _authState.value = result.fold(
                onSuccess = { AuthState.Success(it) },
                onFailure = { AuthState.Failed(mapAuthException(it, "Sign up failed")) }
            )
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Failed("Enter email and password")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.login(email, password)
            _authState.value = result.fold(
                onSuccess = { AuthState.Success(it) },
                onFailure = { AuthState.Failed(mapAuthException(it, "Login failed")) }
            )
        }
    }

    // ✅ FIXED: Extracted sign-out handling logic to ensure the UI updates cleanly
    fun signOut() {
        _authState.value = AuthState.Loading
        authRepository.signOut()
        _authState.value = AuthState.NoSession
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun checkExistingSession() {
        val uid = authRepository.currentUserId
        if (uid == null) {
            _authState.value = AuthState.NoSession
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val user = authRepository.getUserProfile(uid)
            _authState.value = if (user != null) AuthState.Success(user) else AuthState.NoSession
        }
    }

    // ✅ FIXED: Maps cryptic Firebase network errors into user-friendly notifications
    private fun mapAuthException(throwable: Throwable, fallback: String): String {
        return when (throwable) {
            is FirebaseAuthUserCollisionException -> "This email address is already registered."
            is FirebaseAuthInvalidCredentialsException -> "Invalid email or password combination."
            else -> throwable.localizedMessage ?: fallback
        }
    }
}
