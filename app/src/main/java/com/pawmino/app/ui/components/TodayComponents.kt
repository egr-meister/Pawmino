package com.pawmino.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pawmino.app.model.CareCategory
import com.pawmino.app.model.PetProfile
import com.pawmino.app.model.ShoppingItem
import com.pawmino.app.ui.theme.CategoryStyle
import com.pawmino.app.ui.theme.WarmCream
import com.pawmino.app.util.ActiveReminder
import com.pawmino.app.util.CategorySegment
import com.pawmino.app.util.DateTimeUtil
import com.pawmino.app.util.NextTask
import com.pawmino.app.util.ReminderKind

/** Compact pet switcher at the very top of Today. */
@Composable
fun PetSwitcher(
    pets: List<PetProfile>,
    activePetId: String?,
    onSelect: (String) -> Unit,
    onAddPet: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        pets.forEach { pet ->
            FilterChip(
                selected = pet.id == activePetId,
                onClick = { onSelect(pet.id) },
                label = { Text(pet.name.ifBlank { "Unnamed" }) },
                leadingIcon = {
                    Box(
                        modifier = Modifier.size(10.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            )
        }
        AssistChip(
            onClick = onAddPet,
            label = { Text("Add pet") },
            leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
    }
}

/** Narrow date header strip with prev/next and a jump-to-today control. */
@Composable
fun DateHeaderStrip(
    dateIso: String,
    isToday: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isToday) "Today" else DateTimeUtil.displayDate(dateIso),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (isToday) {
                Text(DateTimeUtil.displayDate(dateIso), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                TextButton(onClick = onToday) { Text("Jump to today") }
            }
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Next day")
        }
    }
}

/** The small "Next care task" ribbon attached below the loop. */
@Composable
fun NextTaskRibbon(
    next: NextTask?,
    allComplete: Boolean,
    prefer24: Boolean,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val (label, sub, instanceId) = when {
        next != null -> Triple(
            "Next: ${next.task.title}",
            "Scheduled for ${DateTimeUtil.displayTime(next.instance.scheduledTime, prefer24)}",
            next.instance.id
        )
        allComplete -> Triple("Daily routine complete", "Every scheduled task is done", null)
        else -> Triple("No upcoming timed tasks", "Untimed tasks appear under Anytime", null)
    }
    Surface(
        color = WarmCream,
        shape = RoundedCornerShape(50),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .then(if (instanceId != null) Modifier.clickable { onOpen(instanceId) } else Modifier)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            if (instanceId != null) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** In-app reminder panel; shown only when a reminder condition is met. */
@Composable
fun ReminderPanel(
    reminders: List<ActiveReminder>,
    onOpenTask: (String) -> Unit,
    onOpenShopping: () -> Unit,
    onDismiss: (ReminderKind) -> Unit,
    modifier: Modifier = Modifier
) {
    if (reminders.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        reminders.take(3).forEach { reminder ->
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(
                        Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(reminder.message, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(reminder.detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onDismiss(reminder.kind) }) { Text("Not Now") }
                    when {
                        reminder.taskInstanceId != null ->
                            TextButton(onClick = { onOpenTask(reminder.taskInstanceId) }) { Text("Open Task") }
                        reminder.kind == ReminderKind.Shopping ->
                            TextButton(onClick = onOpenShopping) { Text("Open List") }
                        else -> {}
                    }
                }
            }
        }
    }
}

/** Horizontally scrollable category filter chips with completed/total counts. */
@Composable
fun CategoryChipsRow(
    segments: List<CategorySegment>,
    selected: CareCategory?,
    onSelect: (CareCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (segments.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text("All") }
        )
        segments.forEach { seg ->
            FilterChip(
                selected = selected == seg.category,
                onClick = { onSelect(if (selected == seg.category) null else seg.category) },
                label = { Text("${seg.label} ${seg.completed}/${seg.total}") },
                leadingIcon = {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(CategoryStyle.color(seg.category)))
                },
                colors = FilterChipDefaults.filterChipColors()
            )
        }
    }
}

/** Compact shopping-list preview near the bottom of Today. */
@Composable
fun ShoppingPreview(
    items: List<ShoppingItem>,
    onOpen: () -> Unit,
    onToggle: (ShoppingItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Shopping list", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = onOpen) { Text("Open") }
            }
            if (items.isEmpty()) {
                Text("No unchecked items.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                items.take(3).forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { onToggle(item) }.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(16.dp).clip(RoundedCornerShape(4.dp))
                                .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                        )
                        Text(
                            item.title,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 10.dp).weight(1f)
                        )
                        if (item.quantityLabel.isNotBlank()) {
                            Text(item.quantityLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
