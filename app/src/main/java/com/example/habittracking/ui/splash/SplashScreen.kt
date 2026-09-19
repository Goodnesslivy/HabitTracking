package com.example.habittracking.ui.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.habittracking.viewmodel.AuthState
import com.example.habittracking.viewmodel.AuthViewModel

/**
 * Shown for the brief moment it takes to check whether someone's already signed in.
 * This is the app's actual start destination — Login/Dashboard are decided from here,
 * not hardcoded, so a returning user never sees the login screen flash by.
 */
@Composable
fun SplashScreen(
    onSessionFound: () -> Unit, // ✅ FIXED: Signature matches HabitTrackerNavGraph exactly
    onNoSession: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.authState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkExistingSession()
    }

    LaunchedEffect(state) {
        when (state) {
            is AuthState.Success -> onSessionFound() // ✅ FIXED: Trigger parameterless routing callback
            is AuthState.NoSession -> onNoSession()
            else -> Unit // still Loading — stay on the spinner
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
