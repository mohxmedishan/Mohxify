# Mohxify product roadmap

Mohxify should grow from a text-replacement utility into a privacy-first personal language toolkit. The roadmap below separates near-term improvements from larger product bets. Ideas are proposals, not claims about features that already exist.

## Product principles

- Local-first by default: personal dictionaries and snippets stay on the device unless the user explicitly exports or syncs them.
- User control: every automation can be previewed, paused, edited, and disabled.
- Transparent permissions: explain exactly why Accessibility access is needed and never collect typed text.
- Keyboard-agnostic first: keep working alongside existing keyboards; an optional dedicated keyboard can come later.
- Backward compatibility: preserve existing loadouts and provide reliable, tested backups before storage migrations.

## Phase 1 — Make the current app dependable

- Search and filter large dictionaries.
- Duplicate, rename, import, export, and back up individual loadouts.
- Add robust JSON backup restore with validation and a non-destructive preview.
- Improve the first-run setup and Accessibility explanation.
- Add automated tests for replacement, parsing, migrations, and backup formats.
- Add compatibility diagnostics for apps that do not expose editable text.
- Move from the publicly exposed legacy signing key to a private release key only after an explicit signing-key migration plan.

## Phase 2 — Personal language toolkit

- **Snippet library:** expand triggers into multi-line templates, email signatures, addresses, code fragments, and reusable blocks.
- **Writing modes:** one-tap dictionaries for Shakespeare, pirate, formal, casual, gaming, study, and custom vocabularies.
- **Text transformations:** case conversion, date/time tokens, Unicode symbols, punctuation cleanup, and configurable formatting.
- **Dictionary packs:** curated downloadable/importable packs with versioning, attribution, preview, and safe update/rollback.
- **Per-app profiles:** optional allow/deny lists, with clear warnings and an easy global pause. This must be tested carefully because app identity and editable text availability vary.

## Phase 3 — Beyond replacement

- **Mohxify Studio:** a visual editor for building, testing, sharing, and versioning language packs.
- **Template variables:** user-triggered fields such as name, date, subject, and reusable form sections.
- **Optional keyboard companion:** a dedicated keyboard for users who want deeper control, while retaining the existing accessibility-based mode.
- **Local typing insights:** opt-in counts of expansions and time saved, computed on-device without recording message contents.
- **Encrypted sync:** optional end-to-end encrypted backup across a user's devices; never required for core functionality.
- **Community pack directory:** moderated, signed packs with clear authorship, permissions, changelogs, and user reviews.

## What not to build too early

- Do not add cloud accounts or analytics before local-first privacy is solid.
- Do not promise universal compatibility; Android apps can expose different editing capabilities.
- Do not automatically rewrite whole sentences or send text anywhere.
- Do not bundle AI rewriting until users can explicitly preview changes and keep processing private and optional.

## Suggested north-star

**Mohxify is your personal language layer:** the words, phrases, styles, and templates you want available everywhere you type.
