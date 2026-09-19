package com.example.habittracking.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.habittracking.viewmodel.AuthState
import com.example.habittracking.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit, // ✅ FIXED: Parameter removed to perfectly match HabitTrackerNavGraph signatures
    onNavigateToSignUp: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val state by viewModel.authState.collectAsState()

    LaunchedEffect(state) {
        if (state is AuthState.Success) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome Back", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )

        Spacer(Modifier.height(20.dp))

        if (state is AuthState.Failed) {
            Text((state as AuthState.Failed).reason, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = {
                // ✅ FIXED: Defensive trimming to block auto-complete spacing bugs
                if (email.isNotBlank() && password.isNotBlank()) {
                    viewModel.login(email.trim(), password.trim())
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = state !is AuthState.Loading && email.isNotBlank() && password.isNotBlank()
        ) {
            if (state is AuthState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Log In")
            }
        }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onNavigateToSignUp) {
            Text("Don't have an account? Sign up")
        }
    }
}
