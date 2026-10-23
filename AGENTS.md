# Grayzen

Native Android app (Kotlin, Compose) that applies Android Color correction from the settled foreground app via an AccessibilityService.

## Build

- Gradle 9.8.1 on JDK 25; Java/Kotlin target 17.
- `gradle lint testDebugUnitTest assembleDebug assembleRelease assembleDebugAndroidTest`
- Device: `gradle connectedDebugAndroidTest`
- If Gradle home is read-only, set `GRADLE_USER_HOME` and `-Djava.io.tmpdir` to writable paths.

## Rules

- No commits, pushes, or PRs unless asked.
- One delayed deadline: sample usage on entry and at the single deadline. No polling, ticks, ledger, parallel queries, or retry timer.
- Secure-setting writes on the main thread; usage queries on the single worker.
- Package-only foreground detection: no screen content, gestures, overlays, or foreground service.
- No new dependencies, DI, navigation, database, accounts, or network.

## Map

- `ManagerController` / `ManagerViewModel`: preferences and state.
- `PolicyEngine` / `SecureGrayscaleSettings` / `UsageTracking`: policy, provider, usage.
- `ForegroundAccessibilityService` / `ForegroundWindowClassifier`: window classification.
- `SnoozeCoordinator` / `SecureAccessibilitySettings` / `SnoozeNotificationManager`: snooze and recovery.
- `ui/ManagerScreen.kt`: Home, Grayscale Control, Auto-Snooze, Setup.
- Namespace and application ID: `com.ernvk23.grayzen`.

## Device

- Grant `WRITE_SECURE_SETTINGS` with ADB; enable the accessibility service.
- Check grayscale/color switching, timers, snooze/resume, reboot recovery, uninstall cleanup.
- Turn Grayzen off before uninstalling to restore the previous Color correction.
