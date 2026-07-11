package com.pawmino.app.model

import kotlinx.serialization.Serializable

/**
 * All Pawmino enums. Every enum is annotated for kotlinx.serialization and carries a
 * human-readable [label]. Unknown/legacy values deserialize to a safe default via the
 * repository's tolerant parsing, so adding values later stays backward compatible.
 */

@Serializable
enum class PetType(val label: String) {
    Dog("Dog"),
    Cat("Cat"),
    Bird("Bird"),
    Rabbit("Rabbit"),
    SmallPet("Small Pet"),
    Reptile("Reptile"),
    Fish("Fish"),
    Other("Other");

    companion object {
        fun fromName(name: String?): PetType =
            entries.firstOrNull { it.name == name } ?: Other
    }
}

@Serializable
enum class CareCategory(val label: String) {
    Feeding("Feeding"),
    Walk("Walk"),
    Grooming("Grooming"),
    Play("Play"),
    Water("Water"),
    Cleaning("Cleaning"),
    Training("Training"),
    Litter("Litter"),
    Other("Other");

    companion object {
        fun fromName(name: String?): CareCategory =
            entries.firstOrNull { it.name == name } ?: Other
    }
}

@Serializable
enum class ScheduleType(val label: String) {
    Daily("Every day"),
    SelectedDays("Selected days"),
    OneTime("One time"),
    EveryNumberOfDays("Every N days"),
    Unscheduled("Unscheduled");

    companion object {
        fun fromName(name: String?): ScheduleType =
            entries.firstOrNull { it.name == name } ?: Daily
    }
}

@Serializable
enum class WeekDay(val label: String, val shortLabel: String, val isoNumber: Int) {
    Monday("Monday", "Mon", 1),
    Tuesday("Tuesday", "Tue", 2),
    Wednesday("Wednesday", "Wed", 3),
    Thursday("Thursday", "Thu", 4),
    Friday("Friday", "Fri", 5),
    Saturday("Saturday", "Sat", 6),
    Sunday("Sunday", "Sun", 7);

    companion object {
        fun fromIso(iso: Int): WeekDay = entries.firstOrNull { it.isoNumber == iso } ?: Monday
        fun fromName(name: String?): WeekDay =
            entries.firstOrNull { it.name == name } ?: Monday
    }
}

@Serializable
enum class TaskStatus(val label: String) {
    Pending("Pending"),
    Completed("Completed"),
    Skipped("Skipped");

    companion object {
        fun fromName(name: String?): TaskStatus =
            entries.firstOrNull { it.name == name } ?: Pending
    }
}

@Serializable
enum class DistanceUnit(val label: String) {
    Kilometers("km"),
    Miles("mi"),
    NotTracked("Not tracked");

    companion object {
        fun fromName(name: String?): DistanceUnit =
            entries.firstOrNull { it.name == name } ?: NotTracked
    }
}

@Serializable
enum class ShoppingCategory(val label: String) {
    Food("Food"),
    Treats("Treats"),
    Grooming("Grooming"),
    Cleaning("Cleaning"),
    Toys("Toys"),
    Accessories("Accessories"),
    Other("Other");

    companion object {
        fun fromName(name: String?): ShoppingCategory =
            entries.firstOrNull { it.name == name } ?: Other
    }
}

@Serializable
enum class ShoppingPriority(val label: String) {
    Normal("Normal"),
    High("High");

    companion object {
        fun fromName(name: String?): ShoppingPriority =
            entries.firstOrNull { it.name == name } ?: Normal
    }
}

@Serializable
enum class TimeFormatPreference(val label: String) {
    SystemDefault("System default"),
    TwelveHour("12-hour"),
    TwentyFourHour("24-hour");

    companion object {
        fun fromName(name: String?): TimeFormatPreference =
            entries.firstOrNull { it.name == name } ?: SystemDefault
    }
}
