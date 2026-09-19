package com.example.habittracking.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.habittracking.ui.auth.LoginScreen
import com.example.habittracking.ui.auth.SignUpScreen
import com.example.habittracking.ui.calendar.CalendarScreen
import com.example.habittracking.ui.dashboard.DashboardScreen
import com.example.habittracking.ui.habitcreate.HabitCreateScreen
import com.example.habittracking.ui.habitcreate.HabitEditScreen // ✅ FIXED: Included missing import target reference mapping
import com.example.habittracking.ui.logentry.LogEntryScreen
import com.example.habittracking.ui.splash.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val DASHBOARD = "dashboard"
    const val HABIT_CREATE = "habit_create"
    const val LOG_ENTRY = "log_entry/{habitId}"
    const val CALENDAR = "calendar"

    // ✅ FIXED: Standardised explicit routing keys to bypass manual formatting bugs
    const val HABIT_EDIT = "habit_edit/{habitId}"

    fun logEntry(habitId: String) = "log_entry/$habitId"
    fun habitEdit(habitId: String) = "habit_edit/$habitId"
}

@Composable
fun HabitTrackerNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            SplashScreen(
                onSessionFound = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNoSession = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToSignUp = { navController.navigate(Routes.SIGNUP) }
            )
        }

        composable(Routes.SIGNUP) {
            SignUpScreen(
                onSignUpSuccess = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.navigate(Routes.LOGIN) }
            )
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onAddHabit = { navController.navigate(Routes.HABIT_CREATE) },
                onOpenCalendar = { navController.navigate(Routes.CALENDAR) },
                onOpenHabitLog = { habitId -> navController.navigate(Routes.logEntry(habitId)) },
                // ✅ FIXED: Resolved missing argument hook exception cleanly
                onEditHabit = { habitId -> navController.navigate(Routes.habitEdit(habitId)) }
            )
        }

        composable(Routes.HABIT_CREATE) {
            HabitCreateScreen(
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        // ✅ FIXED: Integrated the editing screen component into your navigation graph framework safely
        composable(
            route = Routes.HABIT_EDIT,
            arguments = listOf(navArgument("habitId") { type = NavType.StringType })
        ) {
            HabitEditScreen(
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.LOG_ENTRY,
            arguments = listOf(navArgument("habitId") { type = NavType.StringType })
        ) {
            LogEntryScreen(
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CALENDAR) {
            CalendarScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
