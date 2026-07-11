package com.pawmino.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pawmino.app.ui.components.EmptyState
import com.pawmino.app.ui.navigation.AppNavigator
import com.pawmino.app.ui.viewmodel.PawminoUiState
import com.pawmino.app.ui.viewmodel.PawminoViewModel
import com.pawmino.app.util.DateTimeUtil
import com.pawmino.app.util.HistoryFilter
import com.pawmino.app.util.HistoryUtil

@Composable
fun HistoryScreen(
    vm: PawminoViewModel,
    state: PawminoUiState,
    nav: AppNavigator
) {
    val pet = state.activePet
    var filter by remember { mutableStateOf(HistoryFilter.All) }

    if (pet == null) {
        EmptyState(
            title = "No pet selected",
            message = "Add a pet profile to build a care history.",
            actionLabel = "Add Pet",
            onAction = { nav.toAddPet() }
        )
        return
    }

    val summaries = HistoryUtil.buildDailySummaries(
        petId = pet.id,
        tasks = state.data.careTasks,
        instances = state.data.taskInstances,
        notes = state.data.petNotes,
        walkLogs = state.data.walkLogs
    ).filter { HistoryUtil.matches(it, filter, state.data.careTasks, state.data.taskInstances, pet.id) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HistoryFilter.entries.forEach { f ->
                FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.label) })
            }
        }

        if (summaries.isEmpty()) {
            EmptyState(
                title = "No care history yet.",
                message = "Complete a task or add a note to begin the history."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(summaries, key = { it.dateIso }) { s ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Text(DateTimeUtil.displayDate(s.dateIso), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${s.completed} of ${s.total} tasks · ${s.percent}%",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            val extras = buildList {
                                if (s.skipped > 0) add("${s.skipped} skipped")
                                if (s.walkCount > 0) add("${s.walkCount} walks")
                                if (s.noteCount > 0) add("${s.noteCount} notes")
                                if (s.categoriesCompleted.isNotEmpty()) add("Done: " + s.categoriesCompleted.joinToString(", ") { it.label })
                            }
                            if (extras.isNotEmpty()) {
                                Text(extras.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            androidx.compose.material3.TextButton(onClick = { nav.toDayDetail(s.dateIso) }) { Text("Open day") }
                        }
                    }
                }
            }
        }
    }
}
