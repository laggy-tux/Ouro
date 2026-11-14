package com.example.red_panda

import com.example.red_panda.data.repository.eventDateSortMillis
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventDateOrderingTest {
    @Test
    fun explicitEventDateOverridesPhotoAndCreationOrder() {
        val eventDate = LocalDate.of(2026, 10, 31).atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        val laterPhotoDate = LocalDate.of(2026, 11, 4).atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()

        assertEquals(eventDate, eventDateSortMillis("Oct 31, 2026", listOf(laterPhotoDate)))
    }

    @Test
    fun dateRangeUsesItsLatestActualDate() {
        val expected = LocalDate.of(2026, 10, 26).atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()

        assertEquals(expected, eventDateSortMillis("25-10-26 to 26-10-26", emptyList()))
    }

    @Test
    fun unparseableEventDateFallsBackToNewestPhotoCaptureTime() {
        assertEquals(3_000L, eventDateSortMillis("Trip", listOf(1_000L, 3_000L, 0L)))
    }

    @Test
    fun unknownEventDateAndCaptureTimesHaveNoSortDate() {
        assertNull(eventDateSortMillis("Trip", listOf(0L, -1L)))
    }
}
