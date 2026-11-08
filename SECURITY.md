# Security policy

## Supported releases

Mohxify is currently a personal, open-source Android project. Report suspected vulnerabilities privately to the repository owner rather than publishing exploit details in a public issue.

## Important current limitation

The legacy APK signing keystore and its password are present in this public repository. Treat that signing identity as compromised. A malicious party with the key can sign an APK that Android may accept as an update to an installation signed with that same key.

The project now contains a separate private-release signing path. It must be configured with a new key stored only in GitHub Actions secrets before a hardened public release. See [private release signing setup](docs/RELEASE_SIGNING.md).

## Privacy

The current app does not request the INTERNET permission. Text replacement and rule storage are local. The Accessibility service is powerful and should be enabled only by users who trust the app. Password fields are skipped by the replacement service.
