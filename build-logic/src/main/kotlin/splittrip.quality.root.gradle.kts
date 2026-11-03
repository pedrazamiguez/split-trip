/**
 * Root-level quality convention plugin.
 *
 * Applied only to the root project to provide:
 * - JaCoCo merged coverage report across all subprojects
 * - Access to [JacocoExclusions] shared constants
 */

apply(plugin = "jacoco")

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

configure<JacocoPluginExtension> {
    toolVersion = catalog.findVersion("jacoco").get().requiredVersion
}

tasks.register<JacocoReport>("jacocoMergedReport") {
    group = "verification"
    description = "Generates a merged JaCoCo coverage report for all subprojects"

    val reportTasks = subprojects.flatMap { sub ->
        sub.tasks.withType<JacocoReport>().matching { it.name == "jacocoTestReport" }
    }
    dependsOn(reportTasks)

    // Execution data from all subprojects
    executionData.setFrom(
        fileTree(rootProject.projectDir) {
            include(
                "**/build/jacoco/*.exec",
                "**/build/outputs/unit_test_code_coverage/**/*.exec",
            )
        }
    )

    // Source directories
    sourceDirectories.setFrom(
        subprojects.flatMap { sub ->
            listOfNotNull(
                sub.file("src/main/kotlin").takeIf { it.exists() },
                sub.file("src/main/java").takeIf { it.exists() },
            )
        }
    )

    // Class directories (with exclusions)
    classDirectories.setFrom(
        subprojects.flatMap { sub ->
            listOf(
                // JVM module classes
                fileTree(sub.layout.buildDirectory.dir("classes/kotlin/main")) {
                    exclude(JacocoExclusions.classExcludes)
                },
                // Android module classes (AGP 9.x output location)
                fileTree(
                    sub.layout.buildDirectory.dir(
                        "intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes"
                    )
                ) {
                    exclude(JacocoExclusions.classExcludes)
                },
            )
        }
    )

    reports {
        xml.required.set(true)
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/merged/html"))
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/merged/jacocoMergedReport.xml"))
    }
}

