package com.ernvk23.grayzen

import org.junit.Assert.assertEquals
import org.junit.Test

class ForegroundTimingTest {
    @Test fun storedValuesAreClampedToTheSupportedRange() {
        assertEquals(0, sanitizeForegroundTimingMs(-1))
        assertEquals(250, sanitizeForegroundTimingMs(251))
        assertEquals(113, sanitizeForegroundTimingMs(113))
        assertEquals(11, sanitizeForegroundTimingMs(11))
    }
}
