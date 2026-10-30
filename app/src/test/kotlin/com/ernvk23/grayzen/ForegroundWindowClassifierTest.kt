package io.github.ernvk23.grayzen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundWindowClassifierTest {
    private val classifier = ForegroundWindowClassifier()
    private val apps = setOf("app.one", "app.two", "pip")

    private fun app(packageName: String, focused: Boolean = true, pip: Boolean = false) =
        WindowMetadata(packageName, focused, focused, 1, pip)

    @Test fun settledApplicationIsAccepted() {
        val result = classifier.classify(listOf(app("app.one")), apps)
        assertEquals("app.one", (result as ForegroundClassification.Settled).packageName)
    }

    @Test fun focusedOwnWindowIsAuthoritative() {
        val own = "io.github.ernvk23.grayzen"
        assertTrue(classifier.isOwnWindowFocused(listOf(app(own)), own))
        assertFalse(classifier.isOwnWindowFocused(listOf(app(own, focused = false)), own))
        assertFalse(classifier.isOwnWindowFocused(listOf(app(own, pip = true)), own))
    }

    @Test fun backgroundAppBehindFocusedAppDuringGestureIsTemporary() {
        val gesture = listOf(app("app.one"), app("app.two", focused = false))
        assertTrue(classifier.classify(gesture, apps) is ForegroundClassification.Temporary)
    }

    @Test fun systemUiOverAnAppIsNotMistakenForDestination() {
        val overlay = listOf(
            WindowMetadata("system.ui", true, true, ForegroundWindowClassifier.WINDOW_TYPE_SYSTEM),
            WindowMetadata("app.one", false, false, ForegroundWindowClassifier.WINDOW_TYPE_APPLICATION),
        )
        assertTrue(classifier.classify(overlay, apps) is ForegroundClassification.NonLaunchable)
    }

    @Test fun layeredDestinationRetriesThenBecomesOverlay() {
        val windows = listOf(app("app.two"), app("app.one", focused = false))
        assertTrue(classifier.classify(windows, apps) is ForegroundClassification.Temporary)
        assertTrue(classifier.classify(windows, apps, retryExpired = true) is ForegroundClassification.Overlay)
    }

    @Test fun splitScreenAcceptsFocusedApp() {
        val windows = listOf(
            app("app.two"), app("app.one", focused = false),
            WindowMetadata(null, false, false, ForegroundWindowClassifier.WINDOW_TYPE_SPLIT_SCREEN_DIVIDER),
        )
        assertTrue(classifier.classify(windows, apps) is ForegroundClassification.Settled)
    }

    @Test fun pictureInPictureDoesNotBlockDestination() {
        val result = classifier.classify(listOf(app("app.two"), app("pip", false, true)), apps)
        assertEquals("app.two", (result as ForegroundClassification.Settled).packageName)
    }

    @Test fun focusedPictureInPictureAppIsSettled() {
        val result = classifier.classify(listOf(app("app.one", focused = false), app("pip", pip = true)), apps)
        assertEquals("pip", (result as ForegroundClassification.Settled).packageName)
    }

    @Test fun focusedPictureInPictureAppOverridesLayeredFullScreenApps() {
        val windows = listOf(
            app("app.one", focused = false),
            app("app.two", focused = false),
            app("pip", pip = true),
        )
        val result = classifier.classify(windows, apps)
        assertEquals("pip", (result as ForegroundClassification.Settled).packageName)
    }

    @Test fun transientsAndMissingRootsAreNotApplied() {
        assertTrue(classifier.classify(listOf(app("keyboard")), apps) is ForegroundClassification.NonLaunchable)
        val missing = WindowMetadata(null, true, true, 1, hasRoot = false)
        assertTrue(classifier.classify(listOf(missing), apps) is ForegroundClassification.Temporary)
    }

    @Test fun clearDestinationAppliesWithoutSecondSample() {
        val recovery = ForegroundRecovery()
        val app = ForegroundClassification.Settled("app.one", "settled")
        assertEquals(ForegroundRecoveryAction.Apply, recovery.next(app, initialDelayMs = 25, nowMs = 0))
    }

    @Test fun zeroForegroundDelayStillUsesPositiveRecoveryWaits() {
        val recovery = ForegroundRecovery()
        val temporary = ForegroundClassification.Temporary("transition")
        assertEquals(ForegroundRecoveryAction.Retry(5L), recovery.next(temporary, initialDelayMs = 0, nowMs = 0))
        assertEquals(ForegroundRecoveryAction.Retry(7L), recovery.next(temporary, initialDelayMs = 0, nowMs = 5))
    }

    @Test fun recoveryGrowsUntilOneSecondOrTenRetries() {
        val temporary = ForegroundClassification.Temporary("transition")
        val cases = listOf(
            Triple(10, listOf(13L, 17L, 22L, 29L, 37L, 48L, 63L, 82L, 106L, 138L), 555L),
            Triple(20, listOf(26L, 34L, 44L, 57L, 74L, 97L, 125L, 163L, 212L, 168L), 1_000L),
            Triple(25, listOf(33L, 42L, 55L, 71L, 93L, 121L, 157L, 204L, 224L), 1_000L),
            Triple(50, listOf(65L, 85L, 110L, 143L, 186L, 241L, 170L), 1_000L),
        )
        for ((initialDelay, waits, recoveryDuration) in cases) {
            val recovery = ForegroundRecovery()
            var now = 10_000L + initialDelay
            for (wait in waits) {
                assertEquals(
                    ForegroundRecoveryAction.Retry(wait),
                    recovery.next(temporary, initialDelayMs = initialDelay, nowMs = now),
                )
                now += wait
            }
            assertEquals(10_000L + initialDelay + recoveryDuration, now)
            assertTrue(recovery.retryExpired(now))
            assertEquals(ForegroundRecoveryAction.Stop, recovery.next(temporary, initialDelay, now))
        }
    }

    @Test fun delayedCallbacksAreCappedByTheRecoveryWindow() {
        val recovery = ForegroundRecovery()
        val temporary = ForegroundClassification.Temporary("transition")
        assertEquals(ForegroundRecoveryAction.Retry(33L), recovery.next(temporary, 25, 25))
        assertEquals(ForegroundRecoveryAction.Retry(42L), recovery.next(temporary, 25, 900))
        assertEquals(ForegroundRecoveryAction.Retry(55L), recovery.next(temporary, 25, 956))
        assertEquals(ForegroundRecoveryAction.Retry(14L), recovery.next(temporary, 25, 1_011))
        assertTrue(recovery.retryExpired(1_025))
        assertEquals(ForegroundRecoveryAction.Stop, recovery.next(temporary, 25, 1_025))
    }

    @Test fun recoveryNeverSchedulesMoreThanTenRetries() {
        val recovery = ForegroundRecovery()
        val temporary = ForegroundClassification.Temporary("transition")
        repeat(10) {
            assertTrue(recovery.next(temporary, 10, 0) is ForegroundRecoveryAction.Retry)
        }
        assertTrue(recovery.isExhausted(0))
        assertEquals(ForegroundRecoveryAction.Stop, recovery.next(temporary, 10, 0))
    }

    @Test fun missingRootRecoversToClearDestination() {
        val recovery = ForegroundRecovery()
        val missing = ForegroundClassification.Temporary("focused window root unavailable")
        val app = ForegroundClassification.Settled("app.one", "settled")
        assertEquals(ForegroundRecoveryAction.Retry(33L), recovery.next(missing, 25, 25))
        assertEquals(ForegroundRecoveryAction.Apply, recovery.next(app, 25, 58))
        assertEquals(ForegroundRecoveryAction.Retry(33L), recovery.next(missing, 25, 2_025))
    }

    @Test fun clearDestinationOnLastRetryApplies() {
        val recovery = ForegroundRecovery()
        val temporary = ForegroundClassification.Temporary("transition")
        val app = ForegroundClassification.Settled("app.one", "settled")
        assertEquals(ForegroundRecoveryAction.Retry(325L), recovery.next(temporary, 250, 250))
        assertEquals(ForegroundRecoveryAction.Retry(423L), recovery.next(temporary, 250, 575))
        assertEquals(ForegroundRecoveryAction.Retry(252L), recovery.next(temporary, 250, 998))
        assertTrue(recovery.retryExpired(1_250))
        assertEquals(ForegroundRecoveryAction.Apply, recovery.next(app, 250, 1_250))
    }

    @Test fun overlayStopsRecovery() {
        val recovery = ForegroundRecovery()
        val overlay = ForegroundClassification.Overlay("application layer persisted")
        assertEquals(ForegroundRecoveryAction.Stop, recovery.next(overlay, 25, 0))
    }
}
