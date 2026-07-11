package com.pawmino.app.util

import com.pawmino.app.model.ShoppingCategory
import com.pawmino.app.model.ShoppingItem
import com.pawmino.app.model.ShoppingPriority

object ShoppingUtil {

    /**
     * Sort order: unchecked before checked; then High priority before Normal; then by
     * creation time (oldest first) with title as a stable tiebreaker.
     */
    fun sort(items: List<ShoppingItem>): List<ShoppingItem> =
        items.sortedWith(
            compareBy<ShoppingItem> { it.checked }
                .thenBy { it.priority != ShoppingPriority.High }
                .thenBy { it.createdAt }
                .thenBy { it.title.lowercase() }
        )

    fun filter(
        items: List<ShoppingItem>,
        category: ShoppingCategory?,
        petId: String?,
        includeAllPets: Boolean
    ): List<ShoppingItem> = items.filter { item ->
        val categoryOk = category == null || item.category == category
        val petOk = when {
            petId == null -> true
            item.petId == null -> includeAllPets
            else -> item.petId == petId
        }
        categoryOk && petOk
    }
}
