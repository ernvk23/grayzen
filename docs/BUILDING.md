# Building

## Requirements

- JDK 25
- Gradle 9.8.1
- Android SDK 37, configured through `sdk.dir` in `local.properties` or `ANDROID_HOME`
- `adb` and a device for install targets
- Docker (optional, only for `make docker-*`)

## Host builds

```sh
make build        # dist/grayzen-debug.apk
make install      # build, install, grant WRITE_SECURE_SETTINGS
make dev-install  # build, install, grant, enable Accessibility and Usage Access
make release      # dist/grayzen-release.apk (requires keystore.properties)
make test         # lint + unit tests
```

> [!TIP]
> `make help` lists every target. Set `ANDROID_SERIAL=SERIAL` when more than one device is connected.

## Docker builds

Docker builds need only Docker. They use the host `adb` for install targets.

```sh
make docker-build
make docker-install
make docker-dev-install
make docker-release
```

APKs are written to `dist/`.

## Signing

Debug builds are auto-signed. Release builds are unsigned unless `keystore.properties` exists in the repository root.

```sh
make keystore
cp keystore.properties.example keystore.properties
```

Fill in the passwords, then run `make release` or `make docker-release`.

> [!WARNING]
> Back up `release.keystore` and its passwords. Losing them prevents future APKs from updating installed copies. Never commit the keystore or `keystore.properties`; both are gitignored.

To sign an existing unsigned APK:

```sh
apksigner sign --ks release.keystore --ks-key-alias grayzen \
  --out dist/grayzen-signed.apk dist/grayzen-release.apk
```

A self-signed key is enough for sideloading. Play and F-Droid handle store signing separately.

## Releases

Pushing a `vMAJOR.MINOR.PATCH` tag runs [release.yml](../.github/workflows/release.yml). The workflow runs lint and unit tests, builds and verifies the signed APK, then publishes a GitHub release with `grayzen-<version>.apk`, the commit SHA, the APK SHA-256, and commits since the previous tag.

Add these repository secrets once:

```sh
gh secret set KEYSTORE_BASE64 < <(base64 -w0 release.keystore)
gh secret set KEYSTORE_PASSWORD
gh secret set KEY_ALIAS --body grayzen
gh secret set KEY_PASSWORD
```

Then tag a release:

```sh
git tag v1.0.0
git push origin v1.0.0
```

The tag sets `versionName` to `1.0.0` and derives `versionCode` as `major*10000 + minor*100 + patch` (`v1.0.0` → `10000`).

> [!NOTE]
> Local builds use the defaults in `app/build.gradle.kts` (`1.0.0` / `10000`). CI overrides both from the tag.
