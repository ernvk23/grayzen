# Grayzen

Applies Android Color correction based on the foreground app through an AccessibilityService.

Inspired by Graycie, this branch streamlines a stripped-down version with a custom feature set: configurable foreground timing and per-app daily allowances.

## Build

Host builds need JDK 25, Gradle 9.8.1, SDK 37, and `sdk.dir` or `ANDROID_HOME`.

```sh
make build
make install
```

Docker needs only Docker. It uses the host `adb` for install:

```sh
make docker-build
make docker-install
```

`make build` and `make docker-build` write `dist/grayzen-debug.apk`. `make install` and `make docker-install` install that APK and grant secure settings. `make release` or `make docker-release` writes `dist/grayzen-release.apk`. `make help` lists everything.

## Setup

Enable **Grayzen** in Accessibility settings after installing. Usage Access is optional, for timed grayscale.

For a one-command dev setup, run `make dev-install` or `make docker-dev-install`; they also enable Usage Access and the accessibility service.

## Signing

Debug builds are auto-signed. Release builds are unsigned unless `keystore.properties` exists.

```sh
make keystore
cp keystore.properties.example keystore.properties
```

Fill in the passwords, then run `make release` or `make docker-release`. To sign an existing unsigned APK later, use `apksigner` from the Android SDK build-tools:

```sh
apksigner sign --ks release.keystore --ks-key-alias grayzen \
  --out dist/grayzen-signed.apk dist/grayzen-release.apk
```

A self-signed key is enough for sideloading. Play and F-Droid handle store signing separately. Never commit the keystore or properties.

## Credit

Based on [Graycie](https://github.com/abhigyantrips/Graycie) by Abhigyan Trips, GPL-3.0-or-later. Fork by [ernvk23](https://github.com/ernvk23). See [NOTICE](NOTICE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
