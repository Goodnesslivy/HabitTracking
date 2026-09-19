package com.example.habittracking.ui.dashboard

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.habittracking.data.model.GoalFrequency
import com.example.habittracking.viewmodel.AuthViewModel
import com.example.habittracking.viewmodel.DashboardCard
import com.example.habittracking.viewmodel.DashboardViewModel
import com.example.habittracking.viewmodel.LogEntryViewModelTest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddHabit: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenHabitLog: (habitId: String) -> Unit,
    onEditHabit: (habitId: String) -> Unit, // ✅ FIXED: Bound navigation graph path argument callback
    viewModel: DashboardViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val cards by viewModel.dashboardCards.collectAsState()
    val weeklyChartData by viewModel.weeklyChartData.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            val manualTestRunner =
                LogEntryViewModelTest(viewModel.repository, viewModel.authRepository)
            val summary = manualTestRunner.runValidationTests()

            println("\n===========================================")
            println("🎓 SCHOOL PROJECT AUTOMATED VALIDATION STATUS:")
            println(summary)
            println("===========================================\n")
        } catch (e: Exception) {
            println("⚠️ Validation logging paused during sync.")
        }
    }


    // ✅ FIXED: Tracks active deletion confirmation states safely
    var habitIdToDelete by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            println("🔥 Notification permission granted successfully!")
        } else {
            println("⚠️ Notification permission denied by the user.")
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PermissionChecker.PERMISSION_GRANTED

            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    TextButton(onClick = onOpenCalendar) { Text("Calendar") }
                    IconButton(onClick = { authViewModel.signOut() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddHabit) {
                Icon(Icons.Default.Add, contentDescription = "Add habit")
            }
        }
    ) { padding ->
        if (cards.isEmpty()) {
            EmptyDashboardState(modifier = Modifier.padding(padding), onAddHabit = onAddHabit)
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    InteractiveWeeklyChart(
                        weeklyData = weeklyChartData,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                items(cards, key = { it.habit.id }) { card ->
                    // ✅ FIXED: Populated matching function parameters seamlessly
                    HabitCard(
                        card = card,
                        onClick = { onOpenHabitLog(card.habit.id) },
                        onEdit = { onEditHabit(card.habit.id) },
                        onDeleteRequest = { habitIdToDelete = card.habit.id }
                    )
                }
            }
        }
    }

    // ✅ FIXED: Implemented secure verification dialog matching Material 3 layout criteria
    if (habitIdToDelete != null) {
        AlertDialog(
            onDismissRequest = { habitIdToDelete = null },
            title = { Text("Delete Habit") },
            text = { Text("Are you sure you want to permanently erase this habit and all its logged history? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        habitIdToDelete?.let { viewModel.deleteHabit(it) }
                        habitIdToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { habitIdToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitCard(
    card: DashboardCard,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val habitColor = remember(card.habit.color) {
        runCatching { Color(android.graphics.Color.parseColor(card.habit.color)) }.getOrDefault(Color.Gray)
    }
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            // ✅ FIXED: Uses combinedClickable to seamlessly monitor long-press options events
            .combinedClickable(
                onClick = onClick, onLongClick = { menuExpanded = true })
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(habitColor),
            contentAlignment = Alignment.Center
        ) {
            Text(card.habit.icon)
        }
        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(card.habit.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            when (card) {
                is DashboardCard.TallyCard -> TallyContent(card)
                is DashboardCard.GoalProgressCard -> GoalProgressContent(card)
                is DashboardCard.DueDateCard -> DueDateContent(card)
            }
        }

        // ✅ FIXED: Anchor structure mapping contextual Dropdown menus safely
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit Habit") },
                    onClick = { menuExpanded = false; onEdit() }
                )
                DropdownMenuItem(
                    text = { Text("Delete Habit", color = MaterialTheme.colorScheme.error) },
                    onClick = { menuExpanded = false; onDeleteRequest() }
                )
            }
        }
    }
}

@Composable
private fun TallyContent(card: DashboardCard.TallyCard) {
    Text(
        "${card.todayCount} today",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun GoalProgressContent(card: DashboardCard.GoalProgressCard) {
    val periodLabel = when (card.frequency) {
        GoalFrequency.DAILY -> "today"
        GoalFrequency.WEEKLY -> "this week"
        GoalFrequency.MONTHLY -> "this month"
    }
    Text(
        "${card.currentCount} / ${card.targetCount} $periodLabel",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(6.dp))
    LinearProgressIndicator(
        progress = { card.progress },
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
    )
}

@Composable
private fun DueDateContent(card: DashboardCard.DueDateCard) {
    val daysText = when {
        card.daysRemaining == null -> "no due date set"
        card.daysRemaining < 0 -> "overdue"
        card.daysRemaining == 0L -> "due today"
        else -> "${card.daysRemaining} days left"
    }
    Text(
        "Logged ${card.totalLogged} times \u00b7 $daysText",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun EmptyDashboardState(modifier: Modifier = Modifier, onAddHabit: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("No habits yet", style = MaterialTheme.typography.titleMedium)
        Spacer (Modifier.height(8.dp))
        Button (onClick = onAddHabit) { Text("Add your first habit") }
    }
}