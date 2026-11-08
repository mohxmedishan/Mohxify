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
- Global ON/OFF switch and Quick Settings tile.
- No INTERNET permission. Check for updates opens the public GitHub Releases page.


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

Accessibility is powerful. Mohxify uses it only to detect completed words in editable fields and request a text replacement. It does not transmit typed text. Some apps use custom editors that do not support Android's standard text actions, so compatibility cannot be guaranteed everywhere.

## Privacy and compatibility

- Text processing and rule storage happen on-device.
- Mohxify does not request the INTERNET permission.
- Password fields are skipped.
- The app targets Android 8.0 (API 26) and currently targets API 29 for runtime behavior. Raising target SDK requires deliberate compatibility testing of the accessibility workflow and Android edge-to-edge behavior.
- Accessibility-based text editing is not universal; custom editors may behave differently.

## Signing-key security — action required before a hardened public release

The legacy APK signing keystore is currently committed in this public repository, and its password is present in the build configuration. Treat that signing identity as **publicly exposed**, not as a secure production key. Anyone who obtains it could sign a counterfeit update with the same Android package identity.

A private release signing configuration and a manual release workflow are prepared. Before using them:

1. Generate a new private JKS release key on a trusted device or computer. Use a strong unique password and keep at least two secure backups.
2. In GitHub repository Settings → Secrets and variables → Actions, create:
   - `MOHXIFY_KEYSTORE_BASE64`: the new JKS file encoded as Base64.
   - `MOHXIFY_STORE_PASSWORD`: keystore password.
   - `MOHXIFY_KEY_ALIAS`: key alias.
   - `MOHXIFY_KEY_PASSWORD`: key password.
3. Run **Build signed Mohxify release** from Actions.
4. Verify the APK installs and works on your test device before announcing it.

**Important update warning:** an APK signed with the new private key cannot update an installed APK signed with the old public legacy key. Users will need to save each loadout as a text file or export the full JSON backup, uninstall the old app once, install the newly signed APK, and restore their dictionaries. Do not delete the legacy key or publish the rotated APK until you have personally tested this migration and are ready to announce it.

Never share the new keystore or its passwords in chat, issues, commits, or screenshots.

## Roadmap

See [docs/ROADMAP.md](docs/ROADMAP.md) for the longer-term plan: snippets, writing modes, dictionary packs, text transformations, optional keyboard support, local insights, and encrypted sync.
