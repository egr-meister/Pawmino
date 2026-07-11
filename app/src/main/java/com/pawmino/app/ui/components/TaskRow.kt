package com.pawmino.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.pawmino.app.model.CareTask
import com.pawmino.app.model.DailyTaskInstance
import com.pawmino.app.model.TaskStatus
import com.pawmino.app.ui.theme.CategoryStyle
import com.pawmino.app.util.DateTimeUtil

/**
 * One timeline task row: category icon, title, category + optional feeding label, scheduled
 * time, a status pill, and a manual complete toggle. Tapping the row opens the task detail.
 */
@Composable
fun TaskRow(
    instance: DailyTaskInstance,
    task: CareTask?,
    prefer24: Boolean,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val category = task?.category
    val title = task?.title ?: "Deleted care task"
    val completed = instance.status == TaskStatus.Completed
    val timeText = if (instance.scheduledTime.isBlank()) "Anytime"
        else DateTimeUtil.displayTime(instance.scheduledTime, prefer24)

    val feeding = task?.let {
        listOf(it.portionLabel, it.foodLabel).filter { s -> s.isNotBlank() }.joinToString(" • ")
    }.orEmpty()

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(12.dp)
        ) {
            // Category badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background((category?.let { CategoryStyle.color(it) } ?: MaterialTheme.colorScheme.outline).copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category?.let { CategoryStyle.icon(it) } ?: Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = category?.let { CategoryStyle.color(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.padding(horizontal = 12.dp).weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeText + (task?.let { " · " + it.categoryDisplayName() } ?: ""),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (instance.note.isNotBlank()) {
                        Icon(
                            Icons.Outlined.Notes,
                            contentDescription = "Has note",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 6.dp).size(14.dp)
                        )
                    }
                }
                if (feeding.isNotBlank()) {
                    Text(
                        text = feeding,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (instance.status == TaskStatus.Skipped) {
                    StatusPill(status = TaskStatus.Skipped, modifier = Modifier.padding(top = 4.dp))
                }
            }

            val toggleLabel = if (completed) "Mark $title pending" else "Mark $title complete"
            IconButton(
                onClick = onToggleComplete,
                modifier = Modifier.clearAndSetSemantics { contentDescription = toggleLabel }
            ) {
                Icon(
                    imageVector = if (completed) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (completed) CategoryStyle.statusColor(TaskStatus.Completed) else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
