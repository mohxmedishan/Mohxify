# Mohxify

Mohxify is a local Android text-replacement tool for Android 8+ that works alongside your existing keyboard.

## Features

- Accessibility-based replacement after a completed word and whitespace.
- Password fields are skipped.
- Punctuation around words is preserved; apostrophes inside words remain part of the word.
- ALL CAPS and Title Case are preserved.
- Named rule loadouts, one active at a time, with Shortcuts and Shakespeare defaults.
- Import rules from pasted text, preview changes, and merge or replace.
- Export/import all loadouts as a JSON backup, and copy the current loadout as text.
- Global ON/OFF switch and Quick Settings tile.
- No INTERNET permission. Check for updates opens the public GitHub Releases page.

## Build with GitHub Actions (phone-friendly)

You do not need Android Studio or a local terminal.

1. Open the repository's Actions tab.
2. Run Generate permanent Mohxify debug keystore once using Run workflow.
3. Open that completed run, find Artifacts, and download mohxify-debug-keystore.
4. Extract the downloaded ZIP on your phone. It contains mohxify-debug.keystore.
5. In the repository, choose Add file → Upload files, upload that file into the app/ folder (the final path must be app/mohxify-debug.keystore), and commit to main.
6. Each push to main builds mohxify.apk, uploads it as the Mohxify Actions artifact, and publishes it in a GitHub Release.
7. Download the latest APK from the repository's Releases page.

The release page is public because this repository is public; a GitHub account sign-in is not required to download its release assets.

## Signing-key transition warning

**The first APK signed with the new permanent keystore cannot update an APK signed with the old random GitHub Actions debug keystore.** Back up your loadouts, uninstall the old Mohxify once, install the new APK, and restore your backup if needed. After this one-time transition, future APKs use the same committed signing key and install over the top without removing app data.

The committed debug key is intentionally a personal-app signing key, not a production publishing key. Do not reuse it for unrelated apps.

## Enable accessibility

1. Open Mohxify.
2. Tap Enable accessibility service.
3. Enable Mohxify text replacement in Android Accessibility settings.
4. Return to Mohxify.
5. Add the Mohxify Quick Settings tile if desired.
6. Test in a harmless Notes field.

## Compatibility and privacy

The project targets minSdk 26 and targetSdk 29. Android accessibility does not guarantee that every app exposes editable text or accepts ACTION_SET_TEXT; custom editors may behave differently. Accessibility access is powerful, so enable the service only if you trust the app. Text processing and rule storage happen on-device.
