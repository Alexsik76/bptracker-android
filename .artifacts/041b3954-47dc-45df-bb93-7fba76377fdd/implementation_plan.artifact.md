# Implementation Plan — Revise Prescriptions Data Layer

Revise the prescriptions and medication items data layer to follow the "DTO-through" pattern, ensure atomic cache refreshes, and unify enum persistence using wire values.

## User Review Required

> [!IMPORTANT]
> - All domain models for prescriptions and medication items will be removed. The repository will now return `PrescriptionReadDto` and `MedicationItemReadDto`.
> - The `refresh()` method in `PrescriptionRepository` will be made atomic by fetching all data from the network before performing any Room deletions or insertions within a transaction.
> - All enums in `MedicationItemEntity` will be stored using their `@SerialName` values (JSON serialized) to ensure consistency between the local database and backend wire format.

## Proposed Changes

### [DELETE] Domain Models
Remove the `ua.vn.home.bptracker.domain.model` package.
- [DELETE] [Prescription.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/domain/model/Prescription.kt)
- [DELETE] [MedicationItem.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/domain/model/MedicationItem.kt)

### [NEW] [PrescriptionEnums.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/dto/PrescriptionEnums.kt)
Move enums from `domain.model` to `data.dto`. Package: `ua.vn.home.bptracker.data.dto`.

---

### Room Entities & DAOs

#### [MODIFY] [PrescriptionEntity.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/local/entity/PrescriptionEntity.kt)
- Rename `toDomain()` to `toDto()`.
- Return `PrescriptionReadDto`.

#### [MODIFY] [MedicationItemEntity.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/local/entity/MedicationItemEntity.kt)
- Rename `toDomain()` to `toDto()`.
- Return `MedicationItemReadDto`.
- **Enum Persistence Fix**:
    - Update `toDomain` (now `toDto`) to decode `doseUnit`, `freqPeriodUnit`, and `courseType` using the JSON serializer and `@SerialName` values.
    - Implement fallback defaults (e.g., `CourseType.Ongoing`).
    - Update `toEntity()` to serialize all enums using the JSON serializer.

---

### Repositories

#### [MODIFY] [PrescriptionRepository.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/repository/PrescriptionRepository.kt)
- Update interface to use DTOs.
- **RealPrescriptionRepository**:
    - Inject `BpDatabase` to support transactions.
    - **Atomic `refresh()`**:
        1. Fetch all prescriptions.
        2. Fetch items for all prescriptions into memory.
        3. Use `db.withTransaction { ... }` to clear and populate Room tables.
    - Remove misleading comment about cascade uncertainty in `deletePrescription`.
- **MockPrescriptionRepository**:
    - Replace `TODO()` with trivial implementations (empty flows or mocked DTO responses).

---

### DI Wiring

#### [MODIFY] [ServiceLocator.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/core/di/ServiceLocator.kt)
- Pass `database` (the `BpDatabase` instance) into `RealPrescriptionRepository`.

## Verification Plan

### Automated Tests
- Run `./gradlew app:assembleDebug` to verify compilation and dependency wiring.
- Check generated Room code if possible (via build logs/outputs) for correct transaction usage.

### Manual Verification
- Inspect `MedicationItemEntity` mappers to ensure all enums use the serializer and have guarded decoding.
- Verify `refresh()` logic for atomicity and error handling (fetches first, then deletes).
