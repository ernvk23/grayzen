#!/bin/sh
set -eu

apk=${1:?APK path required}
app_id=${APP_ID:?APP_ID required}
component=${ACCESSIBILITY_COMPONENT:?ACCESSIBILITY_COMPONENT required}
adb_cmd=${ADB:-adb}

test -f "$apk" || { echo "APK not found: $apk"; exit 1; }

$adb_cmd install -r "$apk"
$adb_cmd shell pm grant "$app_id" android.permission.WRITE_SECURE_SETTINGS
$adb_cmd shell appops set "$app_id" android:get_usage_stats allow

enabled=$($adb_cmd shell settings get secure enabled_accessibility_services | tr -d '\r')
case ":$enabled:" in
    *":$component:"*) ;;
    *)
        if [ -z "$enabled" ] || [ "$enabled" = "null" ]; then
            next=$component
        else
            next="$enabled:$component"
        fi
        $adb_cmd shell settings put secure enabled_accessibility_services "$next"
        ;;
esac
$adb_cmd shell settings put secure accessibility_enabled 1

echo "Installed and enabled secure settings, Usage Access, and Accessibility."
