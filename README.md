# Mohxify

A small Android 8+ prototype that attempts custom autocorrect-style word replacement while keeping the user's existing keyboard (including Gboard).

## Current behaviour

- Watches accessibility `TYPE_VIEW_TEXT_CHANGED` events.
- When an editable field ends in whitespace, it checks the word immediately before the final whitespace.
- Replaces exact dictionary matches, preserving simple ALL-CAPS and Title Case.
- Stores the dictionary and enabled state locally in app preferences.
- Includes a Quick Settings tile for a fast ON/OFF switch.

Default rules:
- what -> wt
- just -> js
- about -> abt
- because -> bcs

## Important limitations

This is a prototype, not a universal Android input method. Android accessibility APIs do not guarantee that every app exposes editable text or accepts `ACTION_SET_TEXT`. Some apps, secure fields, custom editors, and certain keyboard/input combinations may not work. Replacing the entire field text can also be rejected by an app or disturb cursor behaviour. Test in a harmless Notes field first.

The accessibility service can read text in editable fields while enabled. This app does not request internet permission and the code does not transmit text, but review the source and only enable accessibility for software you trust. Do not enable it in password or other sensitive fields.

## Validation performed

- The pure Kotlin replacement engine was compiled with `kotlinc`.
- Eight logic checks passed: the four default mappings, ALL-CAPS, Title Case, unchanged unrelated words, and a non-matching longer word.
- The Android app itself has not been built or run: this environment has no Android SDK or Gradle installation.

## Build

Requirements: Android Studio with Android SDK 35 and JDK 17.

1. Open this folder in Android Studio.
2. Let Gradle sync.
3. Build > Build Bundle(s) / APK(s) > Build APK(s).
4. Install the generated debug APK on the phone.

The project intentionally targets Android 10 compatibility (minSdk 26, targetSdk 29).

## Enable

1. Open Mohxify.
2. Tap **Enable accessibility service**.
3. In Android Accessibility settings, enable **Mohxify text replacement**.
4. Return to Mohxify.
5. Pull down Quick Settings, edit the tiles, and add **Mohxify**.
6. Test `what ` in a normal Notes text field.

## Privacy

No INTERNET permission is declared. The service processes text locally. Accessibility access is powerful: it can potentially observe text in editable fields in apps where Android exposes it. Disable the service whenever you do not need it.
