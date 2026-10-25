package com.ernvk23.grayzen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AccessibilityReconnectCoordinatorTest {
    private class Access : AccessibilityComponentControl {
        val events = mutableListOf<String>()
        var enabled = true
        var disableAccepted = true

        override fun enableOwnService(): Boolean {
            events += "enable"
            enabled = true
            return true
        }

        override fun disableOwnService(): Boolean {
            events += "disable"
            if (disableAccepted) enabled = false
            return disableAccepted
        }

        override fun isOwnServiceEnabled() = enabled
    }

    private val access = Access()
    private var managerActive = true
    private var permission = true
    private var entryPresent = true
    private var connected = false
    private var consumed = false
    private lateinit var subject: AccessibilityReconnectCoordinator

    @Before fun setUp() {
        subject = AccessibilityReconnectCoordinator(
            accessibility = access,
            managerActive = { managerActive },
            hasPermission = { permission },
            entryPresent = { entryPresent },
            serviceConnected = { connected },
            attemptConsumed = { consumed },
            consumeAttempt = { consumed = true },
            rearmAttempt = { consumed = false },
        )
    }

    @Test fun eligibleLossRunsOneImmediateCycle() {
        assertTrue(subject.onDisconnected())
        assertEquals(listOf("disable", "enable"), access.events)
        assertTrue(access.enabled)
        assertTrue(consumed)

        assertFalse(subject.onDisconnected())
        assertEquals(listOf("disable", "enable"), access.events)
    }

    @Test fun deliberateOffDoesNotWrite() {
        managerActive = false
        assertFalse(subject.onDisconnected())
        assertTrue(access.events.isEmpty())
        assertFalse(consumed)
    }

    @Test fun missingPermissionAndAbsentEntryBlockAutomaticRecovery() {
        permission = false
        assertFalse(subject.onDisconnected())
        assertFalse(consumed)

        permission = true
        entryPresent = false
        assertFalse(subject.onDisconnected())
        assertFalse(subject.reassess())
        assertTrue(access.events.isEmpty())

        assertFalse(consumed)
    }

    @Test fun refusedWriteConsumesAttemptAndStopsAutomaticRetries() {
        access.disableAccepted = false
        assertFalse(subject.onDisconnected())
        assertEquals(listOf("disable"), access.events)
        assertTrue(consumed)

        assertFalse(subject.onDisconnected())
        assertEquals(listOf("disable"), access.events)

    }

    @Test fun appResumeStartsRecoveryAndRearmsOnlyWhileConnected() {
        consumed = true
        connected = true
        assertFalse(subject.reassess())
        assertTrue(consumed)
        subject.rearmIfConnected()
        assertFalse(consumed)

        connected = false
        assertTrue(subject.reassess())
        assertEquals(listOf("disable", "enable"), access.events)
        assertTrue(consumed)

        assertFalse(subject.reassess())
        assertEquals(listOf("disable", "enable"), access.events)
    }
}
