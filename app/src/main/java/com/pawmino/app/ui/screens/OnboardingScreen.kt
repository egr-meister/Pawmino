package com.pawmino.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pawmino.app.model.CareCategory
import com.pawmino.app.ui.components.CareLoop
import com.pawmino.app.ui.components.DisclaimerCard
import com.pawmino.app.util.CategorySegment
import com.pawmino.app.util.LoopSummary

@Composable
fun OnboardingScreen(
    onCreatePet: () -> Unit,
    onExplore: () -> Unit
) {
    // A simplified, illustrative loop (not real data) — no mascot, no photo.
    val demo = LoopSummary(
        segments = listOf(
            CategorySegment(CareCategory.Feeding, "Feeding", 2, 2, 0, 0),
            CategorySegment(CareCategory.Walk, "Walk", 2, 1, 0, 1),
            CategorySegment(CareCategory.Grooming, "Grooming", 1, 1, 0, 0),
            CategorySegment(CareCategory.Play, "Play", 1, 0, 0, 1)
        ),
        totalTasks = 6, completedTasks = 4, skippedTasks = 0, pendingTasks = 2
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Pawmino", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Organize your pet's daily routine and keep a clear local care history.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        CareLoop(summary = demo, petName = "Milo", modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(0.8f))

        OnboardingPoint("Keep daily care in one clear loop.")
        OnboardingPoint("Create feeding, walk, grooming, play, and custom tasks.")
        OnboardingPoint("Mark each activity manually and review previous days.")
        OnboardingPoint("Use the local calendar, history, and shopping list.")
        OnboardingPoint("Reminders appear inside the app — no push notifications, no background use.")
        OnboardingPoint("Your pet profiles and routines stay on this device.")

        DisclaimerCard(
            "Pawmino is a manual pet care routine organizer. Tasks, schedules, notes, and " +
                "completion records are entered by the user. The app does not provide veterinary " +
                "advice, diagnosis, treatment, emergency guidance, or medical recommendations."
        )

        Spacer(Modifier.height(4.dp))
        Button(onClick = onCreatePet, modifier = Modifier.fillMaxWidth()) { Text("Create Pet Profile") }
        TextButton(onClick = onExplore, modifier = Modifier.fillMaxWidth()) { Text("Explore First") }
    }
}

@Composable
private fun OnboardingPoint(text: String) {
    Text(
        text = "• $text",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth()
    )
}
