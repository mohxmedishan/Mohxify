# Mohxify Keyboard — Build Prompt

> **Purpose:** This is the implementation brief for the future Mohxify Android keyboard. It is intentionally focused. Build a familiar, polished everyday keyboard with the useful Gboard-style essentials, not a giant experimental keyboard full of features nobody asked for.
>
> **Timing:** This document is a build prompt for a future implementation session. Do not start implementation merely by reading or committing this document. When implementation is requested, follow the brief below.

---

## Prompt for the implementation model

You are working on **Mohxify**, an existing Kotlin Android app in this repository. Your task, when explicitly asked to begin, is to build a first-party Android keyboard (IME) for Mohxify.

The goal is a keyboard that feels immediately familiar to anyone who has used Gboard: clean QWERTY layout, sensible key sizing and spacing, familiar controls, responsive typing, and a polished suggestion strip. Use Gboard as a usability and layout reference, **not** as something to copy pixel-for-pixel. Do not use Google's branding, logos, proprietary assets, or claim this is Gboard.

### 1. Product scope: keep it focused

The keyboard should concentrate on these things:

1. **Normal typing**
2. **GIFs**
3. **Emoji**
4. **Clipboard**

That is the product. Do not add a pile of unrelated features.

#### A. Normal keyboard

Implement the standard essentials expected from a modern Android keyboard:

- Familiar QWERTY letter layout.
- Shift / caps behavior, including double-tap caps lock if practical.
- Backspace with repeat while held.
- Space bar, enter/action key, and punctuation.
- A simple numbers-and-symbols layout, with a way back to letters.
- Basic long-press alternate characters where straightforward.
- Correct handling of text fields, selection, cursor position, composing text, and editor actions.
- A compact suggestion strip above the keys.
- Sensible key press feedback and responsive touch targets.
- Respect the current editor's input type, such as email, number, password, and multiline fields. Do not show or store sensitive password input in suggestions or clipboard history.

Do not attempt to recreate every obscure Gboard feature. Reliable typing matters more than feature count.

#### B. GIF keyboard and trigger suggestions

This is a core Mohxify feature, not an optional afterthought.

- Provide a GIF panel accessible from a clear GIF button on the keyboard.
- Reuse the existing Mohxify GIF/action data model and local GIF storage where possible. Inspect the current implementation before designing a second incompatible system.
- Users must be able to add, remove, search, and manage GIFs and their trigger words from the existing Mohxify app UI. The keyboard must use the same saved mappings.
- While the user types a configured trigger (for example, ;babymonster), detect the matching trigger and show its GIF as a suggestion in the keyboard suggestion strip or a compact suggestion row.
- Tapping that GIF suggestion should attempt to insert the GIF into the current app's composer/editor. The intended WhatsApp flow is: type trigger → see GIF suggestion → tap suggestion → GIF appears in the WhatsApp composer → the user taps WhatsApp's Send button.
- **Never auto-send a message or GIF.** The user must remain in control of sending.
- Use Android's supported rich-content IME pathway (including InputConnection.commitContent() and the appropriate content URI permission flow) when the target editor supports it.
- Declare and negotiate the relevant MIME types correctly. Handle unsupported editors and rejected content gracefully.
- If direct insertion is unsupported, provide a clear fallback that opens the existing Mohxify GIF Actions/share flow. Do not pretend the fallback is direct composer insertion.
- Keep GIFs and trigger mappings on-device. Do not upload them to a server.

The current repository already has a WhatsApp GIF Actions feature, a GIF import path, a content provider, and saved trigger mappings. Inspect those implementations and reuse or safely migrate them. Do not break the current manual share/import route; it remains a useful fallback.

#### C. Emoji

- Provide a dedicated emoji picker opened from the keyboard.
- Include practical categories and a recently used section if it can be implemented reliably.
- Tapping an emoji inserts it at the current cursor position.
- Include a simple search if it fits cleanly, but do not let search delay or complicate the first working version.
- Do not build emoji generation, AI emoji, avatars, stickers, or elaborate emoji customization.

#### D. Clipboard

- Provide a clipboard panel with a small, useful history of text the user explicitly copies while Mohxify is the active keyboard, where Android permits it.
- Allow tapping an item to paste it into the current editor.
- Allow pinning and deleting saved items, with a simple clear-history action.
- Do not collect clipboard contents silently in the background. Respect Android clipboard privacy restrictions and only capture data through user-visible, permitted interactions.
- Never store clipboard contents from password fields or other sensitive input.
- Keep clipboard data local to the device. No cloud sync.

### 2. Visual and interaction direction

- Make the keyboard feel as familiar and easy to use as Gboard: standard QWERTY proportions, clear key hierarchy, a top utility/suggestion row, and predictable placement of controls.
- Use Mohxify's existing visual identity and app styling where appropriate. The keyboard should look like part of Mohxify, not a random third-party keyboard skin.
- Keep the design clean, modern, compact, and readable. Avoid excessive gradients, glass effects, decorative clutter, and oversized controls.
- Support light and dark system themes if straightforward.
- Make the keyboard comfortable on normal phone screens and responsive to screen size/insets.
- The keyboard must not cover the text field unnecessarily or jump/flicker when panels open and close.
- Provide smooth transitions between the normal keyboard, GIF panel, emoji panel, and clipboard panel.
- Prioritize reliable touch handling and stability over animations.

### 3. Android implementation requirements

Before changing code, inspect the actual repository structure, existing app architecture, Gradle setup, manifest, resources, persistence layer, GIF feature, and text replacement logic. Do not assume file names or invent APIs before inspecting them.

Implement Mohxify as a proper Android **Input Method Editor (IME)**:

- Register an InputMethodService in the Android manifest with the correct binding permission and intent filter.
- Add the required IME metadata XML and keyboard resources.
- Add an in-app route/screen that explains how to enable Mohxify Keyboard and opens the relevant Android input-method settings where supported.
- Make it possible to switch to Mohxify using Android's supported input-method picker.
- Handle IME lifecycle correctly: creation, input view, input view start/finish, input connection changes, selection updates, configuration changes, and teardown.
- Use Android's supported InputConnection APIs for composing and committing text. Avoid fragile accessibility tricks or floating overlays for ordinary keyboard behavior.
- Use Android's supported rich-content APIs for GIF insertion. Check target-editor MIME support and URI permissions instead of assuming every app accepts GIF content.
- Do not request broad permissions without a concrete need. Do not add network access for keyboard functionality.
- Keep the app's existing accessibility-based text replacement feature working. The new keyboard should be an additional first-party input method, not a reason to silently remove or regress the current feature.
- Integrate the existing text replacement rules/loadouts if feasible and architecturally sound. Reuse the same source of truth; do not create a separate dictionary that drifts out of sync.
- Avoid major new dependencies unless they are genuinely necessary. Explain any dependency added.

### 4. Privacy and safety requirements

A keyboard handles extremely sensitive information. Treat privacy as a core engineering requirement:

- All typing, trigger matching, GIF mappings, emoji recents, and clipboard history must be processed and stored locally.
- No keystrokes, typed text, clipboard contents, passwords, or GIF usage data may be sent to analytics, telemetry, external APIs, or a server.
- Do not log typed text or clipboard contents.
- Do not save password-field input or show it in suggestions.
- Respect Android's password and sensitive-field input flags.
- Do not silently read the clipboard or monitor text outside the keyboard's permitted, user-visible operation.
- Make clipboard history easy to clear.
- Do not auto-send messages or content.

### 5. Explicitly out of scope

Do **not** implement these in this project phase:

- Voice typing or speech recognition.
- AI writing, AI chat, AI-generated replies, or generative features.
- Translation.
- Web search built into the keyboard.
- Cloud accounts, cloud sync, or online clipboard sync.
- Themes marketplace or elaborate theme editor.
- Stickers, avatars, kaomoji collections, or GIF generation.
- Handwriting input.
- Swipe/glide typing unless it is trivial and does not delay the core keyboard.
- Floating keyboard, one-handed mode, split keyboard, or elaborate resizing controls.
- Advanced grammar correction, predictive AI, or a complex personalization engine.
- A full replacement for every Gboard feature.

If a feature is not in the four core areas (normal typing, GIFs, emoji, clipboard), leave it out unless it is essential for a functional Android keyboard.

### 6. Build in sensible stages

Do not attempt a huge, unreviewable rewrite. Work in small stages, keeping the app buildable after each one:

1. **Repository audit and plan:** identify reusable code, data models, current GIF flow, app navigation, and any compatibility risks. Report the plan before editing.
2. **IME foundation:** manifest registration, metadata, enable/setup flow, keyboard view, and reliable basic typing.
3. **Keyboard essentials:** symbols, shift, backspace, enter, space, editor actions, input-type handling, and suggestion strip.
4. **Emoji panel:** local emoji picker and insertion.
5. **Clipboard panel:** privacy-respecting local history and paste controls.
6. **GIF panel and trigger suggestions:** reuse existing GIF data, trigger matching, rich-content insertion, and graceful fallback.
7. **Integration and polish:** verify existing accessibility replacement, GIF Actions screen, import/share flow, app navigation, and lifecycle behavior.
8. **Tests and release readiness:** unit tests, build, APK artifact, and a clear device-test checklist.

Do not claim a stage is complete until the code, tests, and build support that claim.

### 7. Acceptance criteria

The implementation is not complete until all of the following are true:

- Mohxify appears in Android's available keyboards and can be enabled through the app's setup guidance.
- The keyboard opens in normal editable text fields and types correctly.
- Shift, caps lock (if implemented), backspace, space, punctuation, symbols, and enter/editor actions work.
- Cursor movement, selection, and composing text do not corrupt existing text.
- Password fields do not leak input into suggestions, logs, or clipboard history.
- GIF, emoji, and clipboard panels open and close reliably without flicker or losing the current editor connection.
- Emoji inserts at the current cursor.
- Clipboard items can be pasted, pinned, deleted, and cleared, subject to Android's privacy restrictions.
- A configured trigger produces the correct GIF suggestion while typing.
- Tapping a GIF suggestion inserts it into WhatsApp's composer on a compatible WhatsApp/device version, without sending it.
- If the editor rejects GIF insertion, the user gets a clear working fallback through the existing share flow.
- Existing Mohxify accessibility text replacement, loadouts, GIF Actions management, and manual GIF import/share remain functional.
- No typed text, passwords, clipboard contents, or GIF data are transmitted off-device.
- The project builds successfully and relevant tests pass.

### 8. Device testing and honest reporting

The primary real-world test device is:

- **Huawei Y9 Prime 2019**
- **Model:** STK-L21
- **Android:** 10
- **EMUI:** 12
- **RAM:** 4 GB

Also test on an emulator or another Android device if available. Test at minimum in WhatsApp, a basic Notes/text editor, and a browser text field.

Android and individual apps control whether rich GIF content can be committed into their editors. Do not promise universal GIF insertion. Record the exact app/device/version tested and distinguish clearly between:

- implemented in code,
- unit-tested,
- successfully built,
- installed on a device,
- verified in a specific target app.

Never say "works everywhere" based on a successful build or emulator test.

---

## Final instruction

Build a **simple, dependable, Gboard-familiar Mohxify keyboard** focused on **typing + GIFs + emoji + clipboard**. Do not inflate the scope. Make the core experience genuinely reliable, preserve existing Mohxify features, protect user privacy, and be honest about platform limitations.