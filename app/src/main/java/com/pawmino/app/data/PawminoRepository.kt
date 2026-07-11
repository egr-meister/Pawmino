package com.pawmino.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pawmino.app.model.AppData
import com.pawmino.app.model.AppSettings
import com.pawmino.app.model.CareTask
import com.pawmino.app.model.DailyTaskInstance
import com.pawmino.app.model.PetNote
import com.pawmino.app.model.PetProfile
import com.pawmino.app.model.ReminderSettings
import com.pawmino.app.model.ShoppingItem
import com.pawmino.app.model.TaskStatus
import com.pawmino.app.model.WalkLog
import com.pawmino.app.util.DateTimeUtil
import com.pawmino.app.util.TaskGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pawmino")

/**
 * Single local repository for all Pawmino data. Backed by DataStore Preferences with one
 * serialized JSON string per entity list plus settings. No network, no cloud, no accounts.
 *
 * All reads are exposed as a [Flow]; all writes are transactional across keys via [mutate].
 */
class PawminoRepository(private val context: Context) {

    private object Keys {
        val PETS = stringPreferencesKey("pets_json")
        val CARE_TASKS = stringPreferencesKey("care_tasks_json")
        val TASK_INSTANCES = stringPreferencesKey("task_instances_json")
        val WALK_LOGS = stringPreferencesKey("walk_logs_json")
        val PET_NOTES = stringPreferencesKey("pet_notes_json")
        val SHOPPING_ITEMS = stringPreferencesKey("shopping_items_json")
        val SETTINGS = stringPreferencesKey("settings_json")
    }

    private val petsSer = ListSerializer(PetProfile.serializer())
    private val tasksSer = ListSerializer(CareTask.serializer())
    private val instancesSer = ListSerializer(DailyTaskInstance.serializer())
    private val walksSer = ListSerializer(WalkLog.serializer())
    private val notesSer = ListSerializer(PetNote.serializer())
    private val shoppingSer = ListSerializer(ShoppingItem.serializer())

    val appData: Flow<AppData> = context.dataStore.data
        .catch { e ->
            // A read error (e.g. corrupt store) should never crash observers.
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs -> readAppData(prefs) }

    private fun readAppData(prefs: Preferences): AppData {
        val settings = JsonStore.decodeObjectSafe(
            AppSettings.serializer(), prefs[Keys.SETTINGS], AppSettings()
        )
        return AppData(
            pets = JsonStore.decodeListSafe(PetProfile.serializer(), petsSer, prefs[Keys.PETS]),
            careTasks = JsonStore.decodeListSafe(CareTask.serializer(), tasksSer, prefs[Keys.CARE_TASKS]),
            taskInstances = JsonStore.decodeListSafe(DailyTaskInstance.serializer(), instancesSer, prefs[Keys.TASK_INSTANCES]),
            walkLogs = JsonStore.decodeListSafe(WalkLog.serializer(), walksSer, prefs[Keys.WALK_LOGS]),
            petNotes = JsonStore.decodeListSafe(PetNote.serializer(), notesSer, prefs[Keys.PET_NOTES]),
            shoppingItems = JsonStore.decodeListSafe(ShoppingItem.serializer(), shoppingSer, prefs[Keys.SHOPPING_ITEMS]),
            settings = settings
        )
    }

    private fun writeAppData(prefs: androidx.datastore.preferences.core.MutablePreferences, data: AppData) {
        prefs[Keys.PETS] = JsonStore.encodeList(petsSer, data.pets)
        prefs[Keys.CARE_TASKS] = JsonStore.encodeList(tasksSer, data.careTasks)
        prefs[Keys.TASK_INSTANCES] = JsonStore.encodeList(instancesSer, data.taskInstances)
        prefs[Keys.WALK_LOGS] = JsonStore.encodeList(walksSer, data.walkLogs)
        prefs[Keys.PET_NOTES] = JsonStore.encodeList(notesSer, data.petNotes)
        prefs[Keys.SHOPPING_ITEMS] = JsonStore.encodeList(shoppingSer, data.shoppingItems)
        prefs[Keys.SETTINGS] = JsonStore.json.encodeToString(AppSettings.serializer(), data.settings)
    }

    /** Read-modify-write across all keys in a single atomic DataStore edit. */
    private suspend fun mutate(transform: (AppData) -> AppData) {
        context.dataStore.edit { prefs ->
            val current = readAppData(prefs)
            val updated = transform(current)
            writeAppData(prefs, updated)
        }
    }

    // ---------------------------------------------------------------------------------------
    // Settings & onboarding
    // ---------------------------------------------------------------------------------------

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) =
        mutate { it.copy(settings = transform(it.settings)) }

    suspend fun setOnboardingCompleted(completed: Boolean) =
        updateSettings { it.copy(onboardingCompleted = completed) }

    suspend fun setActivePet(petId: String?) =
        updateSettings { it.copy(activePetId = petId) }

    suspend fun updateReminderSettings(transform: (ReminderSettings) -> ReminderSettings) =
        updateSettings { it.copy(reminderSettings = transform(it.reminderSettings)) }

    // ---------------------------------------------------------------------------------------
    // Pets
    // ---------------------------------------------------------------------------------------

    suspend fun upsertPet(pet: PetProfile) = mutate { data ->
        val exists = data.pets.any { it.id == pet.id }
        val pets = if (exists) data.pets.map { if (it.id == pet.id) pet else it } else data.pets + pet
        // First pet becomes the active pet automatically.
        val active = data.settings.activePetId ?: pet.id
        data.copy(pets = pets, settings = data.settings.copy(activePetId = active))
    }

    /** Delete a pet and everything linked to it, then re-select a sensible active pet. */
    suspend fun deletePet(petId: String) = mutate { data ->
        val remainingPets = data.pets.filterNot { it.id == petId }
        val newActive = when {
            data.settings.activePetId != petId -> data.settings.activePetId
            remainingPets.isNotEmpty() -> remainingPets.first().id
            else -> null
        }
        data.copy(
            pets = remainingPets,
            careTasks = data.careTasks.filterNot { it.petId == petId },
            taskInstances = data.taskInstances.filterNot { it.petId == petId },
            walkLogs = data.walkLogs.filterNot { it.petId == petId },
            petNotes = data.petNotes.filterNot { it.petId == petId },
            shoppingItems = data.shoppingItems.filterNot { it.petId == petId },
            settings = data.settings.copy(activePetId = newActive)
        )
    }

    /** Remove completion history and logs for one pet, keeping the pet and its task setup. */
    suspend fun deletePetHistory(petId: String) = mutate { data ->
        data.copy(
            taskInstances = data.taskInstances.filterNot { it.petId == petId },
            walkLogs = data.walkLogs.filterNot { it.petId == petId },
            petNotes = data.petNotes.filterNot { it.petId == petId }
        )
    }

    // ---------------------------------------------------------------------------------------
    // Care tasks
    // ---------------------------------------------------------------------------------------

    suspend fun upsertTask(task: CareTask) = mutate { data ->
        val exists = data.careTasks.any { it.id == task.id }
        val tasks = if (exists) data.careTasks.map { if (it.id == task.id) task else it } else data.careTasks + task
        data.copy(careTasks = tasks)
    }

    suspend fun setTaskEnabled(taskId: String, enabled: Boolean) = mutate { data ->
        val now = DateTimeUtil.nowTimestamp()
        data.copy(careTasks = data.careTasks.map {
            if (it.id == taskId) it.copy(enabled = enabled, updatedAt = now) else it
        })
    }

    /** Delete a task definition. Existing instances are kept for history with a fallback label. */
    suspend fun deleteTask(taskId: String) = mutate { data ->
        data.copy(careTasks = data.careTasks.filterNot { it.id == taskId })
    }

    suspend fun applyTemplate(template: RoutineTemplate, petId: String) = mutate { data ->
        val startOrder = (data.careTasks.filter { it.petId == petId }.maxOfOrNull { it.sortOrder } ?: -1) + 1
        val newTasks = template.build(petId).mapIndexed { i, t -> t.copy(sortOrder = startOrder + i) }
        data.copy(careTasks = data.careTasks + newTasks)
    }

    // ---------------------------------------------------------------------------------------
    // Task instances
    // ---------------------------------------------------------------------------------------

    /** Generate any missing instances for the pet + date without overwriting existing ones. */
    suspend fun ensureInstances(petId: String, dateIso: String) = mutate { data ->
        val (merged, added) = TaskGenerator.ensureInstances(
            data.taskInstances, data.careTasks, petId, dateIso
        )
        if (added.isEmpty()) data else data.copy(taskInstances = merged)
    }

    /**
     * Persist a status change. The caller passes the resolved instance (existing or freshly
     * generated) so status changes work from the loop, timeline, day detail, or calendar even
     * before an instance has been persisted.
     */
    suspend fun setInstanceStatus(instance: DailyTaskInstance, status: TaskStatus, note: String? = null) =
        mutate { data ->
            val now = DateTimeUtil.nowTimestamp()
            val updated = instance.copy(
                status = status,
                completedAt = if (status == TaskStatus.Completed) now else "",
                note = note ?: instance.note,
                updatedAt = now,
                createdAt = instance.createdAt.ifBlank { now }
            )
            val exists = data.taskInstances.any { it.id == updated.id }
            val list = if (exists) {
                data.taskInstances.map { if (it.id == updated.id) updated else it }
            } else {
                data.taskInstances + updated
            }
            data.copy(taskInstances = list)
        }

    suspend fun updateInstanceNote(instanceId: String, note: String) = mutate { data ->
        val now = DateTimeUtil.nowTimestamp()
        data.copy(taskInstances = data.taskInstances.map {
            if (it.id == instanceId) it.copy(note = note, updatedAt = now) else it
        })
    }

    suspend fun deleteInstance(instanceId: String) = mutate { data ->
        data.copy(taskInstances = data.taskInstances.filterNot { it.id == instanceId })
    }

    // ---------------------------------------------------------------------------------------
    // Walk logs
    // ---------------------------------------------------------------------------------------

    suspend fun upsertWalk(walk: WalkLog) = mutate { data ->
        val exists = data.walkLogs.any { it.id == walk.id }
        val list = if (exists) data.walkLogs.map { if (it.id == walk.id) walk else it } else data.walkLogs + walk
        data.copy(walkLogs = list)
    }

    suspend fun deleteWalk(walkId: String) = mutate { data ->
        data.copy(walkLogs = data.walkLogs.filterNot { it.id == walkId })
    }

    // ---------------------------------------------------------------------------------------
    // Notes
    // ---------------------------------------------------------------------------------------

    suspend fun upsertNote(note: PetNote) = mutate { data ->
        val exists = data.petNotes.any { it.id == note.id }
        val list = if (exists) data.petNotes.map { if (it.id == note.id) note else it } else data.petNotes + note
        data.copy(petNotes = list)
    }

    suspend fun deleteNote(noteId: String) = mutate { data ->
        data.copy(petNotes = data.petNotes.filterNot { it.id == noteId })
    }

    // ---------------------------------------------------------------------------------------
    // Shopping items
    // ---------------------------------------------------------------------------------------

    suspend fun upsertShoppingItem(item: ShoppingItem) = mutate { data ->
        val exists = data.shoppingItems.any { it.id == item.id }
        val list = if (exists) data.shoppingItems.map { if (it.id == item.id) item else it } else data.shoppingItems + item
        data.copy(shoppingItems = list)
    }

    suspend fun setShoppingChecked(itemId: String, checked: Boolean) = mutate { data ->
        val now = DateTimeUtil.nowTimestamp()
        data.copy(shoppingItems = data.shoppingItems.map {
            if (it.id == itemId) it.copy(checked = checked, updatedAt = now) else it
        })
    }

    suspend fun deleteShoppingItem(itemId: String) = mutate { data ->
        data.copy(shoppingItems = data.shoppingItems.filterNot { it.id == itemId })
    }

    /** Clear checked items. If [petId] is null, clears checked items for every pet + shared. */
    suspend fun clearCheckedShoppingItems(petId: String?) = mutate { data ->
        val remaining = data.shoppingItems.filterNot { item ->
            item.checked && (petId == null || item.petId == petId)
        }
        data.copy(shoppingItems = remaining)
    }

    // ---------------------------------------------------------------------------------------
    // Reset
    // ---------------------------------------------------------------------------------------

    suspend fun resetAllData() {
        context.dataStore.edit { it.clear() }
    }
}
