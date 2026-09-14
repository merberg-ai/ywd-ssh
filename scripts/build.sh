#!/usr/bin/env bash
set -Eeuo pipefail

REPO_URL="https://github.com/merberg-ai/ywd-ssh.git"
BRANCH="${YWD_SSH_BRANCH:-main}"
SRC_ROOT="${YWD_SRC_ROOT:-$HOME/src}"
APP_DIR="${YWD_SSH_DIR:-$SRC_ROOT/ywd-ssh}"

say() { printf '\n[YWD-SSH] %s\n' "$*"; }

mkdir -p "$SRC_ROOT"

if [[ -d "$APP_DIR/.git" ]]; then
    say "Updating $APP_DIR"
    if ! git -C "$APP_DIR" diff --quiet || ! git -C "$APP_DIR" diff --cached --quiet; then
        echo "ERROR: $APP_DIR has local tracked changes. Commit/stash them before running the updater." >&2
        exit 2
    fi
    git -C "$APP_DIR" fetch --prune origin
    git -C "$APP_DIR" checkout "$BRANCH"
    git -C "$APP_DIR" pull --ff-only origin "$BRANCH"
elif [[ -e "$APP_DIR" ]]; then
    echo "ERROR: $APP_DIR exists but is not a git checkout." >&2
    exit 2
else
    say "Cloning YWD-SSH into $APP_DIR"
    git clone --branch "$BRANCH" "$REPO_URL" "$APP_DIR"
fi

say "Preparing Android build environment"
"$APP_DIR/scripts/setup.sh"

say "Building APK"
"$APP_DIR/scripts/build-local.sh"

say "Done"
printf 'APK: %s\n' "$APP_DIR/dist/ywd-ssh-debug.apk"
