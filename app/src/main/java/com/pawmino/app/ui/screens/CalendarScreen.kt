package com.pawmino.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pawmino.app.ui.components.CalendarGrid
import com.pawmino.app.ui.components.EmptyState
import com.pawmino.app.ui.components.SwatchLegend
import com.pawmino.app.ui.navigation.AppNavigator
import com.pawmino.app.ui.theme.StatusCompleted
import com.pawmino.app.ui.theme.StatusPending
import com.pawmino.app.ui.theme.StatusSkipped
import com.pawmino.app.ui.viewmodel.PawminoUiState
import com.pawmino.app.ui.viewmodel.PawminoViewModel
import com.pawmino.app.util.CalendarUtil
import com.pawmino.app.util.DateTimeUtil
import java.time.YearMonth

@Composable
fun CalendarScreen(
    vm: PawminoViewModel,
    state: PawminoUiState,
    nav: AppNavigator
) {
    val pet = state.activePet
    if (pet == null) {
        EmptyState(
            title = "No pet selected",
            message = "Add a pet profile to view the care calendar.",
            actionLabel = "Add Pet",
            onAction = { nav.toAddPet() }
        )
        return
    }

    val initialMonth = DateTimeUtil.parseDateOrNull(state.selectedDateIso)?.let { YearMonth.from(it) } ?: YearMonth.now()
    var month by remember { mutableStateOf(initialMonth) }

    val calendarMonth = CalendarUtil.buildMonth(
        yearMonth = month,
        firstDayOfWeek = state.data.settings.firstDayOfWeek,
        petId = pet.id,
        tasks = state.data.careTasks,
        storedInstances = state.data.taskInstances,
        notes = state.data.petNotes
    )
    val selectedIndicator = calendarMonth.cells.firstOrNull { it.dateIso == state.selectedDateIso }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
            }
            Text(
                calendarMonth.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(onClick = { month = month.plusMonths(1) }) {
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Next month")
            }
        }

        CalendarGrid(
            month = calendarMonth,
            selectedDateIso = state.selectedDateIso,
            onSelect = { vm.selectDate(it) }
        )

        SwatchLegend(
            items = listOf(
                StatusCompleted to "All done",
                StatusPending to "Note",
                StatusSkipped to "Incomplete"
            )
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(DateTimeUtil.displayDate(state.selectedDateIso), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (selectedIndicator != null && selectedIndicator.total > 0) {
                    Text(
                        "${selectedIndicator.completed} of ${selectedIndicator.total} tasks complete" +
                            if (selectedIndicator.skipped > 0) " · ${selectedIndicator.skipped} skipped" else "",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text("No scheduled tasks on this day.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (selectedIndicator?.hasNote == true) {
                    Text("Has a note.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = { nav.toDayDetail(state.selectedDateIso) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Open day detail")
                }
            }
        }
    }
}
