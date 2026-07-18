# Walkthrough - Offline-first Measurement Sync

Implemented a robust offline-first synchronization mechanism for blood pressure measurements, mirroring the established pattern for intake reports. This ensures that measurements taken while offline are stored locally and successfully synced when connectivity is restored, rather than being clobbered by server refreshes.

## Changes Made

### [Data Layer]

#### [MODIFY] [MeasurementEntity.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/local/entity/MeasurementEntity.kt)
- Replaced the `isSynced` boolean with a `syncState` string field.
- Integrated the `SyncState` object to track `SYNCED`, `PENDING_CREATE`, and `PENDING_DELETE` states.
- Updated `toDto()` and `toEntity()` extensions to support the new sync state.

#### [MODIFY] [MeasurementDao.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/local/dao/MeasurementDao.kt)
- Added `getPending()` to retrieve all rows requiring synchronization.
- Added `deleteSynced()` to allow clearing the local cache of synced items without affecting pending offline data.
- Added `markPendingDelete()` for logical deletion of synced items while offline.

#### [MODIFY] [BpDatabase.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/local/BpDatabase.kt)
- Bumped database version from **5 to 6**.
- **Note**: This triggers a destructive migration, clearing local data. Users should be online and synced before upgrading.

### [Repository Layer]

#### [MODIFY] [MeasurementRepository.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/repository/MeasurementRepository.kt)
- **`createMeasurement`**: Now creates a local `PENDING_CREATE` record with a client-side UUID if the network is unavailable.
- **`getMeasurements`**: Implemented non-clobbering refresh; it deletes only `SYNCED` local records before inserting server data, keeping offline changes visible.
- **`deleteMeasurement`**: Handles both local-only (hard delete) and synced (mark `PENDING_DELETE` on failure) scenarios.
- **`syncPending`**: Added logic to drain the buffer of pending creations and deletions.

### [Feature Layer]

#### [MODIFY] [HomeViewModel.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/home/HomeViewModel.kt)
- Updated `refresh()` to trigger `syncPending()` before fetching the latest measurements, ensuring any local changes are pushed first.

### [Testing]

#### [NEW] [RealMeasurementRepositoryTest.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/test/java/ua/vn/home/bptracker/data/repository/RealMeasurementRepositoryTest.kt)
- Added comprehensive unit tests covering:
    - Offline creation of `PENDING_CREATE` rows.
    - Buffer draining via `syncPending` (replacing temporary IDs with server IDs).
    - Resilience of pending data during server refreshes (`getMeasurements`).

## Verification Results

### Automated Tests
- **Build**: Successfully assembled debug APK.
- **Unit Tests**: Passed all 23 tests in `:app:testDebugUnitTest`, including the new offline-sync scenarios.

```bash
./gradlew :app:testDebugUnitTest
# 23 passed, 0 skipped, 0 failed
```
