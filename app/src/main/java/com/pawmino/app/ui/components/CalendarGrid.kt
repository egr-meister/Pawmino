package com.pawmino.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pawmino.app.model.WeekDay
import com.pawmino.app.ui.theme.StatusCompleted
import com.pawmino.app.ui.theme.StatusPending
import com.pawmino.app.ui.theme.StatusSkipped
import com.pawmino.app.util.CalendarMonth
import com.pawmino.app.util.DayIndicator
import com.pawmino.app.util.DayStatus

/**
 * A month grid drawn with Compose primitives. Indicators use BOTH color and shape so status
 * is never conveyed by color alone: filled dot = all complete, half ring = partial, outline =
 * tasks existed but none complete, plus a small mark when a note exists.
 */
@Composable
fun CalendarGrid(
    month: CalendarMonth,
    selectedDateIso: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            month.weekHeaders.forEach { header ->
                Text(
                    text = header,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(vertical = 6.dp),
                    fontWeight = FontWeight.Medium
                )
            }
        }
        // 6 rows of 7.
        month.cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { cell ->
                    DayCell(
                        indicator = cell,
                        selected = cell.dateIso == selectedDateIso,
                        onClick = { onSelect(cell.dateIso) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    indicator: DayIndicator,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val inMonth = indicator.inMonth
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(2.dp)
            .aspectRatio(0.82f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(top = 4.dp)
    ) {
        Text(
            text = indicator.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = when {
                !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                selected -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
        Box(
            modifier = Modifier.padding(top = 2.dp).size(14.dp),
            contentAlignment = Alignment.Center
        ) {
            StatusIndicator(indicator.status)
        }
        if (indicator.hasNote) {
            Box(
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(StatusPending)
            )
        }
    }
}

@Composable
private fun StatusIndicator(status: DayStatus) {
    when (status) {
        DayStatus.NoData -> { /* nothing */ }
        DayStatus.AllComplete -> Canvas(Modifier.size(11.dp)) {
            drawCircle(color = StatusCompleted)
        }
        DayStatus.Partial -> Canvas(Modifier.size(11.dp)) {
            val stroke = size.minDimension * 0.18f
            drawCircle(color = StatusCompleted, style = Stroke(width = stroke))
            // Half-filled to signal partial completion.
            drawArc(
                color = StatusCompleted,
                startAngle = 90f,
                sweepAngle = 180f,
                useCenter = true
            )
        }
        DayStatus.NoneComplete -> Canvas(Modifier.size(11.dp)) {
            val stroke = size.minDimension * 0.18f
            drawCircle(color = StatusSkipped, style = Stroke(width = stroke))
        }
    }
}
