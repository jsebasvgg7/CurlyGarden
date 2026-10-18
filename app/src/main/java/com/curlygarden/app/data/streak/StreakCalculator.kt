package com.curlygarden.app.data.streak

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val KEY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

const val DEFAULT_TIMEZONE = "America/Bogota"

private fun zoneOf(timezone: String): ZoneId =
    try {
        ZoneId.of(timezone)
    } catch (e: Exception) {
        ZoneId.of(DEFAULT_TIMEZONE)
    }

fun todayKeyFor(timezone: String): String =
    LocalDate.now(zoneOf(timezone)).format(KEY_FORMATTER)

fun dateKeyFor(date: LocalDate): String = date.format(KEY_FORMATTER)

fun calculateStreak(streakLog: Map<String, Boolean>, timezone: String): Int {
    val today = LocalDate.now(zoneOf(timezone))
    val hasToday = streakLog[dateKeyFor(today)] == true

    var cursor = if (hasToday) today else today.minusDays(1)
    var count = 0
    while (streakLog[dateKeyFor(cursor)] == true) {
        count++
        cursor = cursor.minusDays(1)
    }
    return count
}

fun weekDaysCompleted(streakLog: Map<String, Boolean>, timezone: String): Set<LocalDate> {
    val today = LocalDate.now(zoneOf(timezone))
    val sunday = today.minusDays(today.dayOfWeek.value.toLong() % 7)
    return (0..6L)
        .map { sunday.plusDays(it) }
        .filter { streakLog[dateKeyFor(it)] == true }
        .toSet()
}
