// =============================================================================
// settings.gradle.kts
// =============================================================================
// This file tells Gradle which modules exist in the project and where to
// find plugin/dependency repositories.
// =============================================================================

pluginManagement {
    repositories {
        google()          // Android-specific plugins (AGP, etc.)
        mavenCentral()    // Most open-source libraries
        gradlePluginPortal() // Gradle community plugins
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MapTestFramework"
include(":app")
