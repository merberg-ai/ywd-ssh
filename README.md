# YWD-SSH

[![Android CI](https://github.com/merberg-ai/ywd-ssh/actions/workflows/android.yml/badge.svg)](https://github.com/merberg-ai/ywd-ssh/actions/workflows/android.yml)

A small, fast Android SSH client with a retro/cyber YWD interface.

The goal is deliberately simple: saved hosts, secure SSH connections, and a good terminal without accounts, subscriptions, telemetry, ads, or cloud dependencies.

> **Status:** `0.0.1-dev` — first physical-test build. Password SSH, host-key verification, saved host profiles, and the interactive terminal are implemented.

## One-line Ubuntu build

From an Ubuntu machine, including a fresh development host:

```bash
curl -fsSL https://raw.githubusercontent.com/merberg-ai/ywd-ssh/main/scripts/build.sh | bash
```

The bootstrap script:

- clones or updates the project at `~/src/ywd-ssh`
- installs/checks Java 17 and basic host prerequisites
- installs the Android command-line SDK under `~/src/android-sdk`
- caches Gradle under `~/.cache/ywd-ssh`
- installs the required Android SDK platform/build tools
- builds the debug APK
- prints its SHA-256 checksum

The APK is copied to:

```text
~/src/ywd-ssh/dist/ywd-ssh-debug.apk
```

The script refuses to overwrite tracked local Git changes.

## Local rebuild

Once the machine is bootstrapped:

```bash
cd ~/src/ywd-ssh
./scripts/build-local.sh
```

## 0.0.1-dev functionality

- Android native app: Kotlin + Jetpack Compose
- ConnectBot `cbssh` for SSH transport
- ConnectBot `termlib` / libvterm for terminal emulation
- saved host profiles: nickname, hostname/IP, port, username
- passwords requested per connection and **never persisted**
- first-use SHA-256 host-key fingerprint approval
- persistent host-key trust with explicit changed-key warning
- password authentication
- interactive `xterm-256color` PTY
- terminal resize propagation to the SSH server
- scrollback, selection, clipboard paste, and terminal hyperlink handling
- YWD dark/cyan/magenta styling
- mobile extra-key strip: ESC, TAB, arrows, Ctrl-C, Ctrl-D, `/`, `~`, and `|`

## Planned after the first physical test

- host editing and improved connection management
- private-key authentication and protected key storage
- configurable terminal/font/key-bar settings
- multiple sessions/tabs
- SFTP
- port forwarding
- host import/export

The project intentionally stays focused on being a lightweight SSH terminal rather than becoming another account-backed remote-management service.

## License

Apache-2.0. See `LICENSE`.
