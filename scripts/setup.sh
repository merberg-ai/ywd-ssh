#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SRC_ROOT="${YWD_SRC_ROOT:-$HOME/src}"
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$SRC_ROOT/android-sdk}"
CMDLINE_VERSION="15859902"
CMDLINE_SHA256="4e4c464f145a7512b57d088ac6c278c03c9eea610886b35a5e0804e74eedf583"
CMDLINE_URL="https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_VERSION}_latest.zip"

say() { printf '\n[YWD-SSH] %s\n' "$*"; }

need_apt=0
for cmd in git curl unzip java javac; do
    command -v "$cmd" >/dev/null 2>&1 || need_apt=1
done

if (( need_apt )); then
    say "Installing host build prerequisites"
    sudo apt-get update
    sudo DEBIAN_FRONTEND=noninteractive apt-get install -y \
        git curl unzip ca-certificates openjdk-17-jdk
fi

java_major="$(java -version 2>&1 | awk -F'[\".]' '/version/ {print $2; exit}')"
if [[ -z "$java_major" || "$java_major" -lt 17 ]]; then
    say "Java 17+ is required; installing OpenJDK 17"
    sudo apt-get update
    sudo DEBIAN_FRONTEND=noninteractive apt-get install -y openjdk-17-jdk
fi

mkdir -p "$SRC_ROOT" "$ANDROID_SDK_ROOT/cmdline-tools"

if [[ ! -x "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]]; then
    say "Installing Android command-line tools"
    tmpdir="$(mktemp -d)"
    trap 'rm -rf "$tmpdir"' EXIT
    curl -fL --retry 3 -o "$tmpdir/cmdline-tools.zip" "$CMDLINE_URL"
    printf '%s  %s\n' "$CMDLINE_SHA256" "$tmpdir/cmdline-tools.zip" | sha256sum -c -
    unzip -q "$tmpdir/cmdline-tools.zip" -d "$tmpdir/unpacked"
    rm -rf "$ANDROID_SDK_ROOT/cmdline-tools/latest"
    mv "$tmpdir/unpacked/cmdline-tools" "$ANDROID_SDK_ROOT/cmdline-tools/latest"
fi

export ANDROID_SDK_ROOT
export ANDROID_HOME="$ANDROID_SDK_ROOT"
export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"

say "Accepting Android SDK licenses"
yes | sdkmanager --licenses >/dev/null 2>&1 || true

say "Installing Android SDK packages"
sdkmanager \
    "platform-tools" \
    "platforms;android-36" \
    "build-tools;35.0.0"

cat > "$ROOT_DIR/local.properties" <<EOF
sdk.dir=$ANDROID_SDK_ROOT
EOF

say "Android build environment is ready"
printf 'SDK:  %s\nJava: %s\n' "$ANDROID_SDK_ROOT" "$(java -version 2>&1 | head -n1)"
