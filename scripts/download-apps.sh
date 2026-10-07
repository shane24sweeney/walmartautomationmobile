#!/usr/bin/env bash
# Downloads the app builds under test into apps/. They are too large to commit (the APK is over
# GitHub's 100 MB limit), so every checkout fetches the same pinned release instead.
set -euo pipefail

VERSION="v2.2.0"
BASE="https://github.com/webdriverio/native-demo-app/releases/download/${VERSION}"
DIR="$(cd "$(dirname "$0")/.." && pwd)/apps"
mkdir -p "$DIR"

if [[ ! -f "$DIR/android.apk" ]]; then
  echo "Downloading Android app ${VERSION}..."
  curl -fsSL -o "$DIR/android.apk" "${BASE}/android.wdio.native.app.${VERSION}.apk"
fi

# The iOS simulator build is only needed (and only unzippable as an .app) on macOS.
if [[ "$(uname)" == "Darwin" && ! -d "$DIR/ios.app" ]]; then
  echo "Downloading iOS simulator app ${VERSION}..."
  tmp="$(mktemp -d)"
  curl -fsSL -o "$tmp/ios.zip" "${BASE}/ios.simulator.wdio.native.app.${VERSION}.zip"
  unzip -q "$tmp/ios.zip" -d "$tmp"
  mv "$(find "$tmp" -maxdepth 2 -name '*.app' -type d | head -1)" "$DIR/ios.app"
  rm -rf "$tmp"
fi
