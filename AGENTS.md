# Grayzen

Native Android app (Kotlin, Compose) that applies Android Color correction from the settled foreground app via an AccessibilityService.

## Build

- Gradle 9.8.1 on JDK 25; Java/Kotlin target 17.
- `gradle lint testDebugUnitTest assembleDebug assembleRelease assembleDebugAndroidTest`
- Device: `gradle connectedDebugAndroidTest`
- If Gradle home is read-only, set `GRADLE_USER_HOME` and `-Djava.io.tmpdir` to writable paths.

## Rules

- No commits, pushes, or PRs unless asked.
- Never commit keystores, `keystore.properties`, or signing secrets.
- One delayed deadline: sample usage on entry and at the single deadline. No polling, ticks, ledger, parallel queries, or retry timer.
- Secure-setting writes on the main thread; usage queries on the single worker.
- Package-only foreground detection: no screen content, gestures, overlays, or foreground service.
- No new dependencies, DI, navigation, database, accounts, or network.

## Tests

- Add tests only for vital behavior: a required outcome or a concrete regression worth preventing. Identify the failure each new test would catch; omit tests without one.
- Use the smallest set of meaningful cases. Extend existing coverage where possible; skip new tests when existing tests already protect the behavior or the change has no meaningful behavioral risk.
- Assert observable outcomes against requirements. Avoid filler, duplicate cases, implementation-mirroring assertions, and mocks that merely confirm their own setup.
- Regression tests must catch the original bug. Derive expectations from intended behavior, not current output; never weaken valid assertions just to make tests green.
- Run relevant existing checks even when no new tests are needed.

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
