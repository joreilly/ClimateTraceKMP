package dev.johnoreilly.climatetrace.data

import dev.johnoreilly.climatetrace.remote.DataMetadata
import dev.johnoreilly.climatetrace.remote.TemporalCoverage
import kotlin.test.Test
import kotlin.test.assertEquals

class DataCoverageTest {
    private fun coverage(start: String?, end: String?) =
        DataCoverage.from(DataMetadata(temporalCoverage = TemporalCoverage(start = start, end = end)))

    @Test
    fun yearInProgressIsListedButIsNotTheDefault() {
        val coverage = coverage("2015-01", "2026-08")
        assertEquals((2016..2026).map { it.toString() }, coverage.years)
        assertEquals("2025", coverage.latestYear)
        assertEquals("2026", coverage.partialYear)
        assertEquals(8, coverage.partialThroughMonth)
        assertEquals((2016..2025).map { it.toString() }, coverage.completeYears)
    }

    @Test
    fun completeFinalYearIsTheDefaultAndNotPartial() {
        val coverage = coverage("2015-01", "2026-12")
        assertEquals("2026", coverage.latestYear)
        assertEquals((2016..2026).map { it.toString() }, coverage.years)
        assertEquals(null, coverage.partialYear)
        assertEquals(null, coverage.partialThroughMonth)
    }

    @Test
    fun shortHistoryIsNotPaddedOut() {
        assertEquals(listOf("2023", "2024", "2025", "2026"), coverage("2023-01", "2026-08").years)
    }

    @Test
    fun partYearAtTheStartIsLeftOut() {
        assertEquals("2016", coverage("2015-03", "2025-12").years.first())
    }

    @Test
    fun unusableCoverageFallsBackToFixedYears() {
        assertEquals(DataCoverage.Fallback.years, coverage(null, null).years)
        assertEquals(DataCoverage.Fallback.years, coverage("garbage", "2026-08").years)
        assertEquals(DataCoverage.Fallback.years, coverage("2026-01", "2026-08").years)
    }
}
