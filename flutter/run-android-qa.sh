#!/usr/bin/env bash
set -euo pipefail

usage() {
    cat <<'USAGE'
Usage: ./run-android-qa.sh -d DEVICE -t integration_test/TEST.dart -f DEFINES.json [--enable-push]

Builds and verifies the isolated .qa APK before running a Flutter integration
test. The defines file can contain fixture credentials; its contents are never
printed. Push stays disabled unless --enable-push is explicitly selected.
Run only one Flutter build/test in this checkout at a time.
USAGE
}

device_id=""
test_file=""
defines_file=""
enable_push=0
while (($#)); do
    case "$1" in
        -d|--device)
            (($# >= 2)) || { usage >&2; exit 2; }
            device_id="$2"
            shift 2
            ;;
        -t|--test)
            (($# >= 2)) || { usage >&2; exit 2; }
            test_file="$2"
            shift 2
            ;;
        -f|--defines-file)
            (($# >= 2)) || { usage >&2; exit 2; }
            defines_file="$2"
            shift 2
            ;;
        --enable-push)
            enable_push=1
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            printf 'Unknown argument: %s\n' "$1" >&2
            usage >&2
            exit 2
            ;;
    esac
done

if [[ -z "$device_id" || -z "$test_file" || -z "$defines_file" ]]; then
    usage >&2
    exit 2
fi

app_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$app_dir"
if [[ "$test_file" != integration_test/*.dart || ! -f "$test_file" ]]; then
    printf 'Select an existing integration_test/*.dart file.\n' >&2
    exit 2
fi
if [[ ! -f "$defines_file" ]]; then
    printf 'Defines file was not found.\n' >&2
    exit 2
fi

# Reject contradictory settings before touching the device. Keep secrets in
# the defines file rather than spelling tokens on the command line.
python3 - "$defines_file" "$enable_push" <<'PY'
import json
import sys

with open(sys.argv[1], encoding='utf-8') as source:
    values = json.load(source)
if not isinstance(values, dict):
    raise SystemExit('The defines file must contain a JSON object.')

def enabled(name):
    value = values.get(name)
    if value is None:
        return None
    return str(value).lower() == 'true'

if enabled('QA_INTEGRATION') is False:
    raise SystemExit('QA_INTEGRATION must not be false in the defines file.')
requested_push = sys.argv[2] == '1'
if enabled('PUSH_ENABLED') not in (None, requested_push):
    raise SystemExit('PUSH_ENABLED conflicts with --enable-push.')
PY

if [[ "$(adb -s "$device_id" get-state 2>/dev/null)" != "device" ]]; then
    printf 'Android device is unavailable: %s\n' "$device_id" >&2
    exit 1
fi

sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
aapt=""
for candidate in "$sdk_root"/build-tools/*/aapt; do
    if [[ -x "$candidate" ]]; then aapt="$candidate"; fi
done
if [[ -z "$aapt" ]]; then
    printf 'Android SDK aapt was not found under %s/build-tools.\n' "$sdk_root" >&2
    exit 1
fi

normal_package='com.ride.swift_app'
qa_package='com.ride.swift_app.qa'
normal_first_install=""
if adb -s "$device_id" shell pm path "$normal_package" | grep -q '^package:'; then
    normal_first_install="$(adb -s "$device_id" shell dumpsys package "$normal_package" |
        awk -F= '/^[[:space:]]*firstInstallTime=/{if (value == "") value = $2} END {print value}')"
fi

push_define='--dart-define=PUSH_ENABLED=false'
if ((enable_push)); then push_define='--dart-define=PUSH_ENABLED=true'; fi
defines=("--dart-define-from-file=$defines_file" '--dart-define=QA_INTEGRATION=true' "$push_define")

# Flutter's integration test device reads an existing app-debug.apk before it
# rebuilds the test entrypoint, then uninstalls that captured package on exit.
# Build this variant first so its cleanup can target only the .qa package.
SWIFTAPP_QA_BUILD=1 flutter build apk --debug "${defines[@]}"
apk='build/app/outputs/flutter-apk/app-debug.apk'
apk_package="$("$aapt" dump badging "$apk" | awk -F "'" '/^package: name=/{print $2}')"
if [[ "$apk_package" != "$qa_package" ]]; then
    printf 'Refusing to run: APK package is %s (expected %s).\n' "$apk_package" "$qa_package" >&2
    exit 1
fi

printf 'Verified isolated Android package: %s\n' "$qa_package"
test_status=0
SWIFTAPP_QA_BUILD=1 flutter test "$test_file" -d "$device_id" "${defines[@]}" || test_status=$?

# Do not silently reinstall a removed normal app: that would hide lost account
# data. Fail loudly if the installed normal package changed during the test.
if [[ -n "$normal_first_install" ]]; then
    if ! adb -s "$device_id" shell pm path "$normal_package" | grep -q '^package:'; then
        printf 'ERROR: The normal app package disappeared during QA; its data may be lost.\n' >&2
        exit 1
    fi
    after_first_install="$(adb -s "$device_id" shell dumpsys package "$normal_package" |
        awk -F= '/^[[:space:]]*firstInstallTime=/{if (value == "") value = $2} END {print value}')"
    if [[ "$after_first_install" != "$normal_first_install" ]]; then
        printf 'ERROR: The normal app was reinstalled during QA; inspect its local session.\n' >&2
        exit 1
    fi
    printf 'Normal app package and install time are unchanged.\n'
fi

exit "$test_status"
