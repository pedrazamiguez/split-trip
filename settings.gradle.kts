pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SplitTrip"
include(":app")
include(":core")
include(":core:common")
include(":core:design-system")
include(":core:logging")
include(":data")
include(":data:firebase")
include(":data:local")
include(":data:remote")
include(":domain")
include(":features")
include(":features:activity-logging")
include(":features:authentication")
include(":features:balances")
include(":features:contributions")
include(":features:expenses")
include(":features:withdrawals")
include(":features:groups")
include(":features:subunits")
include(":features:main-entry")
include(":features:onboarding")
include(":features:profile")
include(":features:settings")
include(":konsist-tests")
