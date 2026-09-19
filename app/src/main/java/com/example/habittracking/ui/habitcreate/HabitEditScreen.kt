package com.example.habittracking.ui.habitcreate

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.habittracking.data.model.GoalFrequency
import com.example.habittracking.data.model.GoalType
import com.example.habittracking.data.model.TrackingMode
import com.example.habittracking.viewmodel.HabitEditState
import com.example.habittracking.viewmodel.HabitEditViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditScreen(
    onDone: () -> Unit,
    onCancel: () -> Unit,
    viewModel: HabitEditViewModel = hiltViewModel()
) {
    val form by viewModel.form.collectAsState()
    val state by viewModel.state.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state is HabitEditState.Saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Habit") },
                navigationIcon = { TextButton(onClick = onCancel) { Text("Cancel") } }
            )
        }
    ) { padding ->
        if (state is HabitEditState.Loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
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

                SectionLabel("Extra tracking features")
                ToggleRow(label = "Timer", checked = form.hasTimer, onCheckedChange = { v -> viewModel.updateForm { it.copy(hasTimer = v) } })
                ToggleRow(label = "Journal entry", checked = form.hasJournal, onCheckedChange = { v -> viewModel.updateForm { it.copy(hasJournal = v) } })
                ToggleRow(label = "Checklist", checked = form.hasList, onCheckedChange = { v -> viewModel.updateForm { it.copy(hasList = v) } })

                if (state is HabitEditState.Failed) {
                    Spacer(Modifier.height(12.dp))
                    Text((state as HabitEditState.Failed).reason, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.updateHabit() },
                    enabled = state !is HabitEditState.Saving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state is HabitEditState.Saving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text("Save Changes")
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
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

// ✅ FIXED: Added missing reusable form rows below to allow standalone compilation success

@Composable
private fun ColorRow(selected: String, onSelect: (String) -> Unit) {
    val colors = listOf("#4C6EF5", "#22B8CF", "#12B886", "#FAB005", "#FA5252", "#E64980", "#BE4BDB")
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        colors.forEach { hex ->
            val color = remember(hex) { Color(android.graphics.Color.parseColor(hex)) }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (selected == hex) 3.dp else 0.dp,
                        color = if (selected == hex) MaterialTheme.colorScheme.outline else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onSelect(hex) }
            )
        }
    }
}

@Composable
private fun IconGrid(selected: String, onSelect: (String) -> Unit) {
    val icons = listOf("🎯", "🏃", "💧", "📚", "🧘", "🥦", "🛌", "💻", "🎨", "🎹", "🪵", "🧹")
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        icons.take(6).forEach { icon ->
            IconButtonSample(icon = icon, isSelected = selected == icon, onClick = { onSelect(icon) })
        }
    }
    Spacer(Modifier.height(6.dp))
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        icons.drop(6).forEach { icon ->
            IconButtonSample(icon = icon, isSelected = selected == icon, onClick = { onSelect(icon) })
        }
    }
}

@Composable
private fun IconButtonSample(icon: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(icon, fontSize = 20.sp)
    }
}

@Composable
private fun TrackingModeRow(selected: TrackingMode, onSelect: (TrackingMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilterChip(
            selected = selected == TrackingMode.TALLY,
            onClick = { onSelect(TrackingMode.TALLY) },
            label = { Text("Simple Tally Counter") }
        )
        FilterChip(
            selected = selected == TrackingMode.GOAL,
            onClick = { onSelect(TrackingMode.GOAL) },
            label = { Text("Targets & Deadlines") }
        )
    }
}


@Composable
private fun GoalTypeRow(selected: GoalType, onSelect: (GoalType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilterChip(
            selected = selected == GoalType.COUNT,
            onClick = { onSelect(GoalType.COUNT) },
            label = { Text("Numeric Target") })
        FilterChip (selected =
            selected == GoalType.DUE_DATE,
            onClick = { onSelect(GoalType.DUE_DATE) },
            label = {
            Text(
                "Completion Due Date"
            )
        })
    }
}

@Composable
private fun FrequencyRow(selected: GoalFrequency, onSelect: (GoalFrequency) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GoalFrequency.values().forEach { freq ->
            FilterChip(
                selected = selected == freq,
                onClick = { onSelect(freq) },
                label = { Text(freq.name.lowercase().replaceFirstChar { it.uppercase() }) })
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
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch (checked = checked, onCheckedChange = onCheckedChange)
    }
}