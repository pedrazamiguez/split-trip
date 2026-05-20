import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    id("com.android.application")
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

android {
    compileSdk = catalog.findVersion("compileSdk").get().requiredVersion.toInt()

    defaultConfig {
        minSdk = catalog.findVersion("minSdk").get().requiredVersion.toInt()
    }

    lint {
        ignoreTestSources = true
        checkDependencies = false
        ignoreWarnings = true
        abortOnError = true
        sarifReport = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// ── Ktlint ──────────────────────────────────────────────────────────────
apply(plugin = "org.jlleitschuh.gradle.ktlint")

configure<KtlintExtension> {
    version.set(catalog.findVersion("ktlint").get().requiredVersion)
    android.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(false)
}

// ── Detekt ──────────────────────────────────────────────────────────────
apply(plugin = "io.gitlab.arturbosch.detekt")

configure<DetektExtension> {
    config.setFrom(files("${rootProject.projectDir}/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    ignoreFailures = false
}

tasks.withType<Detekt>().configureEach {
    reports {
        sarif.required.set(true)
        html.required.set(true)
        xml.required.set(false)
        txt.required.set(false)
    }
}

// ── JaCoCo ──────────────────────────────────────────────────────────────
apply(plugin = "jacoco")

configure<JacocoPluginExtension> {
    toolVersion = catalog.findVersion("jacoco").get().requiredVersion
}

@Suppress("UnstableApiUsage")
android.buildTypes {
    getByName("debug") {
        enableUnitTestCoverage = true
    }
}

tasks.register<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Generates JaCoCo coverage report for debug unit tests"
    dependsOn("testDebugUnitTest")

    classDirectories.setFrom(
        fileTree(
            layout.buildDirectory.dir(
                "intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes"
            )
        ) {
            exclude(JacocoExclusions.classExcludes)
        }
    )
    sourceDirectories.setFrom(files("src/main/kotlin", "src/main/java"))
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include(
                "jacoco/testDebugUnitTest.exec",
                "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
            )
        }
    )

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

