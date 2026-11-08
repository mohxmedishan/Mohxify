# Mohxify

Mohxify is a local-first Android text replacement tool that works alongside your existing keyboard. It turns words and shortcuts into phrases you choose, while keeping your dictionaries on-device.

## Current features

- Accessibility-based replacement after a completed word and whitespace.
- Password fields are skipped.
- Punctuation around words is preserved; apostrophes inside words remain part of the word.
- ALL CAPS and Title Case are preserved.
- Named rule loadouts, one active at a time.
- Synonym alternatives can be cycled using Backspace after a replacement.
- Import rules from pasted text or a `.txt` file, preview changes, and merge or replace.
- Search originals and replacement values in large loadouts.
- Duplicate, rename, create, and delete loadouts.
- Copy or download the current loadout as a text file.
- Export every loadout and the active selection to a JSON backup, then restore from a validated backup with an explicit replace confirmation.
- WhatsApp-only floating GIF Actions button, with locally saved trigger-to-GIF mappings and explicit sharing to WhatsApp.
- Global ON/OFF switch and Quick Settings tile.
- Checks the public GitHub Releases feed for newer versions and offers in-app APK download and installation through Android’s system installer.
- Downloads are checked against the SHA-256 digest published with the GitHub release before installation.

## WhatsApp GIF Actions

When Mohxify's accessibility service is enabled, a small **GIF** button appears while WhatsApp is in the foreground. Tap it to open the Actions screen, add a trigger phrase, and choose a local GIF file. Saved actions can be searched and shared to WhatsApp.

The current implementation uses Android's `ACTION_SEND` media-sharing flow. WhatsApp may ask you to choose a conversation before you send the GIF. It does **not** silently send messages or guarantee insertion into the chat that was already open. Actual in-composer GIF insertion depends on WhatsApp's handling of rich content and needs device testing.

GIF mappings and selected document URIs are stored locally. Mohxify does not upload GIFs or trigger text.

## Desktop apps

A separate Windows and macOS desktop application is being developed in [desktop/](desktop/). It uses Tauri 2 and keeps its rules and loadouts locally on the device. See [docs/DESKTOP.md](docs/DESKTOP.md) for setup and build instructions. Android remains the existing Kotlin app and its APK workflow is unchanged.

## Build

The project uses Android Gradle Plugin 8.7.3, Kotlin 2.0.21, Java 17, and compile SDK 35.

Run the **Mohxify quality checks** workflow to execute JVM unit tests and build a debug APK. The signed release workflow is manual and requires the private signing secrets described in [Release signing](docs/RELEASE_SIGNING.md).

## Enable accessibility

1. Open Mohxify.
2. Read the service explanation and tap Enable accessibility service.
3. Enable Mohxify text replacement in Android Accessibility settings.
4. Return to Mohxify.
5. Add the Mohxify Quick Settings tile if desired.
6. Test in a harmless Notes field before using it elsewhere.

Accessibility is powerful. Mohxify uses it to detect completed words in editable fields and request a text replacement, and to display its configured floating Actions button while WhatsApp is foregrounded. It does not transmit typed text. Some apps use custom editors that do not support Android's standard text actions, so compatibility cannot be guaranteed everywhere.

## Privacy and compatibility

- Text processing, rule storage, GIF mappings, and trigger matching happen on-device.
- Mohxify uses internet access only to check public GitHub release metadata and download release APKs when you request an update. Typed text and dictionaries remain on-device and are not transmitted.
- Password fields are skipped.
- GIF files are selected by the user through Android's document picker and shared only after the user taps the WhatsApp share action.
- The app supports Android 8.0 (API 26) and targets API 35. Accessibility behavior can vary between apps and Android versions, so test on the devices and text fields you use.
- Accessibility-based text editing is not universal; custom editors may behave differently.

## Signing-key security — action required before a hardened public release

The legacy debug signing keystore was exposed in the repository and must be treated as compromised. It has been removed from the current source tree and is no longer used by CI. Historical commits may still contain it, so never reuse that identity or password. Anyone who obtains it could sign a counterfeit update with the same Android package identity.

The app's debug build now uses Gradle's local debug signing identity. Public release builds must use the separate private release signing configuration and GitHub Actions secrets. Before publishing a release:

1. Generate a new private JKS release key on a trusted device or computer. Use a strong unique password and keep at least two secure backups.
2. In GitHub repository Settings → Secrets and variables → Actions, create:
   - `MOHXIFY_KEYSTORE_BASE64`: the new JKS file encoded as Base64.
   - `MOHXIFY_STORE_PASSWORD`: keystore password.
   - `MOHXIFY_KEY_ALIAS`: key alias.
   - `MOHXIFY_KEY_PASSWORD`: key password.
3. Run **Build signed Mohxify release** from Actions.
4. Verify the APK installs and works on your test device before announcing it.

**Important update warning:** debug APKs and signed release APKs use different signing certificates, so Android will not let a signed release update a debug installation in place. For the one-time switch, export all loadouts to a JSON backup, uninstall this build, install the latest signed APK from GitHub Releases, and restore your backup. Existing installations signed with the exposed legacy key may also need this migration. After the app is on the private release signing key, future signed releases can update in place.

Never share the new keystore or its passwords in chat, issues, commits, or screenshots.

## Roadmap

See [docs/ROADMAP.md](docs/ROADMAP.md) for the longer-term plan: snippets, writing modes, dictionary packs, text transformations, optional keyboard support, local insights, and encrypted sync.
