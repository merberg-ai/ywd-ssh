#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GRADLE_VERSION="8.13"
CACHE_ROOT="${XDG_CACHE_HOME:-$HOME/.cache}/ywd-ssh"
GRADLE_HOME="$CACHE_ROOT/gradle-$GRADLE_VERSION"
GRADLE_ZIP="$CACHE_ROOT/gradle-$GRADLE_VERSION-bin.zip"

say() { printf '\n[YWD-SSH] %s\n' "$*"; }

if [[ ! -x "$GRADLE_HOME/bin/gradle" ]]; then
    say "Installing Gradle $GRADLE_VERSION into the local cache"
    mkdir -p "$CACHE_ROOT"
    curl -fL --retry 3 \
        -o "$GRADLE_ZIP" \
        "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
    rm -rf "$GRADLE_HOME"
    unzip -q "$GRADLE_ZIP" -d "$CACHE_ROOT"
fi

if [[ ! -f "$ROOT_DIR/local.properties" ]]; then
    say "local.properties is missing; running Android environment setup"
    "$ROOT_DIR/scripts/setup.sh"
fi

say "Building YWD-SSH debug APK"
cd "$ROOT_DIR"
"$GRADLE_HOME/bin/gradle" --no-daemon --stacktrace :app:assembleDebug

mkdir -p "$ROOT_DIR/dist"
cp -f "$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk" \
    "$ROOT_DIR/dist/ywd-ssh-debug.apk"

say "Build complete"
ls -lh "$ROOT_DIR/dist/ywd-ssh-debug.apk"
sha256sum "$ROOT_DIR/dist/ywd-ssh-debug.apk"
