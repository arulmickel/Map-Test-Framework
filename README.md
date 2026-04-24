# MapTest Framework
## Android Maps Test Automation Framework

**Purpose:** A production-style Android Maps app paired with a comprehensive SDET test framework.
Built as interview preparation for Apple's SDET MapKit Engineer (Android) role.

---

## Running the App

The app launches without a Google Maps API key - when `MAPS_API_KEY` is empty in `local.properties`, the UI renders a fallback surface instead of crashing. This is intentional: an SDET framework should be runnable on any machine without secret provisioning.

![App running on emulator with fallback UI](docs/screenshots/app-running-fallback.png)

What the screenshot proves:
- **Graceful degradation** - `MapScreen` checks `BuildConfig.MAPS_API_KEY.isBlank()` and swaps the `GoogleMap` composable for `MapFallbackSurface` instead of throwing on a missing key.
- **Permission state handling** - the rationale card with Allow / Not now / Open Settings is the `PERMISSION_LOCATION_DENIED_CARD` branch firing.
- **Compose navigation** - Map / Favorites bottom bar is wired through `NavHost` with Hilt-injected ViewModels.
- **Clean install** - APK builds with AGP 8.2.2 + Gradle 8.13 + Kotlin 1.9.22 + KSP, no manifest merger errors.

To enable the real map, drop a key into `local.properties`:
```
MAPS_API_KEY=AIza...
```
No code changes needed - the key flows from `local.properties` → `buildConfigField` + `manifestPlaceholders` → `MapScreen` at runtime.

---

## Why This Project Exists

This project demonstrates:
- **Android Maps app development** (Google Maps SDK - closest public equivalent to Apple MapKit)
- **Test automation framework design** (Page Object Model, test data builders, helpers)
- **Espresso + Compose UI testing** (the exact stack Apple uses for Android testing)
- **DSA concepts in real context** (LRU cache, graph traversal, binary search - used naturally)
- **Offline-first architecture** (Room + connectivity handling - critical for maps apps)
- **CI readiness** (GitHub Actions integration, stable selectors, flake reduction)

---

## Architecture Overview

```
┌─────────────────────────────────────────────┐
│                    UI Layer                  │
│  MapScreen | SearchScreen | FavoritesScreen  │
│         (Jetpack Compose + Test Tags)        │
├─────────────────────────────────────────────┤
│               ViewModel Layer                │
│     MapViewModel (StateFlow + UDF)           │
├─────────────────────────────────────────────┤
│              Repository Layer                │
│   LocationRepository (single source of truth)│
├──────────────────┬──────────────────────────┤
│   Local (Room)   │   Remote (Retrofit)       │
│   LocationDao    │   PlacesApiService        │
│   LRU Cache      │                           │
└──────────────────┴──────────────────────────┘
```

## Test Architecture

```
┌─────────────────────────────────────────────┐
│              Test Layer                       │
├─────────────────────────────────────────────┤
│  tests/           → Actual test classes       │
│  framework/                                   │
│    ├── base/      → BaseTestCase              │
│    ├── pages/     → Page Objects (POM)        │
│    ├── helpers/   → Location, Network, Perms  │
│    ├── data/      → Test Data Builders        │
│    └── rules/     → Custom JUnit Rules        │
├─────────────────────────────────────────────┤
│  Unit Tests (test/)                           │
│    ├── ViewModel tests                        │
│    ├── Repository tests                       │
│    ├── LRU Cache tests                        │
│    └── Route Validator tests                  │
└─────────────────────────────────────────────┘
```

---

## SDET Test Strategy

The framework is structured around three principles a real SDET role cares about:

**1. Stable selectors over fragile ones.** Compose `testTag`s and resource IDs only - no text-based matching, no XPath. When a designer changes copy, tests don't break.

**2. Test doubles via DI, not mocks at the call site.** Hilt's `@UninstallModules` + `@BindValue` swaps the real `LocationRepository` for a fake at the module boundary. ViewModels and screens never know they're being tested. This is how Apple's Android team scales tests across hundreds of features.

**3. Determinism by construction.** `animationsDisabled = true` in `testOptions`, in-memory Room for DAO tests, `MockWebServer` for network, `kotlinx-coroutines-test` for coroutines. No `Thread.sleep`, no flaky waits.

| Layer | Tool | Why this tool |
|---|---|---|
| Unit (JVM) | JUnit 4 + MockK + Truth + Turbine | Fast, Kotlin-native, Flow-aware |
| Integration (DB) | Room in-memory + JUnit | Real SQL, zero device dependency |
| Network | OkHttp `MockWebServer` | Real HTTP stack, deterministic responses |
| UI (instrumented) | Compose UI Test + Espresso + Hilt test rules | First-class Compose semantics + Hilt injection |
| Page Objects | Custom POM in `framework/pages/` | Hides selectors, exposes intent |

---

## Tech Stack & Why Each Choice

| Technology | Why |
|---|---|
| **Kotlin** | Apple's Android SDET role requires Kotlin. Industry standard. |
| **Jetpack Compose** | Modern Android UI - Apple's team tests Compose with Semantics/test tags |
| **Google Maps SDK** | Closest public equivalent to Apple MapKit on Android |
| **Room** | Offline-first caching - maps apps MUST work without network |
| **Hilt** | Dependency injection - makes testing possible by swapping real → fake |
| **Coroutines/Flow** | Async operations - location updates, network calls, DB queries |
| **Espresso** | Android UI testing framework - core skill for this role |
| **Compose UI Testing** | Testing Compose screens with semantics - explicitly in the JD |
| **MockK** | Kotlin-native mocking - cleaner than Mockito for Kotlin code |
| **JUnit 4** | Android instrumented tests still primarily use JUnit 4 |
| **Page Object Model** | Test architecture pattern - reduces duplication, improves maintenance |

---

## DSA Concepts Used Naturally

| DSA Concept | Where It's Used | LeetCode Pattern |
|---|---|---|
| **HashMap** | Location caching, search history | Two Sum, frequency counting |
| **LRU Cache** | Map tile/location cache (LinkedHashMap + DLL) | LeetCode #146 |
| **Binary Search** | Finding nearest location from sorted list | Search in sorted array |
| **BFS/DFS** | Route pathfinding between locations | Graph traversal |
| **Queue** | Location update event processing | BFS, task scheduling |
| **Sliding Window** | Flaky test detection over recent runs | Subarray problems |
| **Sorting** | Ordering locations by distance | Merge sort, comparators |
| **Trie** | Location search autocomplete | Prefix matching |

---

## Setup Instructions

1. Clone this repo
2. Open in Android Studio (Hedgehog or newer - bundles JDK 17, which AGP 8.2 requires)
3. (Optional) Add a Google Maps API key in `local.properties` - leave blank to use the fallback UI:
   ```
   MAPS_API_KEY=your_key_here
   ```
4. Sync Gradle and run the app on an API 26+ emulator
5. To run tests:
   ```bash
   # Unit tests (JVM, fast)
   ./gradlew test

   # Instrumented tests (needs emulator/device)
   ./gradlew connectedAndroidTest
   ```

---

## Key Learning Paths

### For Interview Prep:
1. Start with `util/LRULocationCache.kt` → understand the DSA
2. Read `framework/pages/` → understand Page Object Model
3. Read `framework/helpers/` → understand test infrastructure
4. Read `tests/` → understand how everything comes together

### For Understanding Architecture:
1. Start with `domain/model/` → data models
2. Read `data/` → how data flows (local ↔ remote)
3. Read `ui/viewmodel/` → state management
4. Read `ui/screens/` → Compose UI with test tags

---

## Author
**Arul Michael Antony Felix Raja**
GitHub: [arulmickel](https://github.com/arulmickel)
