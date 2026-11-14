package com.example.red_panda.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaCaptureDateResolverTest {
    @Test
    fun dateTakenHasHighestPriority() {
        assertEquals(
            4_000L,
            selectCaptureTime(
                dateTaken = 4_000L,
                readEmbeddedCaptureTime = { 3_000L },
                mediaStoreModified = 2_000L,
                readFileModified = { 1_000L }
            )
        )
    }

    @Test
    fun embeddedCaptureTimeIsUsedWhenDateTakenIsMissing() {
        assertEquals(
            3_000L,
            selectCaptureTime(
                dateTaken = null,
                readEmbeddedCaptureTime = { 3_000L },
                mediaStoreModified = 2_000L,
                readFileModified = { 1_000L }
            )
        )
    }

    @Test
    fun modifiedTimeIsUsedBeforeFileStatFallback() {
        assertEquals(
            2_000L,
            selectCaptureTime(
                dateTaken = null,
                readEmbeddedCaptureTime = { null },
                mediaStoreModified = 2_000L,
                readFileModified = { 1_000L }
            )
        )
    }

    @Test
    fun fileModifiedTimeIsUsedWhenEarlierSourcesAreMissing() {
        assertEquals(
            1_000L,
            selectCaptureTime(
                dateTaken = null,
                readEmbeddedCaptureTime = { null },
                mediaStoreModified = null,
                readFileModified = { 1_000L }
            )
        )
    }

    @Test
    fun nonPositiveAndMissingTimesResolveToUnknown() {
        assertEquals(
            0L,
            selectCaptureTime(
                dateTaken = 0L,
                readEmbeddedCaptureTime = { -1L },
                mediaStoreModified = 0L,
                readFileModified = { null }
            )
        )
    }
}
