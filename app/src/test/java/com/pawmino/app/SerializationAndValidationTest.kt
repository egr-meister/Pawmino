package com.pawmino.app

import com.pawmino.app.data.JsonStore
import com.pawmino.app.model.AppSettings
import com.pawmino.app.model.PetProfile
import com.pawmino.app.model.ScheduleType
import com.pawmino.app.model.WeekDay
import com.pawmino.app.util.DateTimeUtil
import com.pawmino.app.util.Validation
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SerializationAndValidationTest {

    private val petListSer = ListSerializer(PetProfile.serializer())

    @Test
    fun roundTrip_preservesPets() {
        val pets = listOf(
            PetProfile(id = "1", name = "Milo"),
            PetProfile(id = "2", name = "Luna")
        )
        val json = JsonStore.encodeList(petListSer, pets)
        val decoded = JsonStore.decodeListSafe(PetProfile.serializer(), petListSer, json)
        assertEquals(pets, decoded)
    }

    @Test
    fun corruptedJson_fallsBackToEmpty() {
        val decoded = JsonStore.decodeListSafe(PetProfile.serializer(), petListSer, "{not valid json")
        assertTrue(decoded.isEmpty())
    }

    @Test
    fun blankJson_fallsBackToEmpty() {
        val decoded = JsonStore.decodeListSafe(PetProfile.serializer(), petListSer, "")
        assertTrue(decoded.isEmpty())
    }

    @Test
    fun oneMalformedElement_doesNotDiscardValidOnes() {
        // Second element has an invalid enum for petType; recovery keeps the valid entries.
        val raw = """
            [
              {"id":"1","name":"Milo"},
              {"id":"2","name":"Bad","petType":"NotARealType"},
              {"id":"3","name":"Luna"}
            ]
        """.trimIndent()
        val decoded = JsonStore.decodeListSafe(PetProfile.serializer(), petListSer, raw)
        // coerceInputValues repairs the bad enum, so all three survive.
        assertEquals(3, decoded.size)
        assertEquals(setOf("1", "2", "3"), decoded.map { it.id }.toSet())
    }

    @Test
    fun settings_defaultsMergeForMissingKeys() {
        val decoded = JsonStore.decodeObjectSafe(AppSettings.serializer(), """{"onboardingCompleted":true}""", AppSettings())
        assertTrue(decoded.onboardingCompleted)
        assertNotNull(decoded.reminderSettings)
        assertEquals(30, decoded.reminderSettings.leadTimeMinutes)
    }

    @Test
    fun invalidDateAndTime_areRejectedSafely() {
        assertNull(DateTimeUtil.parseDateOrNull("2026-13-40"))
        assertNull(DateTimeUtil.parseTimeOrNull("99:99"))
        assertTrue(DateTimeUtil.isValidDate("2026-07-10"))
        assertTrue(DateTimeUtil.isValidTime("07:30"))
    }

    @Test
    fun validation_petNameRequired() {
        assertNotNull(Validation.petNameError(""))
        assertNull(Validation.petNameError("Milo"))
    }

    @Test
    fun validation_scheduleRules() {
        assertNotNull(Validation.scheduleError(ScheduleType.SelectedDays, emptyList(), "", null))
        assertNull(Validation.scheduleError(ScheduleType.SelectedDays, listOf(WeekDay.Monday), "", null))
        assertNotNull(Validation.scheduleError(ScheduleType.OneTime, emptyList(), "bad-date", null))
        assertNotNull(Validation.scheduleError(ScheduleType.EveryNumberOfDays, emptyList(), "", 0))
        assertNull(Validation.scheduleError(ScheduleType.EveryNumberOfDays, emptyList(), "", 2))
        assertNull(Validation.scheduleError(ScheduleType.Daily, emptyList(), "", null))
    }

    @Test
    fun validation_noteLengthLimits() {
        assertNull(Validation.noteTitleError("short"))
        assertNotNull(Validation.noteTitleError("x".repeat(Validation.NOTE_TITLE_MAX + 1)))
        assertNotNull(Validation.noteBodyError("x".repeat(Validation.NOTE_BODY_MAX + 1)))
    }
}
