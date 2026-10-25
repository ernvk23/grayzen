package io.github.ernvk23.grayzen

enum class ManagerStatus { DISABLED, ACTIVE, SNOOZING, SNOOZED, RESUMING }

interface SnoozeNotifications {
    fun isReady(): Boolean
    fun post(packageName: String, reenableManager: Boolean): Boolean
    fun cancel()
}

interface SnoozeAppLauncher {
    fun launch(packageName: String): Boolean
}

/** Keeps snooze ordering around injected Android/provider boundaries. */
class SnoozeCoordinator(
    private val engine: PolicyEngine,
    private val notifications: SnoozeNotifications,
    private val accessibility: AccessibilityComponentControl,
    private val launcher: SnoozeAppLauncher,
    private val phaseChanged: (ManagerStatus) -> Unit,
) {
    fun automatic(packageName: String, disableSelf: () -> Unit): Boolean {
        if (!engine.state.isAutoSnoozeCandidate(packageName)) return false
        if (!notifications.isReady()) {
            engine.reportError(NOTIFICATION_ERROR)
            return false
        }
        phaseChanged(ManagerStatus.SNOOZING)
        if (!engine.prepareSnooze(packageName)) return finishSnoozed(false)
        // Recovery must exist before the service removes its own access.
        if (!postRecovery(packageName)) {
            engine.abortSnooze(NOTIFICATION_ERROR)
            phaseChanged(ManagerStatus.ACTIVE)
            return false
        }
        engine.restoreColorForSnooze()
        disableSelf()
        return finishSnoozed(true)
    }

    fun openSafely(packageName: String): Boolean {
        if (packageName !in engine.state.snoozePackages ||
            engine.state.snoozedForPackage != null) return false
        if (!notifications.isReady()) {
            engine.reportError(NOTIFICATION_ERROR)
            return false
        }
        phaseChanged(ManagerStatus.SNOOZING)
        if (!engine.prepareSnooze(packageName)) return finishSnoozed(false)
        if (!postRecovery(packageName)) {
            engine.abortSnooze(NOTIFICATION_ERROR)
            phaseChanged(ManagerStatus.ACTIVE)
            return false
        }
        engine.restoreColorForSnooze()
        return try {
            val accepted = accessibility.disableOwnService()
            val stillEnabled = accessibility.isOwnServiceEnabled()
            if (!accepted || stillEnabled) {
                val message = "Couldn’t pause accessibility; app not opened."
                if (stillEnabled) {
                    engine.abortSnooze(message)
                    notifications.cancel()
                    phaseChanged(ManagerStatus.ACTIVE)
                    false
                } else {
                    engine.reportError(message)
                    finishSnoozed(false)
                }
            } else if (!launcher.launch(packageName)) {
                engine.reportError("The app has no launcher activity and could not be opened.")
                finishSnoozed(false)
            } else finishSnoozed(true)
        } catch (_: SecurityException) {
            failSafeLaunch("Missing secure settings; app not opened.")
        } catch (_: RuntimeException) {
            failSafeLaunch("Couldn’t pause accessibility; app not opened.")
        }
    }

    /** Starts re-enable; snooze is cleared only by [onServiceConnected]. */
    fun resume(): Boolean {
        if (engine.state.snoozedForPackage == null) return false
        phaseChanged(ManagerStatus.RESUMING)
        return try {
            if (!accessibility.enableOwnService()) {
                resumeFailed("Android refused to re-enable accessibility.")
            } else true
        } catch (_: SecurityException) {
            resumeFailed("Missing secure settings. Re-enable the service in Accessibility.")
        } catch (_: RuntimeException) {
            resumeFailed("Couldn’t re-enable accessibility. Check Accessibility settings.")
        }
    }

    fun resumeTimedOut() {
        if (engine.state.snoozedForPackage != null) {
            engine.reportError("Accessibility did not reconnect. Try again.")
            phaseChanged(ManagerStatus.SNOOZED)
        }
    }

    fun onServiceConnected(): Boolean {
        if (engine.state.snoozedForPackage == null) return false
        engine.completeResume()
        notifications.cancel()
        phaseChanged(ManagerStatus.ACTIVE)
        return true
    }

    /** Defensive path for non-UI callers; the Home switch is locked during recovery. */
    fun cancelByTurningOff() {
        engine.setEnabled(false)
        notifications.cancel()
        phaseChanged(ManagerStatus.DISABLED)
    }

    fun repostRecovery(): Boolean = engine.state.snoozedForPackage?.let(::postRecovery) ?: false

    private fun postRecovery(packageName: String): Boolean = runCatching {
        notifications.post(packageName, reenableManager = engine.state.managerEnabled)
    }.getOrDefault(false)

    private fun resumeFailed(message: String): Boolean {
        engine.reportError(message)
        phaseChanged(ManagerStatus.SNOOZED)
        return false
    }

    private fun failSafeLaunch(message: String): Boolean {
        val stillEnabled = runCatching { accessibility.isOwnServiceEnabled() }.getOrDefault(true)
        if (stillEnabled) {
            engine.abortSnooze(message)
            notifications.cancel()
            phaseChanged(ManagerStatus.ACTIVE)
            return false
        }
        engine.reportError(message)
        return finishSnoozed(false)
    }

    private fun finishSnoozed(result: Boolean): Boolean {
        phaseChanged(ManagerStatus.SNOOZED)
        return result
    }

    companion object {
        const val NOTIFICATION_ERROR =
            "Notifications are off. Turn them on to use Auto-snooze."
    }
}
