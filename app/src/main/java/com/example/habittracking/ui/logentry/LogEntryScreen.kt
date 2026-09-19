package com.example.habittracking.ui.logentry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.habittracking.data.model.ChecklistItem
import com.example.habittracking.viewmodel.LogEntryViewModel
import com.example.habittracking.viewmodel.LogSaveState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogEntryScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: LogEntryViewModel = hiltViewModel()
) {
    val habit by viewModel.habit.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    LaunchedEffect(saveState) {
        if (saveState is LogSaveState.Saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(habit?.name ?: "Log entry") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
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
            habit?.let { h ->
                CheckInSection(viewModel)

                if (h.hasTimer) {
                    SectionDivider("Timer")
                    TimerSection(viewModel)
                }
                if (h.hasJournal) {
                    SectionDivider("Journal")
                    JournalSection(viewModel)
                }
                if (h.hasList) {
                    SectionDivider("Checklist")
                    ChecklistSection(viewModel)
                }
            }

            if (saveState is LogSaveState.Failed) {
                Spacer(Modifier.height(12.dp))
                Text((saveState as LogSaveState.Failed).reason, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = { viewModel.saveLog() },
                enabled = saveState !is LogSaveState.Saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (saveState is LogSaveState.Saving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Save")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionDivider(label: String) {
    Spacer(Modifier.height(24.dp))
    Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun CheckInSection(viewModel: LogEntryViewModel) {
    val count by viewModel.count.collectAsState()
    val completed by viewModel.completed.collectAsState()

    Column {
        Text(
            if (completed) "Checked in $count ${if (count == 1) "time" else "times"} today" else "Not checked in yet today",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { viewModel.checkIn() }) { Text("Check in") }
            if (count > 0) {
                OutlinedButton(onClick = { viewModel.undoCheckIn() }) { Text("Undo") }
            }
        }
    }
}

@Composable
private fun TimerSection(viewModel: LogEntryViewModel) {
    val seconds by viewModel.timerSeconds.collectAsState()
    val running by viewModel.timerRunning.collectAsState()

    val minutes = seconds / 60
    val secs = seconds % 60

    Column {
        Text(
            "%02d:%02d".format(minutes, secs),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { viewModel.toggleTimer() }) {
                Icon(
                    imageVector = if (running) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(Modifier.width(6.dp))
                Text(if (running) "Pause" else "Start")
            }
            OutlinedButton(onClick = { viewModel.resetTimer() }) { Text("Reset") }
        }
    }
}

@Composable
private fun JournalSection(viewModel: LogEntryViewModel) {
    val text by viewModel.journalText.collectAsState()
    val wordCount by viewModel.journalWordCount.collectAsState()

    Column {
        OutlinedTextField(
            value = text,
            onValueChange = { viewModel.updateJournalText(it) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            placeholder = { Text("How did it go?") }
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "$wordCount / 150 words",
            style = MaterialTheme.typography.labelSmall,
            color = if (wordCount >= 150) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ChecklistSection(viewModel: LogEntryViewModel) {
    val items by viewModel.listItems.collectAsState()
    val suggested by viewModel.suggestedItems.collectAsState()
    var newItemText by remember { mutableStateOf("") }

    Column {
        if (suggested.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Copy yesterday's ${suggested.size} item${if (suggested.size == 1) "" else "s"}?",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = { viewModel.applySuggestedList() }) { Text("Copy") }
                TextButton(onClick = { viewModel.dismissSuggestedList() }) { Text("No") }
            }
            Spacer(Modifier.height(12.dp))
        }

        items.forEachIndexed { index, item ->
            ChecklistRow(
                item = item,
                onToggle = { viewModel.toggleChecklistItem(index) },
                onRemove = { viewModel.removeChecklistItem(index) }
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newItemText,
                onValueChange = { newItemText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add an item") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                // ✅ FIXED: Closed out the syntax brackets correctly
                keyboardActions = KeyboardActions(onDone = {
                    if (newItemText.isNotBlank()) {
                        viewModel.addChecklistItem(newItemText.trim())
                        newItemText = ""
                    }
                })
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newItemText.isNotBlank()) {
                        viewModel.addChecklistItem(newItemText.trim())
                        newItemText = ""
                    }
                }
            ) {
                Text("Add")
            }
        }
    }
}

// ✅ FIXED: Implemented the missing functional ChecklistRow layout
@Composable
private fun ChecklistRow(
    item: ChecklistItem,
    onToggle: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.checked,
            onCheckedChange = { onToggle() }
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = item.text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove item",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
