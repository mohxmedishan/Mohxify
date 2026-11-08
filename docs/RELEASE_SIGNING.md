# Private release signing setup

The legacy debug signing key was exposed in the public repository and must be considered compromised. It has been removed from the current source tree, but older Git commits and existing clones may still contain it. Never reuse the legacy key or password.

## 1. Create a new private release key

On a trusted computer, create a new JKS file with a strong, unique password. Keep two secure offline backups.

```bash
keytool -genkeypair -v \
  -keystore mohxify-release.jks \
  -storetype JKS \
  -alias mohxify-release \
  -keyalg RSA -keysize 3072 -validity 10000
```

Do not commit this file or share it in issues, pull requests, screenshots, or chat.

## 2. Add GitHub Actions secrets

Open repository Settings → Secrets and variables → Actions and create:

| Secret | Value |
|---|---|
| `MOHXIFY_KEYSTORE_BASE64` | One-line Base64 encoding of the new JKS |
| `MOHXIFY_STORE_PASSWORD` | Keystore password |
| `MOHXIFY_KEY_ALIAS` | `mohxify-release` |
| `MOHXIFY_KEY_PASSWORD` | Key password |

On Linux, create the Base64 value with `base64 -w 0 mohxify-release.jks`. Never paste the raw key or passwords into source control.

## 3. Build and test

After the release workflow is merged into `main`, open Actions → **Build signed Mohxify release** → Run workflow. The workflow reconstructs the private key only on the temporary runner, verifies the signing identity, runs tests, builds the release APK and publishes a GitHub Release.

## 4. Existing-install migration

Android will not install an APK signed with a different certificate as an update to an existing installation.

1. Export all Mohxify loadouts to a JSON backup before changing signing identities.
2. Test the new APK on a spare device.
3. Users with builds signed by the exposed legacy key must uninstall that build, install the new APK and restore their backup.
4. Keep the new private key backed up. Losing it prevents future in-place updates.

The debug workflow now uses Gradle's local debug signing identity. The public release workflow uses only the private GitHub Actions secrets. Do not publish a replacement release until the migration has been tested and clearly communicated.
