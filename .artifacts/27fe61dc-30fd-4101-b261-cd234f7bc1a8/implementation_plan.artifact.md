# Implementation Plan - Resolve Experimental Warning for Commit/Push

The goal is to eliminate the "experimental" warning caused by `android.disallowKotlinSourceSets=false` in `gradle.properties`. This warning currently blocks the Git commit/push process in Android Studio with extra confirmation steps. We will achieve this by manually registering KSP-generated sources using the new AGP 9.0 API, allowing us to remove the problematic flag.

## Proposed Changes

### Build Configuration

#### [MODIFY] [gradle.properties](file:///D:/dev/bp_tracker/mobile_app/gradle.properties)
- Remove `android.disallowKotlinSourceSets=false`.
- Add `ksp.registerSourceSets=false` to prevent KSP from using the disallowed legacy DSL.

#### [MODIFY] [build.gradle.kts](file:///D:/dev/bp_tracker/mobile_app/app/build.gradle.kts)
- Add an `androidComponents` block to manually register KSP generated source directories for each variant. This uses the modern AGP 9.0 `Sources` API.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to ensure the project still builds successfully.
- Verify that the "experimental" warning for `android.disallowKotlinSourceSets` is gone from the build/sync output.

### Manual Verification
- Check that KSP-generated code (like Room DAOs/Databases) is still recognized by the compiler and IDE.
