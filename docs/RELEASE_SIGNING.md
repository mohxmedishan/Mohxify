# Private release signing setup

This guide prepares a new signing identity for future public releases. The existing signing key is committed in the public repository and must be considered compromised.

## 1. Create a new key

On a trusted computer or Android device running Termux, create a new JKS file. Use a strong, unique password and keep two offline backups.

```bash
keytool -genkeypair -v \
  -keystore mohxify-release.jks \
  -storetype JKS \
  -alias mohxify-release \
  -keyalg RSA -keysize 3072 -validity 10000
```

Do not reuse the legacy password. Do not commit this file.

## 2. Add GitHub repository secrets

Open the repository → Settings → Secrets and variables → Actions → New repository secret.

Create:

| Secret | Value |
|---|---|
| `MOHXIFY_KEYSTORE_BASE64` | One-line Base64 encoding of the JKS file |
| `MOHXIFY_STORE_PASSWORD` | Keystore password |
| `MOHXIFY_KEY_ALIAS` | `mohxify-release` |
| `MOHXIFY_KEY_PASSWORD` | Key password |

On Linux, produce the one-line Base64 value with:

```bash
base64 -w 0 mohxify-release.jks
```

On macOS, use `base64 < mohxify-release.jks | tr -d '\\n'`.

Paste the output directly into the GitHub secret field. Never paste the keystore or passwords into an issue, pull request, commit, or chat.

## 3. Build and test

After the workflow is merged into `main`, open Actions → **Build signed Mohxify release** → Run workflow. It verifies all four secrets, reconstructs the key only in the temporary GitHub Actions runner, runs JVM unit tests, builds a signed release APK, and publishes a versioned GitHub Release.

The temporary keystore file is ignored by Git and exists only for the duration of that runner.

## 4. One-time user migration

Android does not permit an APK signed with a different certificate to update an existing installation with the same package name.

Before publishing the first APK signed with the new key:

1. Test the new APK on a spare device.
2. Export and safely store your Mohxify dictionaries.
3. Announce that users must uninstall the old build once, install the new APK, and restore their dictionaries.
4. Keep the new key backed up securely. Losing it prevents future in-place updates.

Do not remove the legacy key or publish the rotated APK until the new build has been tested and the migration is intentional.
