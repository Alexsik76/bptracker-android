# Implementation Plan - Fix Gradle and Kotlin Warnings

The goal is to resolve the warnings shown in the build output and identified by static analysis. This includes cleaning up `gradle.properties`, updating `build.gradle.kts`, and fixing code-level warnings in `BpDatabase.kt` and `CameraScanScreen.kt`.

## User Review Required

> [!NOTE]
> Most changes are mechanical cleanups of deprecated settings and redundant code. No functional changes are expected.

## Proposed Changes

### Build Configuration

#### [MODIFY] [gradle.properties](file:///D:/dev/bp_tracker/mobile_app/gradle.properties)
- Remove deprecated/experimental flags:
    - `android.defaults.buildfeatures.resvalues`
    - `android.sdk.defaultTargetSdkToCompileSdkIfUnset`
    - `android.enableAppCompileTimeRClass`
    - `android.usesSdkInManifest.disallowed`
    - `android.r8.optimizedResourceShrinking`
    - `android.disallowKotlinSourceSets`
- Add performance improvement flag:
    - `android.dependency.excludeLibraryComponentsFromConstraints=true`

#### [MODIFY] [build.gradle.kts](file:///D:/dev/bp_tracker/mobile_app/app/build.gradle.kts)
- Explicitly enable `resValues` in `buildFeatures` as it is now disabled by default in AGP 8+.

### Data Layer

#### [MODIFY] [BpDatabase.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/local/BpDatabase.kt)
- Update `fallbackToDestructiveMigration()` to use the non-deprecated version with a boolean parameter.

### UI / Camera Feature

#### [MODIFY] [CameraScanScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/camera/CameraScanScreen.kt)
- Remove the unused and shadowed `ImageProxy.toBitmap()` extension function (CameraX now provides this as a member).
- Fix various Kotlin lint/style warnings:
    - Move lambda arguments out of parentheses.
    - Use property access syntax for `surfaceProvider`.
    - Add missing trailing commas.
    - Fix explicit `get` call on ByteBuffer.
    - Add parameter names for boolean literals in function calls.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to ensure all warnings are gone (or significantly reduced) and the project builds successfully.

### Manual Verification
- Launch the app and verify the camera scanning functionality still works as expected.
