# Tasks — Revise Prescriptions Data Layer

- [ ] Remove Domain Models Layer
    - [x] Delete `Prescription.kt` and `MedicationItem.kt`
    - [x] Move `PrescriptionEnums.kt` to `data.dto`
    - [x] Remove `domain/model` directory
- [x] Update DTOs and Imports
    - [x] Fix imports in `PrescriptionDtos.kt`
- [x] Update Room Persistence
    - [x] Update `PrescriptionEntity.kt` (mapper to DTO)
    - [x] Update `MedicationItemEntity.kt` (mapper to DTO, unified enum persistence)
- [x] Refactor Repository
    - [x] Update `PrescriptionRepository` interface to use DTOs
    - [x] Implement atomic `refresh()` in `RealPrescriptionRepository` using transactions
    - [x] Update `MockPrescriptionRepository` with trivial implementations
- [x] Dependency Injection
    - [x] Update `ServiceLocator` to pass database to repository
- [x] Verification
    - [x] Build project (`./gradlew app:assembleDebug`)
    - [x] Commit and push to `dev`
