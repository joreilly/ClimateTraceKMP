package dev.johnoreilly.climatetrace.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class ClimateTraceA2uiComponentsTest {

    // EmissionsChart used "%,.1f".format(), which is JVM-only; the replacement must match it.
    @Test
    fun formatOneDecimalMatchesJvmGroupedFormat() {
        assertEquals("0.0", 0.0.formatOneDecimal())
        assertEquals("8.3", 8.26.formatOneDecimal())
        assertEquals("999.9", 999.94.formatOneDecimal())
        assertEquals("1,000.0", 999.96.formatOneDecimal())
        assertEquals("1,263.4", 1263.401.formatOneDecimal())
        assertEquals("12,345,678.9", 12_345_678.9.formatOneDecimal())
        assertEquals("-1,234.5", (-1234.5).formatOneDecimal())
    }
}
