# Walkthrough — Refresh Dashboard on Resume

I have implemented a refresh mechanism for the Dashboard (Home screen) that triggers whenever the user returns to it. This ensures that any new measurements added while the Dashboard was in the back stack are immediately visible.

## Changes Made

### Android App

#### [MainActivity.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/MainActivity.kt)

- Added `LifecycleEventEffect(Lifecycle.Event.ON_RESUME)` to the `home` navigation destination.
- This effect calls `homeVm.refresh()` every time the screen is resumed.
- Added necessary imports: `androidx.lifecycle.Lifecycle` and `androidx.lifecycle.compose.LifecycleEventEffect`.

```diff
             composable("home") {
                 val homeVm: HomeViewModel = viewModel()
                 val homeState by homeVm.state.collectAsState()

+                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
+                    homeVm.refresh()
+                }
+
                 HomeScreen(
                     state = homeState,
```

## Verification Results

### Automated Tests
- Executed `./gradlew app:assembleDebug` — **Passed**.

### Manual Verification Path
1. Launch the app to the Home screen.
2. Navigate to "History" or "Manual Entry".
3. Add a new measurement.
4. Go back to the Home screen.
5. **Observation**: The hero tile and recent readings list now update instantly to show the new record.
