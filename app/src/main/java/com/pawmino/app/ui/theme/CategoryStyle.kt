package com.pawmino.app.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SportsBaseball
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.pawmino.app.model.CareCategory
import com.pawmino.app.model.TaskStatus

/** Central mapping of care categories to a consistent color and icon. */
object CategoryStyle {

    fun color(category: CareCategory): Color = when (category) {
        CareCategory.Feeding -> FeedingCoral
        CareCategory.Walk -> WalkBlue
        CareCategory.Grooming -> GroomingLavender
        CareCategory.Play -> PlayAmber
        CareCategory.Water -> WaterAqua
        CareCategory.Cleaning -> CleaningSage
        CareCategory.Training -> TrainingRose
        CareCategory.Litter -> CleaningSage
        CareCategory.Other -> OtherGray
    }

    fun icon(category: CareCategory): ImageVector = when (category) {
        CareCategory.Feeding -> Icons.Outlined.RestaurantMenu
        CareCategory.Walk -> Icons.Outlined.DirectionsWalk
        CareCategory.Grooming -> Icons.Outlined.Brush
        CareCategory.Play -> Icons.Outlined.SportsBaseball
        CareCategory.Water -> Icons.Outlined.WaterDrop
        CareCategory.Cleaning -> Icons.Outlined.CleaningServices
        CareCategory.Training -> Icons.Outlined.School
        CareCategory.Litter -> Icons.Outlined.Inventory2
        CareCategory.Other -> Icons.Outlined.Pets
    }

    fun statusColor(status: TaskStatus): Color = when (status) {
        TaskStatus.Completed -> StatusCompleted
        TaskStatus.Pending -> StatusPending
        TaskStatus.Skipped -> StatusSkipped
    }

    fun statusIcon(status: TaskStatus): ImageVector = when (status) {
        TaskStatus.Completed -> Icons.Filled.CheckCircle
        TaskStatus.Pending -> Icons.Filled.Schedule
        TaskStatus.Skipped -> Icons.Filled.Schedule
    }
}
