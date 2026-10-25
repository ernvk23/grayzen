GRADLE ?= gradle
ADB ?= adb$(if $(ANDROID_SERIAL), -s $(ANDROID_SERIAL),)
COMPOSE ?= docker compose
APP_ID ?= io.github.ernvk23.grayzen
KEYSTORE ?= release.keystore
KEY_ALIAS ?= grayzen
ACCESSIBILITY_COMPONENT ?= $(APP_ID)/$(APP_ID).ForegroundAccessibilityService

ANDROID_SERIAL ?=
ANDROID_USER_HOME ?= $(CURDIR)/.android
DOCKER_USER := $(shell id -u):$(shell id -g)
ifneq ($(ANDROID_SERIAL),)
export ANDROID_SERIAL
endif
export ANDROID_USER_HOME

.PHONY: help build install dev-install release test clean uninstall keystore docker-build docker-install docker-dev-install docker-release check-sdk check-adb

help:
	@echo "Grayzen"
	@echo "  make build           Host debug build -> dist/grayzen-debug.apk"
	@echo "  make install         Host build, install, and grant secure settings"
	@echo "  make dev-install     Host build, install, and enable all permissions"
	@echo "  make release         Host release build -> dist/grayzen-release.apk"
	@echo "  make test            Run lint and unit tests"
	@echo "  make clean           Remove dist and build outputs"
	@echo "  make uninstall       Remove Grayzen from the device"
	@echo "  make keystore        Create a local release keystore"
	@echo "  make docker-build    Docker debug build -> dist/grayzen-debug.apk"
	@echo "  make docker-install  Docker build, install, and grant"
	@echo "  make docker-dev-install  Docker build, install, and enable all permissions"
	@echo "  make docker-release  Docker release build -> dist/grayzen-release.apk"
	@echo ""
	@echo "Host builds need JDK 25, Gradle 9.8.1, and SDK 37."
	@echo "Docker builds need only Docker. APKs are in dist/."
	@echo "Set ANDROID_SERIAL=SERIAL when more than one device is connected."

check-sdk:
	@if [ -z "$$ANDROID_HOME" ] && [ ! -f local.properties ]; then \
		echo "Set ANDROID_HOME or sdk.dir in local.properties."; exit 1; \
	fi

check-adb:
	@$(ADB) get-state >/dev/null 2>&1 || { \
		echo "No device. Connect one or set ANDROID_SERIAL=SERIAL."; exit 1; \
	}

build: check-sdk
	$(GRADLE) assembleDebug
	mkdir -p dist
	cp app/build/outputs/apk/debug/app-debug.apk dist/grayzen-debug.apk

install: check-adb build
	$(ADB) install -r dist/grayzen-debug.apk
	$(ADB) shell pm grant $(APP_ID) android.permission.WRITE_SECURE_SETTINGS

dev-install: check-adb build
	ADB="$(ADB)" APP_ID="$(APP_ID)" ACCESSIBILITY_COMPONENT="$(ACCESSIBILITY_COMPONENT)" \
		scripts/dev-install.sh dist/grayzen-debug.apk

release: check-sdk
	$(GRADLE) assembleRelease
	mkdir -p dist
	cp app/build/outputs/apk/release/*.apk dist/grayzen-release.apk

test: check-sdk
	$(GRADLE) lint testDebugUnitTest

uninstall: check-adb
	$(ADB) uninstall $(APP_ID)

keystore:
	@test ! -f $(KEYSTORE) || { echo "$(KEYSTORE) already exists"; exit 1; }
	keytool -genkeypair -v -keystore $(KEYSTORE) -alias $(KEY_ALIAS) -keyalg RSA -keysize 4096 -validity 10000
	@echo "Copy keystore.properties.example to keystore.properties and fill in the passwords."

docker-build:
	mkdir -p dist
	$(COMPOSE) run --rm --build --user $(DOCKER_USER) build assembleDebug
	cp app/build/outputs/apk/debug/app-debug.apk dist/grayzen-debug.apk

docker-install: check-adb docker-build
	$(ADB) install -r dist/grayzen-debug.apk
	$(ADB) shell pm grant $(APP_ID) android.permission.WRITE_SECURE_SETTINGS

docker-dev-install: check-adb docker-build
	ADB="$(ADB)" APP_ID="$(APP_ID)" ACCESSIBILITY_COMPONENT="$(ACCESSIBILITY_COMPONENT)" \
		scripts/dev-install.sh dist/grayzen-debug.apk

docker-release:
	mkdir -p dist
	$(COMPOSE) run --rm --build --user $(DOCKER_USER) build assembleRelease
	cp app/build/outputs/apk/release/*.apk dist/grayzen-release.apk

clean:
	rm -rf dist build app/build .gradle .kotlin .docker-gradle
