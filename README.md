# Grayzen

Applies Android Color correction based on the foreground app through an AccessibilityService.

Inspired by Graycie, this branch streamlines a stripped-down version with a custom feature set: configurable foreground timing and per-app daily allowances.

**Build** (JDK 25, Gradle 9.8.1, SDK 37; target 17; set `sdk.dir` or `ANDROID_HOME`):

```sh
gradle assembleDebug
gradle installDebug
```

**Setup**: grant secure settings once, then enable **Grayzen** in Accessibility settings.

```sh
adb shell pm grant com.ernvk23.grayzen android.permission.WRITE_SECURE_SETTINGS
```

Usage Access is optional, for timed grayscale.

**Credit**: based on [Graycie](https://github.com/abhigyantrips/Graycie) by Abhigyan Trips, GPL-3.0-or-later. Fork by [ernvk23](https://github.com/ernvk23). See [NOTICE](NOTICE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
