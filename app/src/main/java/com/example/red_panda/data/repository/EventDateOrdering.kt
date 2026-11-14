package com.example.red_panda.data.repository

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Locale

internal fun eventDateSortMillis(eventDate: String, photoCaptureTimes: Iterable<Long>): Long? {
    parseEventDateMillis(eventDate)?.let { return it }
    return photoCaptureTimes.asSequence().filter { it > 0L }.maxOrNull()
}

private fun parseEventDateMillis(value: String): Long? {
    val dateParts = value.trim()
        .split(EVENT_DATE_RANGE_SEPARATOR)
        .filter(String::isNotBlank)
    val locales = listOf(Locale.getDefault(), Locale.US).distinct()
    val dates = dateParts.flatMap { datePart ->
        locales.flatMap { locale ->
            EVENT_DATE_PATTERNS.mapNotNull { pattern ->
                try {
                    LocalDate.parse(
                        datePart,
                        DateTimeFormatter.ofPattern(pattern, locale)
                            .withResolverStyle(ResolverStyle.STRICT)
                    )
                } catch (_: DateTimeParseException) {
                    null
                }
            }
        }
    }

    return dates.maxOrNull()
        ?.atStartOfDay(ZoneId.systemDefault())
        ?.toInstant()
        ?.toEpochMilli()
}

private val EVENT_DATE_RANGE_SEPARATOR = Regex("\\s+(?:to|–|—|-)\\s+", RegexOption.IGNORE_CASE)
private val EVENT_DATE_PATTERNS = listOf(
    "MMM d, uuuu",
    "MMMM d, uuuu",
    "d MMM uuuu",
    "d MMMM uuuu",
    "uuuu-MM-dd",
    "uuuu/MM/dd",
    "dd-MM-uuuu",
    "d-M-uuuu",
    "dd-MM-uu",
    "d-M-uu",
    "MM/dd/uuuu",
    "M/d/uuuu",
    "MM/dd/uu",
    "M/d/uu"
)
