package com.pawmino.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import com.pawmino.app.model.ShoppingCategory
import com.pawmino.app.model.ShoppingItem
import com.pawmino.app.model.ShoppingPriority
import com.pawmino.app.ui.components.ConfirmDialog
import com.pawmino.app.ui.components.EmptyState
import com.pawmino.app.ui.components.EnumDropdown
import com.pawmino.app.ui.components.PawTextField
import com.pawmino.app.ui.components.PawTopBar
import com.pawmino.app.ui.navigation.AppNavigator
import com.pawmino.app.ui.theme.ErrorRed
import com.pawmino.app.ui.viewmodel.PawminoUiState
import com.pawmino.app.ui.viewmodel.PawminoViewModel
import com.pawmino.app.util.ShoppingUtil
import com.pawmino.app.util.Validation

@Composable
fun ShoppingListScreen(
    vm: PawminoViewModel,
    state: PawminoUiState,
    nav: AppNavigator
) {
    var categoryFilter by remember { mutableStateOf<ShoppingCategory?>(null) }
    var thisPetOnly by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ShoppingItem?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var pendingClear by remember { mutableStateOf(false) }

    val petId = state.activePet?.id

    Scaffold(
        topBar = {
            PawTopBar(title = "Shopping list", actions = {
                TextButton(onClick = { pendingClear = true }) { Text("Clear checked") }
            })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add item") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = categoryFilter == null, onClick = { categoryFilter = null }, label = { Text("All") })
                ShoppingCategory.entries.forEach { c ->
                    FilterChip(selected = categoryFilter == c, onClick = { categoryFilter = if (categoryFilter == c) null else c }, label = { Text(c.label) })
                }
            }
            Row(modifier = Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !thisPetOnly, onClick = { thisPetOnly = false }, label = { Text("All pets") })
                FilterChip(
                    selected = thisPetOnly,
                    onClick = { thisPetOnly = true },
                    enabled = petId != null,
                    label = { Text("This pet") }
                )
            }

            val filtered = ShoppingUtil.filter(
                items = state.data.shoppingItems,
                category = categoryFilter,
                petId = if (thisPetOnly) petId else null,
                includeAllPets = true
            )
            val sorted = ShoppingUtil.sort(filtered)

            if (sorted.isEmpty()) {
                EmptyState(
                    title = "Shopping list is empty",
                    message = "Add pet supplies to keep track. No stores, prices, or purchase links.",
                    actionLabel = "Add Item",
                    onAction = { showAdd = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(sorted, key = { it.id }) { item ->
                        ShoppingRow(
                            item = item,
                            petName = state.data.pets.firstOrNull { it.id == item.petId }?.name,
                            onToggle = { vm.setShoppingChecked(item.id, !item.checked) },
                            onEdit = { editing = item }
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        ShoppingDialog(
            initial = null,
            defaultPetId = petId,
            onDismiss = { showAdd = false },
            onSave = { pet, title, category, qty, priority, note ->
                vm.saveShoppingItem(null, pet, title, category, qty, priority, note)
                showAdd = false
            },
            onDelete = null
        )
    }
    editing?.let { item ->
        ShoppingDialog(
            initial = item,
            defaultPetId = item.petId,
            onDismiss = { editing = null },
            onSave = { pet, title, category, qty, priority, note ->
                vm.saveShoppingItem(item.id, pet, title, category, qty, priority, note)
                editing = null
            },
            onDelete = { vm.deleteShoppingItem(item.id); editing = null }
        )
    }
    if (pendingClear) {
        ConfirmDialog(
            title = "Clear checked items?",
            text = "Checked shopping items will be removed.",
            confirmLabel = "Clear",
            destructive = true,
            onConfirm = { vm.clearCheckedShoppingItems(null); pendingClear = false },
            onDismiss = { pendingClear = false }
        )
    }
}

@Composable
private fun ShoppingRow(
    item: ShoppingItem,
    petName: String?,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 2.dp, bottom = 2.dp)) {
            Checkbox(checked = item.checked, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None
                    )
                    if (item.priority == ShoppingPriority.High) {
                        Box(modifier = Modifier.padding(start = 6.dp).clip(RoundedCornerShape(50)).background(ErrorRed.copy(alpha = 0.15f)).padding(horizontal = 6.dp)) {
                            Text("High", style = MaterialTheme.typography.labelSmall, color = ErrorRed)
                        }
                    }
                }
                val sub = buildList {
                    add(item.category.label)
                    if (item.quantityLabel.isNotBlank()) add(item.quantityLabel)
                    petName?.let { add(it) }
                }.joinToString(" · ")
                Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onEdit) { Text("Edit") }
        }
    }
}

@Composable
private fun ShoppingDialog(
    initial: ShoppingItem?,
    defaultPetId: String?,
    onDismiss: () -> Unit,
    onSave: (petId: String?, title: String, category: ShoppingCategory, qty: String, priority: ShoppingPriority, note: String) -> Unit,
    onDelete: (() -> Unit)?
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: ShoppingCategory.Food) }
    var qty by remember { mutableStateOf(initial?.quantityLabel ?: "") }
    var priority by remember { mutableStateOf(initial?.priority ?: ShoppingPriority.Normal) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var assignToPet by remember { mutableStateOf((initial?.petId ?: defaultPetId) != null) }
    var submitted by remember { mutableStateOf(false) }

    val titleError = if (submitted) Validation.shoppingTitleError(title) else null

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(if (initial == null) "Add item" else "Edit item", style = MaterialTheme.typography.titleMedium)
            PawTextField(value = title, onValueChange = { title = it }, label = "Item name *", error = titleError)
            EnumDropdown(label = "Category", options = ShoppingCategory.entries, selected = category, optionLabel = { it.label }, onSelect = { category = it })
            PawTextField(value = qty, onValueChange = { qty = it }, label = "Quantity (optional)")
            EnumDropdown(label = "Priority", options = ShoppingPriority.entries, selected = priority, optionLabel = { it.label }, onSelect = { priority = it })
            PawTextField(value = note, onValueChange = { note = it }, label = "Note (optional)", singleLine = false, minLines = 2)
            if (defaultPetId != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(selected = assignToPet, onClick = { assignToPet = true }, label = { Text("This pet") }, modifier = Modifier.padding(end = 8.dp))
                    FilterChip(selected = !assignToPet, onClick = { assignToPet = false }, label = { Text("All pets") })
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(onClick = {
                    submitted = true
                    if (Validation.shoppingTitleError(title) == null) {
                        val pet = if (assignToPet) defaultPetId else null
                        onSave(pet, title, category, qty, priority, note)
                    }
                }) { Text("Save") }
            }
        }
    }
}
