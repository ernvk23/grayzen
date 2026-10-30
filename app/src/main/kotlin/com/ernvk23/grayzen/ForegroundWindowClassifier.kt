package io.github.ernvk23.grayzen

data class WindowMetadata(
    val packageName: String?,
    val focused: Boolean,
    val active: Boolean,
    val type: Int,
    val pictureInPicture: Boolean = false,
    val hasRoot: Boolean = true,
)

sealed interface ForegroundClassification {
    val reason: String
    data class Settled(val packageName: String, override val reason: String) : ForegroundClassification
    data class Temporary(override val reason: String) : ForegroundClassification
    data class Overlay(override val reason: String) : ForegroundClassification
    data class NonLaunchable(val packageName: String?, override val reason: String) : ForegroundClassification
}

class ForegroundWindowClassifier {
    fun isOwnWindowFocused(windows: List<WindowMetadata>, ownPackage: String): Boolean =
        windows.any { it.focused && it.packageName == ownPackage && !it.pictureInPicture }

    fun classify(
        windows: List<WindowMetadata>,
        launchablePackages: Set<String>,
        retryExpired: Boolean = false,
    ): ForegroundClassification {
        if (windows.isEmpty()) return ForegroundClassification.Temporary("no interactive windows")
        val focused = windows.firstOrNull { it.focused } ?: windows.firstOrNull { it.active }
            ?: return ForegroundClassification.Temporary("no focused or active window")
        if (!focused.hasRoot || focused.packageName == null) {
            return ForegroundClassification.Temporary("focused window root unavailable")
        }
        val applicationWindows = windows.filter {
            it.type == WINDOW_TYPE_APPLICATION && it.hasRoot && it.packageName != null && !it.pictureInPicture
        }
        val hasDivider = windows.any { it.type == WINDOW_TYPE_SPLIT_SCREEN_DIVIDER }
        val fullScreenLayered = !focused.pictureInPicture &&
            applicationWindows.mapNotNull { it.packageName }.distinct().size > 1
        if (fullScreenLayered && !hasDivider) {
            return if (retryExpired) ForegroundClassification.Overlay("application layer persisted")
            else ForegroundClassification.Temporary("destination is layered with another application")
        }
        val candidate = focused.packageName
        if (candidate !in launchablePackages) {
            return ForegroundClassification.NonLaunchable(candidate, "focused package is not launchable")
        }
        return ForegroundClassification.Settled(
            candidate,
            if (hasDivider) "focused split-screen application" else "focused application settled",
        )
    }

    companion object {
        const val WINDOW_TYPE_APPLICATION = 1
        const val WINDOW_TYPE_SYSTEM = 3
        const val WINDOW_TYPE_SPLIT_SCREEN_DIVIDER = 5
    }
}
