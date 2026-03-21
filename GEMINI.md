# Gemini Context: portfolio-browser

This project is a **Mobile Multiplatform GitHub Project Browser**, built using Kotlin Multiplatform (KMP) for Android and iOS. It allows users to browse and interact with projects, integrating multiple identity providers and supporting imports from GitHub.

## Project Overview

- **Purpose:** A social-media-like app for browsing developer portfolios and projects.
- **Target Platforms:** Android and iOS.
- **Architecture:** 
    - **MVI (Model-View-Intent):** State management powered by [OrbitMVI](https://github.com/orbit-mvi/orbit-mvi).
    - **Dependency Injection:** [Koin](https://insert-koin.io/).
    - **Networking:** [Ktor](https://ktor.io/) with CIO engine.
    - **Database/Auth:** Firebase Firestore and Firebase Authentication.
    - **UI:** Compose Multiplatform for Android and SwiftUI for iOS.
    - **Repository Pattern:** abstraction over GitHub API and Firestore.
- **Backend:** Firestore database and Azure Search AI vector database 

## Project Structure

- `shared/`: The core Kotlin Multiplatform module.
    - `commonMain/`: Shared logic, repositories (`GitHubProjectRepository`, `FirestoreProjectRepository`), DI (`SharedAppModule`), and MVI stores (`OrbitStore`).
    - `androidMain/`: Platform-specific implementations for Android (e.g., Firestore, Auth).
    - `iosMain/`: Platform-specific implementations for iOS.
- `androidApp/`: Android-specific application code and UI using Jetpack Compose.
- `iosApp/`: iOS-specific application code and UI using SwiftUI.
- `tools/`: Supplementary tools and configurations.
    - `firebase/`: Firebase configuration, security rules, and Cloud Functions (Python).
    - `backfill_index/`: Python scripts for database indexing/maintenance.

## Building and Running

### Prerequisites

1.  **Firebase Configuration:**
    -   Add `google-services.json` to `androidApp/src/debug/`.
2.  **Secrets:**
    -   Create a `secrets.local` file in the root directory with the following keys:
        ```properties
        githubApiKey=YOUR_GITHUB_TOKEN
        githubApiUser=YOUR_GITHUB_USERNAME
        ```
3. **Configuration:**
    -   `BuildInfo.kt` for build constants


### Local Development

- **Firestore Emulator** The Firebase emulator suite located in `tools/firebase`
- **Android Device Emulator**
    -   To run on Android virtual device use `emulator` build variant


### Commands

- **Build Project:** `./gradlew assemble`
- **Run Android App:** `./gradlew :androidApp:installDebug` (or use Android Studio)
- **Run Android App in emulator:** `./gradlew :androidApp:installEmulator` (or use Android Studio)
- **Run Shared Tests:** `./gradlew :shared:test`
- **iOS Development:**
    -   Open `iosApp/iosApp.xcodeproj` in Xcode.
    -   The shared framework is automatically built during the Xcode build process.
- **Run Firestore emulator:** `./tools/start_emulator.sh`

## Development Conventions

### MVI & Business Logic
- **Encapsulation:** ViewModels MUST NOT have direct references to OrbitMVI (e.g., `intent`, `reduce`, `subIntent`).
- **OrbitStore as Foundation:** `OrbitStore` is the base class for specialized business logic classes (e.g., `ProjectsListInteractions`, `ProjectEdition`, `AccountMerger`). These classes encapsulate all state transitions and business rules.
- **ViewModel as Coordinator (Checkpoint):** ViewModels act as high-level flow controllers. They observe state changes from one or more `OrbitStore` implementations and decide when to hand off logic to other specialized components based on specific events or exceptions.
- **Specialized Business Classes:** These classes (extending `OrbitStore`) have a single responsibility and a clear intent. They manage their own narrow state and provide a simple API for the ViewModel to trigger actions.
- **State Synchronization:** When a specialized flow completes, the ViewModel is responsible for synchronizing the result (e.g., `AccountMergeState.Success`) back to the main state (e.g., calling `loginStore.loginAs(user)`).
- **State Flow Observation:** UI (Compose/SwiftUI) should observe `stateFlow` and `sideEffectFlow` only from the ViewModel. The ViewModel bridges flows from multiple stores/business classes.

### Infrastructure & Dependencies
- **Dependency Injection:** Register all shared components in `SharedAppModule.kt`. Platform-specific implementations use `expected`/`actual` patterns or platform-specific factory functions (e.g., `getPlatformAuth()`).
- **Networking:** Use the shared Ktor `HttpClient` configured in `SharedAppModule`.
- **Data Persistence:** Use the Repository pattern to abstract over Firestore and GitHub API.
- **Paging:** Use `app.cash.paging` for consistent multiplatform pagination support.

## Roadmap & Refactoring

The project is currently transitioning to **Milestone 2**, focusing on:
-   Refactoring repositories into intent-based classes (e.g., `PagedSearch`).
-   Moving complex user flows from `OrbitStore` into dedicated flow classes (e.g., `AccountMerger`).
-   Extending external provider support.
