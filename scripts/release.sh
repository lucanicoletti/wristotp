#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
dist="$root/dist"
version="$(sed -n 's/.*versionName = "\(.*\)"/\1/p' "$root/companion/composeApp/build.gradle.kts")"
sdk="${CONNECTIQ_SDK:-$(cat "$HOME/Library/Application Support/Garmin/ConnectIQ/current-sdk.cfg" 2>/dev/null || true)}"

if [[ -z "${DEVELOPER_KEY:-}" || ! -f "$DEVELOPER_KEY" ]]; then
    echo "Set DEVELOPER_KEY to your Connect IQ developer key (.der)." >&2
    exit 1
fi
if [[ ! -x "$sdk/bin/monkeyc" ]]; then
    echo "Connect IQ SDK not found. Set CONNECTIQ_SDK to the SDK folder." >&2
    exit 1
fi
if [[ ! -f "$root/companion/keystore.properties" ]]; then
    echo "companion/keystore.properties is missing, so the APK would be unsigned." >&2
    exit 1
fi

rm -rf "$dist"
mkdir -p "$dist"

(cd "$root/companion" && ./gradlew --quiet :composeApp:assembleRelease)
cp "$root/companion/composeApp/build/outputs/apk/release/composeApp-release.apk" "$dist/wristotp-$version.apk"

build="$(mktemp -d)"
trap 'rm -rf "$build"' EXIT
for device in $(sed -n 's/.*<iq:product id="\([^"]*\)".*/\1/p' "$root/watch/manifest.xml"); do
    echo "Building watch app for $device"
    (cd "$root/watch" && "$sdk/bin/monkeyc" -f monkey.jungle -d "$device" -r -l 2 -y "$DEVELOPER_KEY" -o "$build/wristotp-$version-$device.prg")
done
cp "$build"/*.prg "$dist"

(cd "$dist" && shasum -a 256 -- * > SHA256SUMS)
echo "Release files are in $dist"
