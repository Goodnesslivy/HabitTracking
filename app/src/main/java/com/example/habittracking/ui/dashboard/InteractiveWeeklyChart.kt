package com.example.habittracking.ui.dashboard

import androidx.compose.ui.text.TextStyle
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habittracking.data.model.ChartDayData

@Composable
fun InteractiveWeeklyChart(
    weeklyData: List<ChartDayData>,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    // Animation trigger to smoothly grow the bars upon opening the screen
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(20.dp)
    ) {
        Text(
            text = "Weekly Activity",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Tooltip displaying completion statistics upon interaction
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp),
            contentAlignment = Alignment.Center
        ) {
            if (selectedIndex != null && selectedIndex!! < weeklyData.size) {
                val selectedData = weeklyData[selectedIndex!!]
                Text(
                    text = "${selectedData.completedCount} of ${selectedData.totalCount} completed (${(selectedData.percentage * 100).toInt()}%)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "Tap a bar to see completion metrics",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Custom Graphics Render Layer
        val barColor = MaterialTheme.colorScheme.primary
        val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)

        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .pointerInput(weeklyData) {
                    detectTapGestures { offset ->
                        val barWidthWithSpacing = size.width / weeklyData.size
                        val index = (offset.x / barWidthWithSpacing).toInt()
                        selectedIndex = if (index in weeklyData.indices) index else null
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val barCount = weeklyData.size
            val individualBarWidth = (canvasWidth / barCount) * 0.45f
            val spaceBetweenBars = canvasWidth / barCount

            weeklyData.forEachIndexed { index, data ->
                val centerOfBarX = (index * spaceBetweenBars) + (spaceBetweenBars / 2)
                val leftCoordinateX = centerOfBarX - (individualBarWidth / 2)

                // 1. Draw Background Rails (Tracks)
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(leftCoordinateX, 0f),
                    size = Size(individualBarWidth, canvasHeight),
                    cornerRadius = CornerRadius(individualBarWidth / 2, individualBarWidth / 2)
                )

                // 2. Draw Progress Bars (Animate Height)
                val barFillHeight = canvasHeight * data.percentage * animatedProgress
                val topCoordinateY = canvasHeight - barFillHeight

                drawRoundRect(
                    color = if (selectedIndex == index) barColor.copy(alpha = 0.8f) else barColor,
                    topLeft = Offset(leftCoordinateX, topCoordinateY),
                    size = Size(individualBarWidth, barFillHeight),
                    cornerRadius = CornerRadius(individualBarWidth / 2, individualBarWidth / 2)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Bottom Labels Row
        Row(modifier = Modifier.fillMaxWidth()) {
            weeklyData.forEachIndexed { index, data ->
                Text(
                    text = data.dayLabel,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
