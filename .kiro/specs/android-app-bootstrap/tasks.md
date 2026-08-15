# Implementation Plan: Android App Bootstrap (A0)

## Overview

Establish a buildable, navigable Android application skeleton with Jetpack Compose, Material 3, six screens in linear flow, fake repositories for offline operation, strict layer separation (`Presentation → Domain ← Data`), and a network skeleton ready for real backend integration in later stages.

No actual API calls, camera integration, Meta DAT integration, object detection, tracking, attention logic, or ML is included in A0.

---

## Tasks

- [ ] 1. Android Project Bootstrap

  - [x] 1.1 Create Android project with Gradle Kotlin DSL build configuration
    - Set:
      - `applicationId = "com.gryffindor.smartshopping"`
      - matching `namespace`
      - `minSdk = 29`
      - `compileSdk = 35`
      - `targetSdk = 35`
    - Add A0 dependencies:
      - Compose BOM
      - Material 3
      - Navigation Compose
      - Lifecycle ViewModel Compose
      - Lifecycle Runtime Compose
      - kotlinx-coroutines-android
      - Retrofit
      - OkHttp
      - logging-interceptor
      - converter-kotlinx-serialization
      - kotlinx-serialization-json
    - Add `kotlinx.serialization` Gradle plugin
    - Configure configurable `BACKEND_BASE_URL` using `BuildConfig`
    - Enable:
      - `buildFeatures { buildConfig = true }`
    - Configure build types:
      - debug: `isDebuggable = true`
      - release: `isMinifyEnabled = true`
    - Create:
      - `src/debug/java/com/gryffindor/smartshopping/debug/`
      - optional `.gitkeep`
    - Verify:
      - `./gradlew assembleDebug` succeeds
    - _Requirements: 1.1, 1.2, 1.3, 11.1, 11.2, 11.3_

---

- [ ] 2. Core Architecture Skeleton

  - [x] 2.1 Create package structure and Application class
    - Create main source-set packages:
      - `app/`
      - `feature/`
        - `home/`
        - `shopping/`
        - `review/`
        - `travel/`
        - `checklist/`
        - `recommendation/`
      - `domain/`
      - `data/`
      - `core/`
    - Do not create a `debug/` package under `src/main`
    - Keep debug-only code under `src/debug/`
    - Create `app/SmartShoppingApp.kt`
      - Android `Application`
      - owns the `AppContainer`
    - Create `app/MainActivity.kt`
      - single Activity
      - uses `setContent`
      - hosts `AppNavGraph`
    - _Requirements: 1.3, 2.1, 2.2_

  - [x] 2.2 Create core utilities and DI container
    - Create `core/common/UiState.kt`
      - `Loading`
      - `Success<T>`
      - `Error(message)`
    - Create `core/config/AppConfig.kt`
      - placeholder for future application configuration
      - do not add Attention thresholds during A0
    - Create `core/network/NetworkConfig.kt`
      - `createRetrofit(baseUrl)` factory
      - OkHttp
      - kotlinx.serialization converter
      - `ignoreUnknownKeys = true`
    - Create `app/AppContainer.kt`
      - manual DI
      - provides all fake repository implementations
      - Retrofit/network infrastructure is defined but not actively used by repositories in A0
    - _Requirements: 10.1, 10.2, 10.3, 9.2, 9.3, 12.1_

---

- [ ] 3. Domain Layer

  - [x] 3.1 Create domain models
    - Create `domain/model/Session.kt`
      - `Session`
      - `SessionStatus`
    - Create `domain/model/Product.kt`
    - Create `domain/model/Pricing.kt`
    - Create `domain/model/Observation.kt`
      - `Observation`
      - `TriggerType`
    - Create `domain/model/ObservedProduct.kt`
    - Create `domain/model/SessionProduct.kt`
      - `SessionProduct`
      - `PurchaseState`
      - `PurchaseState` values must match Backend contract:
        - `UNSET`
        - `PURCHASED`
      - interest remains a separate `Boolean`
    - Create `domain/model/ChecklistItem.kt`
    - Create `domain/model/Recommendation.kt`
      - `Recommendation`
      - `RecommendationType`
    - Domain models:
      - contain no serialization annotations
      - contain no DTO references
      - contain no network-library types
    - _Requirements: 5.2, 4.3_

  - [x] 3.2 Create repository interfaces
    - Create `domain/repository/SessionRepository.kt`
      - `createSession(currency)`
      - `completeSession(sessionId)`
    - Create `domain/repository/ShoppingRepository.kt`
      - `getProducts(sessionId)`
      - `submitReview(sessionId, purchased, interested)`
    - Create `domain/repository/TravelRepository.kt`
      - `submitTravel(sessionId, airportCode, flightNumber, airportArrivalAt)`
    - Create `domain/repository/ChecklistRepository.kt`
      - `getRefundChecklist(sessionId)`
    - Create `domain/repository/RecommendationRepository.kt`
      - `getRecommendations(sessionId)`
    - All functions:
      - are `suspend`
      - expose only domain/primitive types
    - Preserve dependency direction:
      - `feature → domain ← data`
    - _Requirements: 5.2, 6.4, 7.2_

---

- [ ] 4. Data Contract Skeleton

  - [x] 4.1 Create DTO data classes
    - Create:
      - `SessionCreateRequestDto.kt`
      - `SessionCreateResponseDto.kt`
      - `SessionCompleteResponseDto.kt`
      - `ProductDto.kt`
      - `PricingDto.kt`
      - `ObservationDto.kt`
      - `ObservedProductDto.kt`
      - `RecognitionResponseDto.kt`
      - `ProductListResponseDto.kt`
      - `ProductListItemDto.kt`
      - `ReviewRequestDto.kt`
      - `ReviewResponseDto.kt`
      - `TravelRequestDto.kt`
      - `TravelResponseDto.kt`
      - `RefundChecklistDto.kt`
      - `ChecklistItemDto.kt`
      - `RecommendationsResponseDto.kt`
      - `RecommendationItemDto.kt`
      - `ErrorResponseDto.kt`
      - `ErrorDetailDto.kt`
    - All DTOs use `@Serializable`
    - Fields must match the approved design and Backend API contract exactly
    - Do not invent additional fields or enums
    - _Requirements: 5.1, 9.4_

  - [x] 4.2 Create Retrofit API interface and mappers
    - Create `data/remote/api/ShoppingApiService.kt`
      - define all 8 Backend endpoint method signatures
      - no actual network calls are made during A0
    - Create `data/repository/mapper/SessionMapper.kt`
    - Create `data/repository/mapper/ProductMapper.kt`
    - Create `data/repository/mapper/ChecklistMapper.kt`
    - Create `data/repository/mapper/RecommendationMapper.kt`
    - Implement DTO → Domain mapping
    - Create `data/meta/` placeholder directory
      - no DAT dependencies
      - no Camera implementation
    - _Requirements: 5.3, 5.5, 9.1, 9.2_

---

- [ ] 5. Fake Repositories

  - [x] 5.1 Implement fake repository classes
    - Create `FakeSessionRepository.kt`
      - `createSession()` returns:
        - non-empty fake `sessionId`
        - `ACTIVE` status
      - `completeSession()` simulates completion
    - Create `FakeShoppingRepository.kt`
      - returns at least 2 `SessionProduct` items
      - use Backend-aligned fixture values:
        - `purchaseState = UNSET`
        - `pricingMode = MOCK`
      - include at least one interested item using the separate `interested` boolean
    - Create `FakeTravelRepository.kt`
      - simulate delay
      - no-op submission
    - Create `FakeChecklistRepository.kt`
      - return at least:
        - 2 required items
        - 1 non-required item
    - Create `FakeRecommendationRepository.kt`
      - return at least:
        - 1 `CROSS_SELL`
        - 1 `REMINDER`
      - use Backend-aligned example reason codes:
        - `SAME_BRAND_DIFFERENT_CATEGORY`
        - `INTERESTED_NOT_PURCHASED`
    - All fake repositories include a small `delay()` so loading states are visible
    - Wire all fake implementations through `AppContainer`
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 10.2, 10.3_

---

- [ ] 6. Checkpoint — Verify Core/Data Compilation

  - [x] 6.1 Verify project compiles after Domain/Data/Fake implementation
    - Run:
      - `./gradlew assembleDebug`
    - Verify:
      - Domain layer compiles
      - DTO/API skeleton compiles
      - mappers compile
      - fake repositories compile
      - AppContainer dependency graph compiles
    - Do not require test execution yet because A0 validation tests are added later

---

- [ ] 7. ViewModels

  - [x] 7.1 Create HomeViewModel and ShoppingViewModel

    - Create `feature/home/HomeViewModel.kt`
      - `HomeUiState` contains:
        - `sessionId: String?`
        - `isSessionActive`
        - `isStarting`
        - `errorMessage`
      - `startShopping()`
        - calls `SessionRepository.createSession()`
        - stores returned `sessionId`
        - marks session active
      - `clearError()`
      - exposes `StateFlow<HomeUiState>`

    - Create `feature/shopping/ShoppingViewModel.kt`
      - `ShoppingUiState` contains:
        - products
        - session active state
      - `loadProducts(sessionId)`
      - `endShopping(sessionId)`
      - `retry()`
      - exposes `StateFlow<UiState<ShoppingUiState>>`

    - Repository calls:
      - run outside UI thread
      - handle exceptions
      - do not allow uncaught repository errors to crash the app

    - _Requirements: 6.1, 6.4, 7.1, 7.2, 7.3, 7.4, 12.1, 12.4_

  - [x] 7.2 Create ReviewViewModel and TravelViewModel

    - Create `feature/review/ReviewViewModel.kt`
      - state:
        - products
        - purchasedIds
        - interestedIds
      - actions:
        - `loadProducts(sessionId)`
        - `togglePurchased(productId)`
        - `toggleInterested(productId)`
        - `submitReview(sessionId)`
        - `retry()`

    - Enforce purchased/interested consistency:
      - when a product becomes purchased, remove it from `interestedIds`
      - purchased state takes precedence over interested state
      - submitted interested IDs must not contain purchased product IDs

    - Create `feature/travel/TravelViewModel.kt`
      - state:
        - airportCode
        - flightNumber
        - arrivalTime
        - isSubmitting
        - errorMessage
      - actions:
        - `updateAirportCode(code)`
        - `updateFlightNumber(number)`
        - `updateArrivalTime(time)`
        - `submitTravel(sessionId)`
      - depends on `TravelRepository`

    - _Requirements: 6.1, 7.1, 7.2, 7.3, 12.1, 12.4_

  - [x] 7.3 Create ChecklistViewModel and RecommendationViewModel

    - Create `feature/checklist/ChecklistViewModel.kt`
      - state:
        - items
        - checkedIds
      - actions:
        - `loadChecklist(sessionId)`
        - `toggleChecked(itemId)`
        - `retry()`

    - Create `feature/recommendation/RecommendationViewModel.kt`
      - state:
        - crossSellItems
        - reminderItems
      - actions:
        - `loadRecommendations(sessionId)`
        - `retry()`

    - All ViewModels:
      - expose `StateFlow`
      - do not expose Compose-specific types
      - depend only on domain repository interfaces
    - _Requirements: 6.1, 7.1, 7.2, 7.3, 12.1, 12.4_

---

- [ ] 8. Compose UI Screens

  - [x] 8.1 Create HomeScreen and ShoppingScreen with ProductCard

    - Create `feature/home/HomeScreen.kt`
      - display Start Shopping button
      - display loading/error state where applicable
      - call `viewModel.startShopping()`
      - observe successful session creation
      - when a non-null `sessionId` is available after successful creation:
        - invoke `onNavigateToShopping(sessionId)`

    - Create `feature/shopping/ShoppingScreen.kt`
      - display fake product list
      - display ProductCard
      - display End Shopping button
      - support Loading/Error/Success states
      - retry action where applicable

    - Create `feature/shopping/ProductCard.kt`
      - product image placeholder
      - brand
      - product name
      - retail price in KRW
      - estimated refund price in KRW
      - converted foreign-currency amount
      - instant refund eligibility
      - null-field fallback indicator

    - Use minimal Material 3 UI only
    - Do not apply final visual polish

    - _Requirements: 2.3, 4.1, 4.2, 4.3, 4.4, 12.2, 12.3_

  - [x] 8.2 Create ReviewScreen and TravelScreen

    - Create `feature/review/ReviewScreen.kt`
      - product list
      - purchased toggle
      - interested toggle
      - UI must reflect purchased-over-interested precedence
      - Confirm button
      - Loading/Error state

    - Create `feature/travel/TravelScreen.kt`
      - airport code TextField
      - flight number TextField
      - arrival time TextField
      - Submit button
      - submitting/error state

    - _Requirements: 2.3, 12.2, 12.3_

  - [x] 8.3 Create ChecklistScreen and RecommendationScreen

    - Create `feature/checklist/ChecklistScreen.kt`
      - checklist item list
      - checkbox
      - required indicator
      - Proceed button
      - Loading/Error state

    - Create `feature/recommendation/RecommendationScreen.kt`
      - Cross-Sell section
      - Reminder section
      - recommendation cards
      - Loading/Error state

    - _Requirements: 2.3, 12.2, 12.3_

---

- [ ] 9. Navigation Wiring

  - [x] 9.1 Create Routes and AppNavGraph

    - Create `app/navigation/Routes.kt`
      - HOME
      - SHOPPING
      - REVIEW
      - TRAVEL
      - CHECKLIST
      - RECOMMENDATION
      - builder functions for routes requiring `sessionId`

    - Create `app/navigation/AppNavGraph.kt`
      - `NavHost`
      - start destination = HOME
      - six composable destinations

    - Propagate the same `sessionId` through:
      - Home
      - Shopping
      - Review
      - Travel
      - Checklist
      - Recommendation

    - Wire ViewModels through `AppContainer`
      - ViewModelFactory or CreationExtras pattern

    - Verify flow:

      ```text
      Home
      → Shopping
      → Shopping Review
      → Travel Input
      → Refund Checklist
      → Airport Recommendation
      ```

    - Use standard Android back-stack behavior
    - Do not add custom navigation guards

    - Update `MainActivity.kt` to host `AppNavGraph`

    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 2.2_

---

- [ ] 10. Checkpoint — Full Compilation and Launch Verification

  - [x] 10.1 Verify application skeleton
    - Run:
      - `./gradlew assembleDebug`
    - Install/launch on emulator or Android device
    - Verify:
      - app launches without crash
      - Home screen renders
      - fake repositories are provided successfully
      - no Backend server is required

---

- [ ] 11. A0 Validation Tests

  - [ ]* 11.1 Write DTO deserialization and mapper unit tests
    - Test JSON → DTO deserialization for key DTOs:
      - `SessionCreateResponseDto`
      - `ProductListResponseDto`
      - `RefundChecklistDto`
      - `RecommendationsResponseDto`
    - Verify `ignoreUnknownKeys` accepts extra fields
    - Test key DTO → Domain mappings:
      - Session
      - Product
      - Pricing
      - Checklist
      - Recommendation
    - _Requirements: 5.3, 5.5, 9.4_

  - [ ]* 11.2 Write fake repository and key ViewModel tests
    - Fake repository verification:
      - sessionId is non-empty
      - expected fake product count
      - checklist required/non-required mix
      - CROSS_SELL and REMINDER recommendation types
    - ViewModel verification:
      - Shopping: Loading → Success
      - Checklist: Loading → Success
      - Recommendation: Loading → Success
      - purchased/interested precedence is maintained in ReviewViewModel
    - _Requirements: 8.2, 8.3, 8.4, 8.5, 12.1_

  - [x] 11.3 Verify architectural boundary constraints
    - Verify no DTO imports exist under `feature/`
    - Verify no Retrofit/OkHttp imports exist under `feature/`
    - Verify no Meta DAT dependency/import exists anywhere
    - Verify no `androidx.compose` import exists in ViewModel files
    - Verify repository interfaces live under `domain/repository/`
    - Run:
      - `./gradlew assembleDebug`
    - _Requirements: 5.4, 6.2, 6.3, 7.1_

---

- [x] 12. Final A0 Checkpoint

  - [x] 12.1 Run final build and manual acceptance flow
    - Run all implemented unit tests
    - Run:
      - `./gradlew assembleDebug`
    - Launch app on emulator or Android device

    - Verify full Fake-only flow:

      ```text
      Home
      → Shopping
      → Shopping Review
      → Travel Input
      → Refund Checklist
      → Airport Recommendation
      ```

    - Verify:
      - no Backend server is required
      - Start Shopping creates and propagates a fake `sessionId`
      - at least one fake Product Card appears on Shopping
      - Review purchase/interested state remains consistent
      - Travel submission proceeds through fake repository
      - Checklist items render
      - CROSS_SELL and REMINDER recommendations render
      - standard back navigation works
      - no runtime crash occurs

    - Verify architectural constraints:
      - no DTO imports under `feature/`
      - no networking imports under `feature/`
      - no Meta DAT dependency/import
      - no Compose types exposed by ViewModels

---

## Notes

- Tasks marked with `*` are optional and can be skipped if necessary to reach the MVP faster.
- Each task references relevant requirements for traceability.
- A0 uses Kotlin + Jetpack Compose throughout.
- Fake repositories are the only active data implementations in A0.
- Retrofit/API definitions are skeletons only and are not used for real Backend communication.
- Meta DAT integration begins in A1.
- Object Detection / Tracking / Attention Gate begin in A2.
- No property-based testing framework is required.
- `src/debug/` is excluded from release builds by the Android build system.
- The priority after A0 is to move quickly to A1/A2 and the Gen2 end-to-end integration checkpoint.

---

## Task Dependency Graph

```json
{
  "waves": [
    ["1.1"],
    ["2.1"],
    ["2.2", "3.1"],
    ["3.2", "4.1"],
    ["4.2", "5.1"],
    ["6.1"],
    ["7.1", "7.2", "7.3"],
    ["8.1", "8.2", "8.3"],
    ["9.1"],
    ["10.1"],
    ["11.1", "11.2", "11.3"],
    ["12.1"]
  ]
}
```