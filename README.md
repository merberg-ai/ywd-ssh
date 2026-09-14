# YWD-SSH

A small, fast Android SSH client with a retro/cyber YWD interface.

The goal is deliberately simple: saved hosts, secure SSH connections, and a good terminal without accounts, subscriptions, telemetry, ads, or cloud dependencies.

> **Status:** very early development. The first milestone is password SSH + host-key verification + an interactive terminal.

## One-line Ubuntu build

From an Ubuntu machine, including a fresh development host:

```bash
curl -fsSL https://raw.githubusercontent.com/merberg-ai/ywd-ssh/main/scripts/build.sh | bash
```

The bootstrap script clones or updates the project in `~/src/ywd-ssh`, installs/checks the Android command-line build environment, builds the debug APK, and copies the result to:

```text
~/src/ywd-ssh/dist/ywd-ssh-debug.apk
```

## Local rebuild

Once the machine is bootstrapped:

```bash
cd ~/src/ywd-ssh
./scripts/build-local.sh
```

## Initial scope

- Android native app: Kotlin + Jetpack Compose
- ConnectBot `cbssh` for SSH
- ConnectBot `termlib` / libvterm for terminal emulation
- Saved host profiles (passwords are not saved)
- Persistent SSH host-key trust
- Password authentication
- Interactive `xterm-256color` PTY
- YWD dark/cyan/magenta styling
- Extra mobile terminal keys

Later milestones can add private-key authentication, SFTP, port forwarding, session tabs, import/export, and more.

## License

Apache-2.0. See `LICENSE`.
