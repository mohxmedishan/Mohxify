# Mohxify Desktop

The desktop application lives in desktop/ and is packaged with Tauri 2. It is independent of the Android Gradle project; Android source and APK workflows remain unchanged.

## Development

Requirements: Node.js 22, Rust stable, and the platform prerequisites documented by Tauri.

    cd desktop
    npm install
    npm run tauri -- dev

## Build installers

    cd desktop
    npm install
    npm run tauri -- build

The app icon is generated from the checked-in source asset automatically before dev and release builds. Build native installers on their respective operating systems. Windows produces an NSIS .exe installer and macOS produces a .dmg. The GitHub Actions workflow builds both targets independently and uploads the installers as workflow artifacts.

## Current desktop functionality

- Local text transformation with case preservation and word-boundary matching.
- Multiple named loadouts with create, rename, activate, and delete actions.
- Rule creation, editing, deletion, search, and .txt import/export.
- Android-compatible JSON backup and restore with structural validation and replacement confirmation.
- Synonym alternatives and a next-synonym control.
- Clipboard paste and copy from the editor.
- Dark and light appearance modes.
- Rules and loadouts stored locally on the device; no account or cloud service is required.

## Platform parity notes

Android's Accessibility Service is platform-specific and is not reused by this desktop target. This first desktop implementation provides an in-app transformation workspace. System-wide replacement in arbitrary Windows/macOS applications requires separate native integrations and explicit OS permissions; it is not silently enabled. The Android app, its accessibility behavior, and its release pipeline are untouched.

A new installation starts with an empty Loadout 1, matching Android. Import existing .txt dictionaries or restore an Android Mohxify JSON backup to bring your rules onto desktop.