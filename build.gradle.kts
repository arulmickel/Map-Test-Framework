// =============================================================================
// PROJECT-LEVEL build.gradle.kts
// =============================================================================
// WHY THIS FILE EXISTS:
// This is the root build script. It declares which Gradle plugins the whole
// project needs. Think of it as the "master toolbox" — it doesn't build
// anything itself, but it makes tools available for sub-modules (like :app).
//
// KEY CONCEPT FOR INTERVIEWS:
// "Why use version catalogs or plugin blocks instead of buildscript?"
// → Modern Gradle uses type-safe plugin blocks. Apple-scale projects need
//   reproducible builds. This approach makes dependency versions explicit
//   and avoids classpath conflicts.
// =============================================================================

plugins {
    // Android Application plugin — needed to build an APK
    id("com.android.application") version "8.2.2" apply false

    // Kotlin Android plugin — compiles Kotlin code for Android
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false

    // Hilt (Dependency Injection) — makes testing possible by swapping
    // real implementations with fakes/mocks
    // WHY HILT MATTERS FOR TESTING:
    // Without DI, your ViewModel directly creates a Repository, which directly
    // creates a Database. You can't test anything in isolation.
    // With Hilt, you say "give me a Repository" and in tests, Hilt gives you
    // a FakeRepository instead. This is fundamental to testable architecture.
    id("com.google.dagger.hilt.android") version "2.50" apply false

    // KSP (Kotlin Symbol Processing) — faster alternative to kapt for
    // annotation processing (Room, Hilt). Apple-scale projects care about
    // build speed.
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
}
