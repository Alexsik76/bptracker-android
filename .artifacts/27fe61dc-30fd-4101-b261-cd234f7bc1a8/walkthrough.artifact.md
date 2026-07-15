# Walkthrough - Fix build.gradle.kts Error

I have fixed the "Suspicious receiver type" error in the module-level `build.gradle.kts` file.

## Changes Made

### Build Configuration

#### [build.gradle.kts](file:///D:/dev/bp_tracker/mobile_app/app/build.gradle.kts)
- Moved the `kotlin { compilerOptions { ... } }` block from inside the `android { ... }` block to the top level of the build script. This is required by the new Android Gradle Plugin 9.0 DSL when using built-in Kotlin support, as the `kotlin` extension is now a top-level project extension.

## Verification Results

### Automated Tests
- Ran `./gradlew assembleDebug` and the build finished successfully without the "Suspicious receiver type" error.
- Kept `compileSdk` and `targetSdk` at version 35 to maintain stability and avoid using preview APIs (API 37).
