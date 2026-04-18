# CLAUDE.md — Project Context for Claude Code

## Who I Am
- **Name:** Arul Michael Antony Felix Raja (Mickel)
- **Role:** Full Stack / Android Engineer at Digital Factory, Chicago (Willis Tower)
- **Education:** M.S. Computer Science (AI Concentration), DePaul University (Nov 2025)
- **Visa:** STEM OPT (F1), will need H-1B sponsorship in future
- **Contact:** arulmichaelantonyf@gmail.com | github.com/arulmickel | linkedin.com/in/arulmickel

## What This Project Is
This is **MapTestFramework** — a production-style Android Maps app with a comprehensive SDET test automation framework. It was built as interview preparation for **Apple's SDET MapKit Engineer (Android) role** based in Austin, TX.

**Job URL:** https://jobs.apple.com/en-us/details/200648915/sdet-mapkit-engineer-android

I have a business screen scheduled for **April 23–28, 2026** with recruiter **Anand Khanna** (akhanna22@apple.com). My cousin referred me internally.

## Interview Pipeline (Expected)
1. **Business Screen** (30 min, with Anand) — may include light technical questions
2. **Online Assessment** (HackerRank) — 30 MCQs on QA fundamentals + 1 medium coding problem
3. **Technical Round 1** — DSA (Arrays, Strings, Binary Search, HashMap) + test design patterns
4. **Technical Round 2** — Framework design, Espresso/Compose testing, debugging, scenario-based
5. **Techno-Managerial** — Behavioral (STAR), API testing, performance testing scenarios

## My Real Work Experience (use for resume bullets and interview answers)
- **Digital Factory:** Authored 25+ Espresso/Compose UI tests, integrated into GitHub Actions CI. Built offline-first geofencing (Room + WorkManager). Reduced flaky CI reruns from 10+/week to 4+/week. Revamped AWS SNS → FCM push notifications with Android 13-15 permission handling. Apollo Kotlin GraphQL (30% payload reduction). Jetpack Compose migration (15% crash reduction).
- **TCS:** Refactored Java/Spring Boot REST APIs, improved response time by 25%. Jenkins CI integration.
- **HumCen:** MVVM Android features (Kotlin/Java/XML), REST API integration, crash reduction 25%.

## Project Architecture
```
app/src/main/          → App code (Clean Architecture)
  domain/model/        → SavedLocation (with Haversine distance — DSA)
  data/local/          → Room DB (Entity, DAO, Database)
  data/remote/         → Retrofit API (PlacesApiService)
  data/repository/     → Offline-first Repository with LRU Cache
  util/                → LRULocationCache (⭐ LeetCode #146), NetworkMonitor
  di/                  → Hilt DI module
  ui/                  → Compose screens, ViewModel (UDF), TestTags

app/src/androidTest/   → Instrumented test framework
  framework/base/      → BaseTestCase (Hilt + ComposeTestRule)
  framework/pages/     → Page Objects (MapPage, FavoritesPage, NavigationPage)
  framework/helpers/   → LocationMockHelper (GPS mocking)
  framework/rules/     → MockLocationRule (JUnit Rule)
  framework/data/      → TestDataBuilder (factory with defaults)
  tests/               → SearchScreenTest, NavigationTest, OfflineModeTest

app/src/test/          → Unit tests (JVM, no device)
  util/LRUCacheTest    → ⭐ 20+ tests for LRU Cache DSA
  viewmodel/           → MapViewModel state management tests
```

## Tech Stack
Kotlin, Jetpack Compose, Google Maps SDK, Room, Hilt, Coroutines/Flow, Retrofit/OkHttp, Espresso, Compose UI Testing, MockK, JUnit 4, Truth assertions, Turbine (Flow testing), GitHub Actions CI

## Key DSA Concepts In This Project
- **LRU Cache** (HashMap + Doubly Linked List) → util/LRULocationCache.kt ✅
- **Trie / Prefix Tree** → util/LocationTrie.kt ✅ (autocomplete search)
- **Graph — BFS/DFS/Dijkstra** → util/RouteGraph.kt ✅ (route validation)
- **HashMap** → Location caching, search history, adjacency lists
- **Binary Search** → Finding nearest location from sorted list
- **Haversine Formula** → Distance calculation in SavedLocation.kt
- **Sliding Window** → Flaky test detection (future enhancement)

## What I Need Help With
1. Getting this project to compile and run in Android Studio
2. Expanding test coverage (more Espresso/Compose UI tests)
3. Building out the FakeNetworkMonitor for offline tests
4. Adding more DSA utilities (Trie for autocomplete, graph for routing)
5. LeetCode practice problems related to this project's patterns
6. Interview prep — mock questions and answers
7. Fixing any build issues, dependency conflicts, or Gradle problems

## Coding Preferences
- Explain things simply (like explaining to a 10-year-old) unless I say otherwise
- Use Kotlin for all Android code
- Include detailed comments explaining WHY, not just WHAT
- Always include interview relevance ("INTERVIEW QUESTION: ...")
- Don't set np.random.seed(1) inside functions
- Follow Clean Architecture (domain/data/ui layers)
- Use Page Object Model for all test classes
- Use TestDataBuilder for all test data (no hardcoded data in tests)

## Files That Still Need Work
- (All items completed!)

## Recently Completed
- `util/NetworkMonitor.kt` → `NetworkMonitor` interface + `RealNetworkMonitor` + `FakeNetworkMonitor(setOnline/StateFlow)`
- `util/PermissionChecker.kt` → `PermissionChecker` interface + `Real` + `Fake` with GRANTED/DENIED/SHOULD_SHOW_RATIONALE/NOT_APPLICABLE status per permission (fine location, background location, POST_NOTIFICATIONS)
- `di/NetworkModule.kt` + `di/PermissionModule.kt` → tiny focused Hilt modules so tests can `@UninstallModules` each without blast radius
- `ui/viewmodel/MapViewModel.kt` → observes both `NetworkMonitor.isOnline` and all three permission flows; surfaces them in `MapUiState` with helper props `shouldShowLocationRationale`, `locationDenied`, `notificationsDenied`
- `ui/screens/MapScreen.kt` → renders `OFFLINE_BANNER`, `PERMISSION_LOCATION_RATIONALE`, `PERMISSION_LOCATION_DENIED_CARD`, `PERMISSION_NOTIFICATIONS_RATIONALE` with independent test tags
- `tests/OfflineModeTest.kt` → `@BindValue FakeNetworkMonitor` covering start/mid-use/reconnect/flaky/favorites scenarios
- `tests/PermissionTest.kt` → `@BindValue FakePermissionChecker` covering granted/denied/rationale × fine-location/background/notifications, mixed states, precedence rules, and API-level guards via `Assume.assumeTrue`
- `AndroidManifest.xml` → added `ACCESS_BACKGROUND_LOCATION` and `POST_NOTIFICATIONS` declarations
- `data/local/LocationDaoTest.kt` → 22 integration tests using in-memory Room + Turbine: CRUD, REPLACE conflict, Flow re-emission, favorites ordering, LIKE search (name/address/case-insensitive/empty), bounding-box nearby, edge cases (poles, unicode, 500-char names)
- `util/LocationTrie.kt` → Trie (prefix tree) for autocomplete. Insert/search/remove/containsExact, case-insensitive, handles duplicate names + multiple locations per word-end
- `util/LocationTrieTest.kt` → 25 JVM unit tests covering insert/search/remove/clear, case insensitivity, unicode, progressive prefix narrowing, edge cases
- `util/RouteGraph.kt` → Weighted directed graph with BFS (fewest hops), DFS (reachability), Dijkstra (shortest distance), plus reachableFrom/isFullyConnected utilities
- `util/RouteGraphTest.kt` → 22 JVM unit tests covering all three algorithms, directed vs bidirectional edges, cycles, disconnected graphs, zero-weight edges, Austin landmarks complex scenario
