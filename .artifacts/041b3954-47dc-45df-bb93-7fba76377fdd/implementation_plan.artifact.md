# Implementation Plan — Prescriptions & Medication Items: Data Layer

Building the data layer for Prescriptions and Medication Items, including DTOs, domain models, Retrofit APIs, Room entities/DAOs, and offline-first repositories.

## User Review Required

> [!IMPORTANT]
> - I will introduce a `domain.model` package to strictly adhere to the "DTOs do not leak" rule, as existing patterns were slightly ambiguous regarding this.
> - Mutations are network-first as requested; local Room mirror will be updated only after successful API calls.

## Proposed Changes

### [NEW] Domain Models
Create a new package `ua.vn.home.bptracker.domain.model` to hold domain-specific classes.

#### [NEW] [Prescription.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/domain/model/Prescription.kt)
#### [NEW] [MedicationItem.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/domain/model/MedicationItem.kt)
#### [NEW] [PrescriptionEnums.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/domain/model/PrescriptionEnums.kt)
Contains `WhenSlot`, `DoseUnit`, `FreqPeriodUnit`, and `CourseType` with `@SerialName` annotations.

---

### Data Layer — DTOs & API

#### [NEW] [PrescriptionDtos.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/dto/PrescriptionDtos.kt)
Contains Read, Create, and Partial Update DTOs for both Prescriptions and Medication Items.
- Includes `prescription_id` in `MedicationItemReadDto`.
- Does NOT include `created_at` in `MedicationItemReadDto` as per OpenAPI check.

#### [NEW] [PrescriptionApi.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/api/PrescriptionApi.kt)
Retrofit interface for prescriptions.

#### [NEW] [MedicationItemApi.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/api/MedicationItemApi.kt)
Retrofit interface for medication items nested under prescriptions.

---

### Data Layer — Room

#### [NEW] [PrescriptionEntity.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/local/entity/PrescriptionEntity.kt)
#### [NEW] [MedicationItemEntity.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/local/entity/MedicationItemEntity.kt)
- `MedicationItemEntity` will have a foreign key to `PrescriptionEntity` with `onDelete = ForeignKey.CASCADE`.
- `when_slots` will be stored as `medsJson` style (manually serialized list).

#### [NEW] [PrescriptionDao.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/local/dao/PrescriptionDao.kt)
#### [NEW] [MedicationItemDao.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/local/dao/MedicationItemDao.kt)

#### [MODIFY] [BpDatabase.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/local/BpDatabase.kt)
Register new entities and DAOs. Increment version if necessary (though destuctive migration is enabled).

---

### Data Layer — Repositories & DI

#### [NEW] [PrescriptionRepository.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/repository/PrescriptionRepository.kt)
Offline-first implementation:
- `getPrescriptions()`: Returns `Flow<List<Prescription>>`.
- `refresh()`: Fetches from network and syncs to Room.
- Mutations: API first, then Room on success.
- Deleting a prescription clears items from Room.

#### [MODIFY] [ServiceLocator.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/core/di/ServiceLocator.kt)
Wiring up the new APIs, DAOs, and repositories.

## Verification Plan

### Automated Tests
- Build the project using `./gradlew assembleDebug` to ensure compilation and dependency wiring are correct.

### Manual Verification
- Verify code structure against `measurements` module for consistency.
- Inspect generated DAOs and entities for correct Room annotations.
- Verify DTO mapping logic in repositories.
