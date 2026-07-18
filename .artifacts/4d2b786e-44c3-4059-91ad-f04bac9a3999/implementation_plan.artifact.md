# Implementation Plan - Wire Signed Release Build

Set up a proper signed `release` build for the Android application while ensuring that no secrets are committed to the repository. The signing configuration will be loaded from a local, git-ignored `keystore.properties` file.

## User Review Required

> [!IMPORTANT]
> **Leaked Secrets Detected**: The file `keystore.properties` is currently tracked by Git and contains sensitive information (passwords). It must be removed from the repository history and the secrets should be rotated. Per your instructions, I will flag this prominently but will not delete the committed file myself.

## Proposed Changes

### [Component Name]

#### [MODIFY] [build.gradle.kts](file:///D:/dev/bp_tracker/mobile_app/app/build.gradle.kts)
- Load `keystore.properties` from the root project directory at the start of the `android { }` block.
- Define `signingConfigs` for the `release` build, loading values from the properties if available.
- Update `buildTypes` to associate the `release` build type with the `release` signing configuration.

#### [MODIFY] [.gitignore](file:///D:/dev/bp_tracker/mobile_app/.gitignore)
- Add entries to exclude `keystore.properties`, `*.jks`, and `*.keystore` from version control.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to ensure that the project still configures and builds the debug version correctly even when signing is wired for release.
- Run `git ls-files keystore.properties *.jks *.keystore` after updating `.gitignore` to confirm if any are still tracked (they will be if already committed, which I will report).

### Manual Verification
- Verify that `assembleRelease` attempt fails or succeeds gracefully depending on the presence of the local `keystore.properties` (without actually providing the secret file in this environment).
