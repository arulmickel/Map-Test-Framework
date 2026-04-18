# MapTest Framework 🗺️
## Android Maps Test Automation Framework

**Purpose:** A production-style Android Maps app with a comprehensive SDET test framework.
Built as interview preparation for Apple's SDET MapKit Engineer (Android) role.

---

## Why This Project Exists

This project demonstrates:
- **Android Maps app development** (Google Maps SDK — closest public equivalent to Apple MapKit)
- **Test automation framework design** (Page Object Model, test data builders, helpers)
- **Espresso + Compose UI testing** (the exact stack Apple uses for Android testing)
- **DSA concepts in real context** (LRU cache, graph traversal, binary search — used naturally)
- **Offline-first architecture** (Room + connectivity handling — critical for maps apps)
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

## Tech Stack & Why Each Choice

| Technology | Why |
|---|---|
| **Kotlin** | Apple's Android SDET role requires Kotlin. Industry standard. |
| **Jetpack Compose** | Modern Android UI — Apple's team tests Compose with Semantics/test tags |
| **Google Maps SDK** | Closest public equivalent to Apple MapKit on Android |
| **Room** | Offline-first caching — maps apps MUST work without network |
| **Hilt** | Dependency injection — makes testing possible by swapping real → fake |
| **Coroutines/Flow** | Async operations — location updates, network calls, DB queries |
| **Espresso** | Android UI testing framework — core skill for this role |
| **Compose UI Testing** | Testing Compose screens with semantics — explicitly in the JD |
| **MockK** | Kotlin-native mocking — cleaner than Mockito for Kotlin code |
| **JUnit 4** | Android instrumented tests still primarily use JUnit 4 |
| **Page Object Model** | Test architecture pattern — reduces duplication, improves maintenance |

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
2. Open in Android Studio (Hedgehog or newer)
3. Add your Google Maps API key in `local.properties`:
   ```
   MAPS_API_KEY=your_key_here
   ```
4. Sync Gradle and run the app
5. To run tests:
   ```bash
   # Unit tests
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
