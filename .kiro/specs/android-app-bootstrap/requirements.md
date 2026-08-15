# Requirements Document

## Introduction

This document specifies the requirements for Stage A0: Android App Repository Bootstrap. The goal is to establish a buildable, navigable Android application skeleton with Jetpack Compose screens, backend DTO skeletons, fake repositories, and proper architectural layer separation. No actual backend connectivity, camera integration, or object detection is included in this stage.

## Glossary

- **App**: The Android application targeting Meta Ray-Ban Gen 2 smart glasses shopping assistance
- **Screen**: A Jetpack Compose full-screen composable representing one step in the user flow
- **ViewModel**: An AndroidX ViewModel that owns UI state and exposes actions to a Screen
- **DTO**: Data Transfer Object — a Kotlin data class mirroring the Backend JSON response structure
- **Domain_Model**: A Kotlin data class representing business concepts within the Domain layer, decoupled from Backend DTOs and UI concerns
- **Fake_Repository**: A Repository implementation that returns hardcoded or in-memory data without network calls
- **Product_Card**: A UI component displaying product name, brand, pricing, refund estimate, and converted currency amount
- **Navigation_Graph**: The Jetpack Compose Navigation graph defining screen routes and transitions
- **Presentation_Layer**: The layer containing Screens, ViewModels, and UI State — prohibited from directly accessing network clients or hardware SDK types
- **Data_Layer**: The layer containing API DTOs, repository implementations, and SDK adapters
- **Domain_Layer**: The layer containing use cases and domain models

## Requirements

### Requirement 1: Android Project Build

**User Story:** As a developer, I want the Android project to compile successfully, so that I can begin feature development on a stable codebase.

#### Acceptance Criteria

1. THE App SHALL compile without errors using the Gradle build system with Kotlin, Jetpack Compose, Material 3, Compose Navigation, and Coroutine/Flow dependencies, producing a debug APK via the `assembleDebug` task
2. THE App SHALL declare a minSdk version verified to be compatible with all declared dependencies; the specific version is determined during design
3. THE App SHALL use the recommended package structure with `app/`, `feature/`, `domain/`, `data/`, `core/`, and `debug/` top-level packages under the main source set

### Requirement 2: Device Execution

**User Story:** As a developer, I want the app to install and launch on a device or emulator, so that I can visually verify the skeleton implementation.

#### Acceptance Criteria

1. THE App SHALL install and launch on an Android device or emulator running the declared minSdk level or higher, completing startup with the main Activity in the RESUMED state without unhandled exceptions
2. WHEN the App launches, THE App SHALL display the Home screen as the NavHost start destination
3. THE App SHALL render all UI using Jetpack Compose with Material 3 components

### Requirement 3: Screen Navigation

**User Story:** As a developer, I want to navigate through all six MVP screens, so that I can verify the complete user flow is wired end-to-end.

#### Acceptance Criteria

1. THE Navigation_Graph SHALL define routes for six screens: Home, Shopping, Shopping Review, Travel Input, Refund Checklist, and Airport Recommendation
2. WHEN the user activates the start shopping action on the Home screen, THE App SHALL navigate to the Shopping screen
3. WHEN the user activates the end shopping action on the Shopping screen, THE App SHALL navigate to the Shopping Review screen
4. WHEN the user taps the confirm button on the Shopping Review screen, THE App SHALL navigate to the Travel Input screen
5. WHEN the user taps the submit button on the Travel Input screen, THE App SHALL navigate to the Refund Checklist screen
6. WHEN the user taps the proceed button on the Refund Checklist screen, THE App SHALL navigate to the Airport Recommendation screen
7. THE Navigation_Graph SHALL define a linear flow matching: Home → Shopping → Shopping Review → Travel Input → Refund Checklist → Airport Recommendation
8. WHEN the user triggers the system back gesture or back button, THE App SHALL follow standard Android back-stack behavior

### Requirement 4: Fake Product Card Display

**User Story:** As a developer, I want the Shopping screen to display at least one fake Product Card, so that I can verify the card layout and data binding work correctly.

#### Acceptance Criteria

1. WHILE a shopping session is active on the Shopping screen, THE App SHALL display at least one Product_Card with data sourced from a Fake_Repository
2. THE Product_Card SHALL display the following fields: a product image placeholder (a static icon or colored box indicating where the product image will appear), brand name, product name, retail price formatted in KRW with thousands separators and ₩ symbol, estimated refund price formatted in KRW with thousands separators and ₩ symbol, converted amount displayed in a single foreign currency (e.g., CNY) with the currency code label, and instant refund eligibility status shown as one of two values: eligible or not eligible
3. THE Product_Card SHALL derive its display data from a Domain_Model, not directly from a DTO
4. IF any required field in the Product_Card Domain_Model is missing or null, THEN THE Product_Card SHALL display a fallback indicator (e.g., dash or placeholder text) for that field instead of crashing the App

### Requirement 5: DTO and Domain Model Separation

**User Story:** As a developer, I want Backend DTOs separated from Domain and UI models, so that API contract changes do not ripple into the Presentation layer.

#### Acceptance Criteria

1. THE Data_Layer SHALL define DTO data classes in a `data/remote/` package that mirror the Backend API JSON structures for Session, ObservedProduct (including product, pricing, observation), RecognitionResponse, RefundChecklist, and Recommendation
2. THE Domain_Layer SHALL define Domain_Model data classes in a `domain/model/` package that contain no serialization annotations and no references to DTO class names, field names, or network-library types
3. THE Data_Layer SHALL provide mapping functions within the repository layer that convert each DTO to its corresponding Domain_Model; error and default-value handling strategy for nullable or missing fields is determined during design
4. THE Presentation_Layer SHALL consume only Domain_Models or UI-specific state classes; no source file outside the `data/` package SHALL import or reference any DTO class
5. IF a Backend API response contains a JSON field not yet mapped to a Domain_Model property, THEN THE Data_Layer SHALL ignore the unmapped field without causing a parsing failure, so that forward-compatible API additions do not break the client

### Requirement 6: Presentation Layer Isolation

**User Story:** As a developer, I want the Presentation layer isolated from networking and hardware SDK types, so that UI replacement on August 17 does not require modifying Data or Domain layers.

#### Acceptance Criteria

1. THE Presentation_Layer SHALL access data exclusively through ViewModel instances that expose UI state via Kotlin StateFlow
2. THE Presentation_Layer SHALL NOT import or reference any network client class (Retrofit interface, HTTP client, OkHttp types) directly; no source file in `feature/` packages SHALL contain import statements from network library packages
3. THE Presentation_Layer SHALL NOT import or reference any Meta DAT SDK type; no source file in `feature/` packages SHALL contain import statements from Meta DAT packages
4. THE ViewModel SHALL depend on repository interfaces defined in the Domain_Layer or Data_Layer interface boundary, not on concrete repository implementations

### Requirement 7: ViewModel Contract Stability

**User Story:** As a developer, I want ViewModel contracts designed for UI replaceability, so that the design team can swap Screen composables without modifying ViewModel, Domain, or Data layers.

#### Acceptance Criteria

1. THE ViewModel for each feature screen (Home, Shopping, Review, Travel, Checklist, Recommendation) SHALL expose UI state as a Kotlin StateFlow with no Compose-framework-specific types (e.g., MutableState, SnapshotStateList) in its public API
2. THE ViewModel for each feature screen SHALL expose user actions as public Kotlin functions whose parameters use only Domain-layer or primitive types, not Compose UI types or Android View references
3. THE ViewModel for screens that load data from a repository SHALL expose a sealed UI state type that includes at minimum Loading, Success, and Error subtypes
4. THE Error subtype in ViewModel UI state SHALL carry a user-presentable message so that any Screen implementation can render error feedback
5. WHEN a Screen composable is replaced with a new implementation, THE replacement Screen SHALL compile and render correctly using only the existing ViewModel public API (state StateFlow and action functions) without modifications to ViewModel, Domain, or Data layer code

### Requirement 8: Fake Repository for Offline UI Flow

**User Story:** As a developer, I want fake repositories providing hardcoded data, so that the entire UI flow works without a running Backend server.

#### Acceptance Criteria

1. THE Data_Layer SHALL provide a Fake_Repository implementation for each repository interface (Session, Shopping/Recognition, Checklist, Recommendation) that can be substituted for the real implementation via the project's dependency injection configuration without code changes to ViewModels or UI
2. WHEN session creation is requested, THE Fake_Repository for Session SHALL return a session object containing a non-empty sessionId and ACTIVE status
3. THE Fake_Repository for Shopping SHALL return at least one ObservedProduct containing product fields (productId, brand, name, category, imageUrl), pricing fields (retailPriceKrw, estimatedRefundKrw, estimatedRefundPriceKrw), and observation fields (triggerType, firstObservedAt, lastObservedAt)
4. THE Fake_Repository for Checklist SHALL return at least two checklist items with title, description, and required flag, where at least one item has required=true and at least one item has required=false
5. THE Fake_Repository for Recommendation SHALL return at least one item with type CROSS_SELL and at least one item with type REMINDER, each containing a displayable product name and recommendation type
6. WHEN the App is built and run without a Backend server, THE App SHALL render all six screens (Home, Shopping, Shopping Review, Travel Input, Refund Checklist, Airport Recommendation) using only Fake_Repository data, completing the linear navigation flow without runtime crashes or empty-state fallbacks on screens that expect data

### Requirement 9: Network Layer Skeleton

**User Story:** As a developer, I want a network client skeleton with API interface definitions, so that real backend integration in later stages requires only swapping the repository implementation.

#### Acceptance Criteria

1. THE Data_Layer SHALL define an HTTP client service interface with method signatures matching the Backend API contract endpoints: POST /sessions, POST /sessions/{id}/complete, POST /sessions/{id}/recognize (multipart/form-data), GET /sessions/{id}/products, PUT /sessions/{id}/review, PUT /sessions/{id}/travel, GET /sessions/{id}/refund-checklist, GET /sessions/{id}/recommendations
2. THE Data_Layer SHALL configure the network client with a configurable base URL, JSON serialization using camelCase field naming, and ISO 8601 UTC date handling
3. THE network client configuration SHALL NOT include any hardcoded API keys or secrets in source code, BuildConfig, or app resources
4. THE network layer SHALL define request and response DTO classes for each endpoint that match the field names and types documented in docs/BACKEND_INTEGRATION.md

### Requirement 10: Dependency Injection Setup

**User Story:** As a developer, I want a dependency injection approach established, so that repositories and use cases can be swapped between fake and real implementations cleanly.

#### Acceptance Criteria

1. THE App SHALL use a dependency injection mechanism to provide repository implementations to ViewModels, where the chosen mechanism is declared in the design document with its tradeoff rationale
2. THE dependency injection configuration SHALL allow switching between Fake_Repository and real repository implementations by changing only the DI module/provider configuration, without modifying ViewModel or Screen source code
3. THE App SHALL default to Fake_Repository implementations for all repositories during the A0 stage

### Requirement 11: Debug Configuration

**User Story:** As a developer, I want debug-specific configuration separated from release builds, so that development aids do not leak into production.

#### Acceptance Criteria

1. THE App SHALL define at least two build variants: debug and release, configured in the Gradle build script
2. THE debug build variant SHALL enable additional logging and developer tools that are disabled in release builds
3. THE App SHALL provide a `debug/` package or debug-only source set where future debug UI components can be placed, separated from release code

### Requirement 12: Error State Handling in UI

**User Story:** As a developer, I want all screens to handle standard UI states, so that the app gracefully represents loading, empty, and error conditions without crashing.

#### Acceptance Criteria

1. THE ViewModel for screens that load data from a repository (Shopping, Review, Checklist, Recommendation) SHALL expose a UI state that represents at minimum Loading, Success, and Error conditions
2. WHEN a ViewModel emits an Error state, THE Screen SHALL display a visible error message describing the failure reason without crashing the App
3. WHEN the user activates a retry affordance on an Error state screen, THE ViewModel SHALL re-execute the failed operation
4. THE App SHALL NOT crash due to a network or repository error on any screen
