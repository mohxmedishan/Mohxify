# Security policy

## Signing identity

The legacy debug keystore was exposed in the public repository history. It is compromised and must never be reused. The current source tree removes the keystore and CI uses Gradle's generated local debug signing identity. Production releases must use the private signing secrets documented in [Release signing](docs/RELEASE_SIGNING.md).

Removing a file from the current branch does not erase it from existing Git history or previously downloaded clones. Treat the old certificate and password as public indefinitely.

## Reporting

Please report security concerns privately to the repository owner rather than publishing exploit details in an issue. Do not include private keystores, passwords, or personal text in reports.
