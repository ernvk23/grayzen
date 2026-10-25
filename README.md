# Grayscale per app

Turns the screen gray while selected apps are open, then restores color when you leave them. Per-app daily limits and auto-snooze are available.

It uses Android's accessibility service only to detect which app is in front. It never reads what is on screen, and it has no internet access.

> [!IMPORTANT]
> Android does not let you grant this permission from Settings. Grant it once from a computer with ADB:
>
> ```sh
> adb shell pm grant com.ernvk23.grayzen android.permission.WRITE_SECURE_SETTINGS
> ```

## Install

1. Download the latest `grayzen-*.apk` from [Releases](https://github.com/ernvk23/grayzen/releases).
2. Install it on the phone.
3. Run the ADB command above.
4. Open the app and complete setup. When asked, enable its accessibility service in Settings → Accessibility.
5. Optional: allow Usage Access if you want daily time limits.

> [!NOTE]
> Requires Android 8.0 or newer.

> [!WARNING]
> Turn the app off before uninstalling, otherwise the screen may stay gray.

## Build

For source builds, see [docs/BUILDING.md](docs/BUILDING.md). Host builds need JDK 25, Gradle 9.8.1, and Android SDK 37; Docker builds need only Docker.

```sh
make build
make docker-build
```

## Credits

Based on [Graycie](https://github.com/abhigyantrips/Graycie), GPL-3.0-or-later. See [third-party notices](THIRD_PARTY_NOTICES.md).
