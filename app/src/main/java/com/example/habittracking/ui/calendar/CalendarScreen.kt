package com.example.habittracking.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.habittracking.viewmodel.CalendarViewModel
import com.example.habittracking.viewmodel.HabitDot
import com.kizitonwose.calendar.compose.CalendarState
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val dotsByDate by viewModel.dotsByDate.collectAsState()
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(24) }
    val endMonth = remember { currentMonth.plusMonths(6) }
    val firstDayOfWeek = remember { daysOfWeek().first() }

    val calendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(calendarState) {
        snapshotFlow { calendarState.firstVisibleMonth.yearMonth }.collect { month ->
            viewModel.setVisibleMonth(month)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            MonthHeader(
                calendarState = calendarState,
                onPrevious = {
                    coroutineScope.launch {
                        calendarState.animateScrollToMonth(calendarState.firstVisibleMonth.yearMonth.minusMonths(1))
                    }
                },
                onNext = {
                    coroutineScope.launch {
                        calendarState.animateScrollToMonth(calendarState.firstVisibleMonth.yearMonth.plusMonths(1))
                    }
                }
            )

            WeekdayHeader(firstDayOfWeek)

            HorizontalCalendar(
                state = calendarState,
                dayContent = { day ->
                    DayCell(
                        day = day,
                        dots = dotsByDate[day.date] ?: emptyList(),
                        isSelected = day.date == selectedDate,
                        onClick = { selectedDate = day.date }
                    )
                }
            )

            selectedDate?.let { date ->
                Spacer(Modifier.height(8.dp))
                SelectedDayPanel(date = date, dots = dotsByDate[date] ?: emptyList())
            }
        }
    }
}

@Composable
private fun MonthHeader(calendarState: CalendarState, onPrevious: () -> Unit, onNext: () -> Unit) {
    val visibleMonth = calendarState.firstVisibleMonth.yearMonth
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onPrevious) { Text("‹") }
        Text(
            "${visibleMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${visibleMonth.year}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        TextButton(onClick = onNext) { Text("›") }
    }
}

@Composable
private fun WeekdayHeader(firstDayOfWeek: DayOfWeek) {
    val days = remember(firstDayOfWeek) { daysOfWeek(firstDayOfWeek) }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        days.forEach { day ->
            Text(
                day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DayCell(day: CalendarDay, dots: List<HabitDot>, isSelected: Boolean, onClick: () -> Unit) {
    val isCurrentMonth = day.position == DayPosition.MonthDate
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(enabled = isCurrentMonth) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = if (isCurrentMonth) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(2.dp))

        // ✅ FIXED: Placed both indicators and text labels on the same line to protect layout square limits
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            dots.take(3).forEach { dot ->
                val color = remember(dot.color) {
                    runCatching { Color(android.graphics.Color.parseColor(dot.color)) }.getOrDefault(Color.Gray)
                }
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
            }
            if (dots.size > 3) {
                Text(
                    text = "+${dots.size - 3}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SelectedDayPanel(date: LocalDate, dots: List<HabitDot>) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        if (dots.isEmpty()) {
            Text(
                "No activity logged this day.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            dots.forEach { dot ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    val color = remember(dot.color) {
                        runCatching { Color(android.graphics.Color.parseColor(dot.color)) }.getOrDefault(Color.Gray)
                    }
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                    Spacer(Modifier.width(8.dp))
                    Text(dot.icon)
                    Spacer(Modifier.width(6.dp))
                    Text(dot.habitName, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
