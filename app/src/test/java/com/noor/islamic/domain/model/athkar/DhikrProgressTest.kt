package com.noor.islamic.domain.model.athkar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The count contract in isolation: a prescribed count yields a real target, and an
 * unspecified count yields none of the derived numbers the UI would otherwise
 * display. Nothing here may substitute a default.
 */
class DhikrProgressTest {

    @Test
    fun aPrescribedCountExposesItsTarget() {
        listOf(1, 3, 7, 33, 100).forEach { value ->
            assertEquals(value, DhikrProgress.targetOf(DhikrCount.Prescribed(value)))
        }
    }

    @Test
    fun anUnspecifiedCountExposesNoTarget() {
        assertNull(DhikrProgress.targetOf(DhikrCount.Unspecified))
    }

    @Test
    fun aNonPositiveDeclaredCountExposesNoTarget() {
        // Not reachable through a valid corpus, but a bad value must not become a
        // usable one if the validator is ever bypassed.
        assertNull(DhikrProgress.targetOf(DhikrCount.Prescribed(0)))
        assertNull(DhikrProgress.targetOf(DhikrCount.Prescribed(-1)))
    }

    @Test
    fun prescribedIsCompleteOnlyAtItsTarget() {
        val count = DhikrCount.Prescribed(33)

        assertFalse(DhikrProgress.isComplete(count, 0))
        assertFalse(DhikrProgress.isComplete(count, 32))
        assertTrue(DhikrProgress.isComplete(count, 33))
        assertTrue(DhikrProgress.isComplete(count, 34))
    }

    @Test
    fun unspecifiedIsNeverComplete() {
        listOf(0, 1, 3, 7, 33, 100, 10_000).forEach { counted ->
            assertFalse(
                "counting $counted must not complete an unspecified dhikr",
                DhikrProgress.isComplete(DhikrCount.Unspecified, counted),
            )
        }
    }

    @Test
    fun unspecifiedHasNoRemainingAndNoFraction() {
        assertNull(DhikrProgress.remainingOf(DhikrCount.Unspecified, 12))
        assertNull(DhikrProgress.fractionOf(DhikrCount.Unspecified, 12))
    }

    @Test
    fun prescribedRemainingAndFractionTrackTheTarget() {
        val count = DhikrCount.Prescribed(33)

        assertEquals(33, DhikrProgress.remainingOf(count, 0))
        assertEquals(0, DhikrProgress.remainingOf(count, 33))
        assertEquals(0, DhikrProgress.remainingOf(count, 40))
        assertEquals(0f, DhikrProgress.fractionOf(count, 0)!!, 0.0001f)
        assertEquals(16f / 33f, DhikrProgress.fractionOf(count, 16)!!, 0.0001f)
        assertEquals(1f, DhikrProgress.fractionOf(count, 40)!!, 0.0001f)
    }

    @Test
    fun nextCountStopsAtThePrescribedTarget() {
        val count = DhikrCount.Prescribed(3)

        assertEquals(1, DhikrProgress.nextCount(count, 0))
        assertEquals(3, DhikrProgress.nextCount(count, 2))
        assertEquals(3, DhikrProgress.nextCount(count, 3))
    }

    @Test
    fun nextCountKeepsIncreasingWhenNoCountIsPrescribed() {
        var counted = 0
        val sequence = (1..250).map {
            counted = DhikrProgress.nextCount(DhikrCount.Unspecified, counted)
            counted
        }

        assertEquals((1..250).toList(), sequence)
        // No traditional default is treated as a ceiling: the count climbs past all of them.
        listOf(1, 3, 7, 33, 100).forEach { invented ->
            assertTrue(
                "count must climb past $invented, not stop there",
                sequence.any { it > invented },
            )
        }
    }

    @Test
    fun nextCountSaturatesInsteadOfOverflowing() {
        assertEquals(
            Int.MAX_VALUE,
            DhikrProgress.nextCount(DhikrCount.Unspecified, Int.MAX_VALUE),
        )
    }

    @Test
    fun clampStoredCapsPrescribedAndLeavesUnspecifiedAlone() {
        assertEquals(33, DhikrProgress.clampStored(DhikrCount.Prescribed(33), 99))
        assertEquals(0, DhikrProgress.clampStored(DhikrCount.Prescribed(33), -5))
        assertEquals(250, DhikrProgress.clampStored(DhikrCount.Unspecified, 250))
        assertEquals(0, DhikrProgress.clampStored(DhikrCount.Unspecified, -5))
    }

    @Test
    fun categoryProgressMeasuresPrescribedCountsOnly() {
        val progress = CategoryProgress(
            categoryId = "morning",
            totalDhikr = 5,
            completedDhikr = 2,
            prescribedDhikr = 3,
        )

        // 2 of 3, not 2 of 5: the two unspecified items must not dilute the ratio.
        assertTrue(progress.hasPrescribedCounts)
        assertEquals(2f / 3f, progress.fraction, 0.0001f)
    }

    @Test
    fun categoryWithNoPrescribedCountsReportsNoProgress() {
        val progress = CategoryProgress(
            categoryId = "morning",
            totalDhikr = 2,
            completedDhikr = 0,
            prescribedDhikr = 0,
        )

        assertFalse(progress.hasPrescribedCounts)
        assertFalse(progress.isComplete)
        assertEquals(0f, progress.fraction, 0.0001f)
    }
}
