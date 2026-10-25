package io.github.ernvk23.grayzen

/** One immediate disable/enable attempt; no timers or automatic retries. */
class AccessibilityReconnectCoordinator(
    private val accessibility: AccessibilityComponentControl,
    private val managerActive: () -> Boolean,
    private val hasPermission: () -> Boolean,
    private val entryPresent: () -> Boolean,
    private val serviceConnected: () -> Boolean,
    private val attemptConsumed: () -> Boolean,
    private val consumeAttempt: () -> Unit,
    private val rearmAttempt: () -> Unit,
) {
    fun onDisconnected(): Boolean = reconnect(knownDisconnected = true)

    /** Accessibility state changed; may start the first attempt. */
    fun reassess(): Boolean = reconnect(knownDisconnected = false)

    /** App resume with a live service makes a future loss eligible. */
    fun rearmIfConnected() {
        if (serviceConnected() && attemptConsumed()) rearmAttempt()
    }

    private fun reconnect(knownDisconnected: Boolean): Boolean {
        if (!managerActive() || !hasPermission()) return false
        if (!knownDisconnected && serviceConnected()) return false
        if (!entryPresent() || attemptConsumed()) return false

        consumeAttempt()
        return try {
            // Request a fresh binding by resetting only our own entry.
            accessibility.disableOwnService() && accessibility.enableOwnService()
        } catch (_: SecurityException) {
            false
        } catch (_: RuntimeException) {
            false
        }
    }
}
