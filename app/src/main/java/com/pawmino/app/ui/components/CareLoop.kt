package com.pawmino.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pawmino.app.ui.theme.DividerColor
import com.pawmino.app.ui.theme.LoopPercentTextStyle
import com.pawmino.app.ui.theme.StatusCompleted
import com.pawmino.app.ui.theme.StatusSkipped
import com.pawmino.app.ui.theme.CategoryStyle
import com.pawmino.app.util.LoopSummary

/**
 * The Daily Pet Care Loop — a segmented circle drawn with Compose Canvas (no chart library).
 * Each segment is a care category sized by its task count; the filled portion shows completion,
 * a neutral gray shows skipped, and a faint track shows what remains.
 *
 * A full textual summary is attached for screen readers via [clearAndSetSemantics].
 */
@Composable
fun CareLoop(
    summary: LoopSummary,
    petName: String,
    modifier: Modifier = Modifier
) {
    val description = summary.accessibilityText(petName)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .aspectRatio(1f)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.11f
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)

            // Faint full-circle track for readability in every state.
            drawArc(
                color = DividerColor.copy(alpha = 0.5f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke)
            )

            val total = summary.totalTasks
            if (total <= 0 || summary.segments.isEmpty()) {
                return@Canvas
            }

            val segmentCount = summary.segments.size
            val gap = if (segmentCount > 1) 5f else 0f
            val available = 360f - gap * segmentCount
            var start = -90f + gap / 2f

            summary.segments.forEach { seg ->
                val sweep = available * (seg.total.toFloat() / total.toFloat())
                val catColor = CategoryStyle.color(seg.category)

                // Category track (faint).
                drawArc(
                    color = catColor.copy(alpha = 0.20f),
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke)
                )

                if (seg.total > 0) {
                    val completedSweep = sweep * (seg.completed.toFloat() / seg.total.toFloat())
                    val skippedSweep = sweep * (seg.skipped.toFloat() / seg.total.toFloat())

                    if (completedSweep > 0f) {
                        drawArc(
                            color = catColor,
                            startAngle = start,
                            sweepAngle = completedSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke)
                        )
                    }
                    if (skippedSweep > 0f) {
                        drawArc(
                            color = StatusSkipped.copy(alpha = 0.55f),
                            startAngle = start + completedSweep,
                            sweepAngle = skippedSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke)
                        )
                    }
                }

                start += sweep + gap
            }
        }

        LoopCenter(summary = summary, petName = petName)
    }
}

@Composable
private fun LoopCenter(summary: LoopSummary, petName: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 40.dp)
    ) {
        Text(
            text = petName.ifBlank { "Your pet" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        when {
            summary.isEmpty -> {
                Text(
                    text = "No care tasks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            summary.allComplete -> {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = StatusCompleted,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = "Routine complete",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StatusCompleted,
                    textAlign = TextAlign.Center
                )
            }
            else -> {
                Text(text = "${summary.percent}%", style = LoopPercentTextStyle, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = "${summary.completedTasks} of ${summary.totalTasks} complete",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
