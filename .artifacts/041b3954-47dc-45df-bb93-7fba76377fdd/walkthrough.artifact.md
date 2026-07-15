# Walkthrough — Prescriptions & Medication Items: Data Layer

Implemented the data layer for managing prescriptions and their medication items, following an offline-first architecture with Retrofit and Room.

## Changes

### Domain Layer
- Created [PrescriptionEnums.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/domain/model/PrescriptionEnums.kt) with serialized names matching the backend contract.
- Created domain models [Prescription.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/domain/model/Prescription.kt) and [MedicationItem.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/domain/model/MedicationItem.kt).

### Data Layer — Network
- Defined DTOs in [PrescriptionDtos.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/dto/PrescriptionDtos.kt) for Create, Read, and Patch operations.
- Implemented [PrescriptionApi.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/api/PrescriptionApi.kt) and [MedicationItemApi.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/api/MedicationItemApi.kt) Retrofit interfaces.

### Data Layer — Local Persistence
- Created Room entities [PrescriptionEntity.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/local/entity/PrescriptionEntity.kt) and [MedicationItemEntity.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/local/entity/MedicationItemEntity.kt) with mappers.
- Implemented DAOs [PrescriptionDao.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/local/dao/PrescriptionDao.kt) and [MedicationItemDao.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/local/dao/MedicationItemDao.kt).
- Updated [BpDatabase.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/local/BpDatabase.kt) to include new entities and DAOs (incremented to version 3).

### Data Layer — Repository & DI
- Implemented [PrescriptionRepository.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/repository/PrescriptionRepository.kt) (Real and Mock) for offline-first data access.
- Wired everything in [ServiceLocator.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/core/di/ServiceLocator.kt).

## Verification Results

### Automated Tests
- Executed `./gradlew app:assembleDebug` — **Build Successful**.

### Manual Verification
- Verified field naming (camelCase in DTOs) and serialization strategy (SnakeCase).
- Verified Enum `@SerialName` annotations against backend requirements.
- Confirmed Room Entity foreign keys and cascade delete configuration.
