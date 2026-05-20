import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    id("org.jetbrains.kotlin.jvm")
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    jvmToolchain(21)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events(
            "passed",
            "skipped",
            "failed"
        )
    }
}

// ── Common test dependencies ─────────────────────────────────────────────
dependencies {
    "testImplementation"(platform(catalog.findLibrary("junit-bom").get()))
    "testImplementation"(catalog.findLibrary("junit-jupiter").get())
    "testRuntimeOnly"(catalog.findLibrary("junit-platform-launcher").get())
    "testImplementation"(catalog.findLibrary("kotlinx-coroutines-test").get())
    "testImplementation"(catalog.findLibrary("mockk").get())
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

tasks.named<Test>("test") {
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.withType<JacocoReport>().configureEach {
    dependsOn(tasks.named("test"))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
