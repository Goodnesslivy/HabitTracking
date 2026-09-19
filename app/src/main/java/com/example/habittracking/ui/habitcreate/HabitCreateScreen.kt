package com.example.habittracking.ui.habitcreate

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.habittracking.data.model.GoalFrequency
import com.example.habittracking.data.model.GoalType
import com.example.habittracking.data.model.TrackingMode
import com.example.habittracking.viewmodel.HabitCreateState
import com.example.habittracking.viewmodel.HabitCreateViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val COLOR_OPTIONS = listOf(
    "#4C6EF5", "#F06595", "#FA5252", "#FD7E14", "#F59F00",
    "#40C057", "#12B886", "#22B8CF", "#7950F2", "#495057"
)

private val ICON_OPTIONS = listOf(
    "🎯", "📖", "🏃", "🧘", "💧", "🥗", "😴", "✍️",
    "🎨", "🎸", "🧹", "🌱", "📵", "💊", "🚭", "💪"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitCreateScreen(
    onDone: () -> Unit,
    onCancel: () -> Unit,
    viewModel: HabitCreateViewModel = hiltViewModel()
) {
    val form by viewModel.form.collectAsState()
    val state by viewModel.state.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state is HabitCreateState.Saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Habit") },
                navigationIcon = { TextButton(onClick = onCancel) { Text("Cancel") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            OutlinedTextField(
                value = form.name,
                onValueChange = { name -> viewModel.updateForm { it.copy(name = name) } },
                label = { Text("Habit name") },
                placeholder = { Text("e.g. Drink water") },
                modifier = Modifier.fillMaxWidth()
            )

            SectionLabel("Color")
            ColorRow(selected = form.color, onSelect = { c -> viewModel.updateForm { it.copy(color = c) } })

            SectionLabel("Icon")
            IconGrid(selected = form.icon, onSelect = { i -> viewModel.updateForm { it.copy(icon = i) } })

            SectionLabel("Tracking mode")
            TrackingModeRow(
                selected = form.trackingMode,
                onSelect = { mode -> viewModel.setTrackingMode(mode) }
            )

            if (form.trackingMode == TrackingMode.GOAL) {
                SectionLabel("Goal type")
                GoalTypeRow(
                    selected = form.goalType,
                    onSelect = { type -> viewModel.updateForm { it.copy(goalType = type) } }
                )

                when (form.goalType) {
                    GoalType.COUNT -> {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = form.targetCount,
                            onValueChange = { count -> viewModel.updateForm { it.copy(targetCount = count) } },
                            label = { Text("Target count") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        FrequencyRow(
                            selected = form.goalFrequency,
                            onSelect = { freq -> viewModel.updateForm { it.copy(goalFrequency = freq) } }
                        )
                    }
                    GoalType.DUE_DATE -> {
                        Spacer(Modifier.height(12.dp))
                        val label = form.dueDateMillis?.let {
                            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it))
                        } ?: "Pick a due date"
                        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(label)
                        }
                    }
                    GoalType.NONE -> Unit
                }
            }

            SectionLabel("Extra tracking for this habit")
            ToggleRow(
                label = "Timer",
                checked = form.hasTimer,
                onCheckedChange = { v -> viewModel.updateForm { it.copy(hasTimer = v) } }
            )
            ToggleRow(
                label = "Journal entry (up to 150 words)",
                checked = form.hasJournal,
                onCheckedChange = { v -> viewModel.updateForm { it.copy(hasJournal = v) } }
            )
            ToggleRow(
                label = "Freeform checklist",
                checked = form.hasList,
                onCheckedChange = { v -> viewModel.updateForm { it.copy(hasList = v) } }
            )

            if (state is HabitCreateState.Failed) {
                Spacer(Modifier.height(12.dp))
                Text((state as HabitCreateState.Failed).reason, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { viewModel.saveHabit() },
                enabled = state !is HabitCreateState.Saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state is HabitCreateState.Saving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Create Habit")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = form.dueDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateForm { it.copy(dueDateMillis = datePickerState.selectedDateMillis) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Spacer(Modifier.height(20.dp))
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ColorRow(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        COLOR_OPTIONS.take(5).forEach { hex -> ColorItem(hex, selected, onSelect) }
    }
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        COLOR_OPTIONS.drop(5).forEach { hex -> ColorItem(hex, selected, onSelect) }
    }
}

@Composable
private fun ColorItem(hex: String, selected: String, onSelect: (String) -> Unit) {
    val color = Color(android.graphics.Color.parseColor(hex))
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (hex == selected) 3.dp else 0.dp,
                color = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onSelect(hex) }
            )
    )
}

@Composable
private fun IconGrid(selected: String, onSelect: (String) -> Unit) {
    // ✅ FIXED: Using structured Rows instead of LazyVerticalGrid to eliminate inner scroll crashes
    val chunks = ICON_OPTIONS.chunked(4)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chunks.forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { icon ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (icon == selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onSelect(icon) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(icon, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackingModeRow(selected: TrackingMode, onSelect: (TrackingMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TrackingMode.values().forEach { mode ->
            FilterChip(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                label = { Text(mode.name) }
            )
        }
    }
}

@Composable
private fun GoalTypeRow(selected: GoalType, onSelect: (GoalType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GoalType.values().forEach { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(type) },
                label = { Text(type.name.replace("_", " ")) })
        }
    }
}

@Composable
private fun FrequencyRow(selected: GoalFrequency?, onSelect: (GoalFrequency) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GoalFrequency.values().forEach { freq ->
            FilterChip(
                selected = selected == freq,
                onClick = { onSelect(freq) },
                label = { Text(freq.name) })
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}