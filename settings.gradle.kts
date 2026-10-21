// Root Gradle settings for Weekend Vibes.
// Repositories are declared once here; modules may not add their own.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "weekendvibes"
include(":app")
include(":core:domain")
include(":core:data")
