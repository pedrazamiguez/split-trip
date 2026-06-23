import de.aaschmid.gradle.plugins.cpd.Cpd

// Top-level build file where you can add configuration options common to all subprojects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.crashlytics) apply false
    alias(libs.plugins.firebase.perf) apply false
    alias(libs.plugins.devtools.ksp) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.cpd)
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.version.catalog.update)
    id("splittrip.quality.root")
}

versionCatalogUpdate {
    sortByKey.set(true)
    keep {
        keepUnusedVersions.set(true)
    }
}

apply(from = "gradle/git-hooks.gradle.kts")

// ── CPD (Copy-Paste Detector) ───────────────────────────────────────────────
cpd {
    language = "kotlin"
    minimumTokenCount = 100
    isIgnoreFailures = true
}

tasks.withType<Cpd>().configureEach {
    reports {
        xml.required.set(true)
        text.required.set(true)
    }
    source = fileTree(rootProject.projectDir) {
        include(
            subprojects.map { sub ->
                sub.projectDir.relativeTo(rootProject.projectDir).path + "/src/main/kotlin/**/*.kt"
            }
        )
    }
}

// ── SonarQube ───────────────────────────────────────────────────────────────
// Compatible with SonarQube Community Edition 9.9 CE (backward-compatible).
//
// AGP 9.x WORKAROUND: As of plugin 7.2.3, AndroidUtils still references the
// legacy AppExtension / LibraryExtension APIs removed in AGP 9.x, crashing
// during task configuration for ANY Android module (app or library).
// Workaround: skip ALL subprojects from the plugin's per-module auto-discovery
// and provide aggregated source, binary, and test paths at the ROOT project
// (which has no Android plugin applied, so AndroidUtils is never invoked).
// Track: https://sonarsource.atlassian.net/browse/SCANGRADLE-287

// Collect source & binary directories from subprojects, resolved at configuration time.
val sonarSources = mutableListOf<String>()
val sonarTests = mutableListOf<String>()
val sonarBinaries = mutableListOf<String>()

subprojects {
    // Skip every subproject so the plugin never calls AndroidUtils on them.
    sonarqube { isSkipProject = true }

    // Aggregate paths for the root-level scanner.
    afterEvaluate {
        val srcMain = file("src/main/kotlin")
        val srcTest = file("src/test/kotlin")
        if (srcMain.exists()) sonarSources.add(srcMain.absolutePath)
        if (srcTest.exists()) sonarTests.add(srcTest.absolutePath)

        // Android modules: classes go to intermediates/built_in_kotlinc (AGP 9.x)
        // JVM modules: classes go to classes/kotlin/main
        val androidClasses = layout.buildDirectory.dir(
            "intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes"
        ).get().asFile
        val jvmClasses = layout.buildDirectory.dir("classes/kotlin/main").get().asFile
        when {
            plugins.hasPlugin("com.android.library") ||
                plugins.hasPlugin("com.android.application") ->
                sonarBinaries.add(androidClasses.absolutePath)

            plugins.hasPlugin("org.jetbrains.kotlin.jvm") ->
                sonarBinaries.add(jvmClasses.absolutePath)
        }
    }
}

sonarqube {
    properties {
        property("sonar.projectKey", "split-trip")
        property("sonar.projectName", "SplitTrip")

        // Skip implicit compilation — CI already compiles before running sonar.
        property("sonar.gradle.skipCompile", "true")

        // Aggregated source, test, and binary paths (populated by afterEvaluate above).
        property("sonar.sources", sonarSources.joinToString(","))
        property("sonar.tests", sonarTests.joinToString(","))
        property("sonar.java.binaries", sonarBinaries.joinToString(","))

        // Consume the merged JaCoCo XML produced by the jacocoMergedReport Gradle task.
        // The CI sonar job downloads this artifact before running ./gradlew sonar.
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "${layout.buildDirectory.get()}/reports/jacoco/merged/jacocoMergedReport.xml",
        )

        // ── Coverage exclusions (mirrors JacocoExclusions.classExcludes) ────────
        // Keep both lists in sync: JaCoCo uses class-path globs, Sonar uses source-path globs.
        property(
            "sonar.coverage.exclusions",
            listOf(
                // Generated / boilerplate
                "**/*Module.kt",
                "**/*Module\$*.kt",
                "**/R.kt",
                "**/BuildConfig.kt",
                // Preview helpers (debug source set)
                "**/*PreviewHelper*.kt",
                // Compose UI — only reachable via instrumentation tests, not JUnit
                "**/presentation/feature/**/*.kt",
                "**/presentation/screen/**/*.kt",
                "**/presentation/component/**/*.kt",
                "**/presentation/preview/**/*.kt",
                // Static Compose data lists (icons, UI constants — e.g. SettingsData.kt)
                "**/presentation/data/**/*.kt",
                // Settings UI models — sealed subclasses with @Composable lambda fields
                "**/presentation/model/SettingsItemModel*.kt",
                "**/presentation/model/SettingsSectionModel*.kt",
                // Design-system: composable components, theme, navigation primitives
                "**/designsystem/presentation/**/*.kt",
                "**/designsystem/foundation/**/*.kt",
                "**/designsystem/navigation/**/*.kt",
                "**/designsystem/permission/**/*.kt",
                // Design-system: constants-only objects — JaCoCo instruments the static class initializer
                // even though there are no executable statements (confirmed false positive).
                "**/designsystem/constant/**/*.kt",
                // Design-system: debug previews, transitions, extensions — UI-only
                "**/designsystem/preview/**/*.kt",
                "**/designsystem/transition/**/*.kt",
                "**/designsystem/extension/**/*.kt",
                // Compose navigation graphs — only testable via instrumentation
                "**/navigation/*Navigation.kt",
                "**/navigation/*NavHost.kt",
                "**/navigation/**/*NavigationProviderImpl.kt",
                "**/navigation/**/*TabGraphContributorImpl.kt",
                "**/navigation/DeepLink*.kt",
                // Koin DI module aggregation wiring (app module)
                "**/*ModuleAggregations*.kt",
                // DataStore — requires Android Context, not unit-testable
                "**/datastore/**/*.kt",
                // Preference repository impls — thin DataStore delegate wrappers (keep JacocoExclusions in sync)
                // Wildcard prefix covers OnboardingPreferenceRepositoryImpl, GroupPreferenceRepositoryImpl,
                // UserPreferenceRepositoryImpl, and any future focused splits.
                "**/*PreferenceRepositoryImpl.kt",
                // AndroidViewModel — requires Application, not unit-testable
                "**/AppVersionViewModel.kt",
                // InstallationIdViewModel — requires Firebase Installation ID (Android runtime)
                "**/InstallationIdViewModel.kt",
                // Android entry points — require Android runtime
                "**/splittrip/App.kt",
                "**/splittrip/MainActivity.kt",
                // Crashlytics logging tree — requires Android Crashlytics SDK
                "**/logging/**/*.kt",
                // Context-dependent provider implementations (require Android Context)
                "**/provider/impl/**/*.kt",
                // WorkManager — requires Android runtime
                "**/worker/**/*.kt",
                // Firebase cloud infra — Tasks.await() requires Android main looper
                "**/firestore/datasource/impl/**/*.kt",
                // Firestore document data classes (pure DTOs with Firebase Timestamp)
                "**/firestore/document/**/*.kt",
                "**/auth/service/impl/**/*.kt",
                "**/messaging/repository/impl/**/*.kt",
                "**/messaging/service/**/*.kt",
                "**/messaging/channel/**/*.kt",
                "**/installation/service/impl/**/*.kt",
                // Room DAO interfaces — abstract methods + @Transaction defaults;
                // requires instrumented tests with Room.inMemoryDatabaseBuilder()
                "**/local/dao/*.kt",
                // Room entity projection data classes — pure data holders
                "**/local/entity/SyncStatusEntry.kt",
                // Local data sources — thin Room DAO wrappers requiring Android/Robolectric
                "**/local/datasource/impl/**/*.kt",
                // Remote data sources — Retrofit HTTP (requires OkHttp/network runtime)
                "**/remote/datasource/impl/**/*.kt",
                // Database migrations — raw DDL SQL; no meaningful unit-test path
                "**/database/DatabaseMigrations.kt",
                // UiEvent, AppCheck, data-holders, mapping and interfaces
                "**/presentation/viewmodel/event/**/*.kt",
                "**/appcheck/AppCheckProviderHelper*.kt",
                "**/presentation/model/CashBalanceUiModel*.kt",
                "**/presentation/view/SettingItemView*.kt",
                "**/presentation/extensions/AddOnValueTypeExtensions*.kt",
                "**/repository/CashWithdrawalRepository*.kt",
                "**/domain/service/ReceiptExtractionService*.kt",
                "**/presentation/mapper/SubunitUiMapper*.kt",
                "**/presentation/viewmodel/strategy/ExpenseFlowStrategy.kt",
                "**/presentation/mapper/GroupUiMapper*.kt",
                "**/features/authentication/presentation/model/AuthenticationUiAction*.kt",
                "**/features/authentication/presentation/model/AuthenticationUiEvent*.kt",
                "**/domain/repository/CurrencyRepository.kt",
                "**/domain/datasource/cloud/CloudUserDataSource.kt",
            ).joinToString(","),
        )

        // ── Full scan exclusions ─────────────────────────────────────────────────
        // Files excluded here are invisible to ALL Sonar analysers (code smells,
        // bugs, duplications, AND coverage).  Use this sparingly — prefer
        // sonar.coverage.exclusions or sonar.issue.ignore.multicriteria for
        // finer-grained suppression.
        property(
            "sonar.exclusions",
            listOf(
                // Database migration scripts: raw DDL SQL strings produce false-positive
                // "define a constant instead of duplicating this literal" code smells
                // (table names / index names repeat across migrations by design).
                "**/database/DatabaseMigrations.kt",
                "**/database/migration/**/*.kt",
                // Auto-generated Tabler icon ImageVector constants: no business logic,
                // inherently duplicated template, long SVG path data strings.
                "**/designsystem/icon/**/*.kt",
            ).joinToString(","),
        )

        // ── Issue exclusions (align with detekt's Compose-aware rules) ───────────
        // Sonar's resourceKey accepts a SINGLE Ant-style path pattern per entry.
        // For multiple paths, use separate multicriteria IDs (e1, e2, …).
        // See wiki/code-quality-and-static-analysis.md § "SonarQube Exclusion System".
        property("sonar.issue.ignore.multicriteria", "e1,e2,e3,e4,e5,e6,e7,e8,e9,e10,e11,e12,e13,e14")

        // ── kotlin:S107 — Too many function parameters ─────────────────────
        // Detekt's LongParameterList ignores @Composable + default params; Sonar's
        // kotlin:S107 does not.  MVI ViewModels legitimately receive many injected
        // dependencies (UseCases, Handlers, Mappers).
        // e1: Feature-layer Compose components
        property("sonar.issue.ignore.multicriteria.e1.ruleKey", "kotlin:S107")
        property("sonar.issue.ignore.multicriteria.e1.resourceKey", "**/presentation/component/**/*.kt")
        // e2: Design-system Compose components
        property("sonar.issue.ignore.multicriteria.e2.ruleKey", "kotlin:S107")
        property("sonar.issue.ignore.multicriteria.e2.resourceKey", "**/designsystem/presentation/**/*.kt")
        // e3: MVI ViewModels (handler-delegated, DI constructor params)
        property("sonar.issue.ignore.multicriteria.e3.ruleKey", "kotlin:S107")
        property("sonar.issue.ignore.multicriteria.e3.resourceKey", "**/presentation/viewmodel/**/*.kt")
        // e11: Feature-layer Compose screens (e.g. LoginScreen, SettingsScreen)
        property("sonar.issue.ignore.multicriteria.e11.ruleKey", "kotlin:S107")
        property("sonar.issue.ignore.multicriteria.e11.resourceKey", "**/presentation/screen/**/*.kt")
        // e12: Feature-layer Compose feature orchestrators (e.g. LoginFeature, SettingsFeature)
        property("sonar.issue.ignore.multicriteria.e12.ruleKey", "kotlin:S107")
        property("sonar.issue.ignore.multicriteria.e12.resourceKey", "**/presentation/feature/**/*.kt")

        // ── kotlin:S3776 — Cognitive complexity ────────────────────────────
        // Compose builder DSL functions exceed the threshold structurally, not
        // logically.  Detekt's CognitiveComplexMethod (threshold 15) already gates
        // via Code Scanning; Sonar duplicates the finding without Compose awareness.
        // e4: Feature-layer Compose components
        property("sonar.issue.ignore.multicriteria.e4.ruleKey", "kotlin:S3776")
        property("sonar.issue.ignore.multicriteria.e4.resourceKey", "**/presentation/component/**/*.kt")
        // e5: Design-system Compose components
        property("sonar.issue.ignore.multicriteria.e5.ruleKey", "kotlin:S3776")
        property("sonar.issue.ignore.multicriteria.e5.resourceKey", "**/designsystem/presentation/**/*.kt")
        // e6: Navigation host (Compose DSL with auth/onboarding branching)
        property("sonar.issue.ignore.multicriteria.e6.ruleKey", "kotlin:S3776")
        property("sonar.issue.ignore.multicriteria.e6.resourceKey", "**/navigation/**/*.kt")
        // e13: Feature-layer Compose screens (complex UI tree structure)
        property("sonar.issue.ignore.multicriteria.e13.ruleKey", "kotlin:S3776")
        property("sonar.issue.ignore.multicriteria.e13.resourceKey", "**/presentation/screen/**/*.kt")
        // e14: Feature-layer Compose feature orchestrators (branching/loading UI logic)
        property("sonar.issue.ignore.multicriteria.e14.ruleKey", "kotlin:S3776")
        property("sonar.issue.ignore.multicriteria.e14.resourceKey", "**/presentation/feature/**/*.kt")

        // ── kotlin:S1479 — Too many "when" clauses ─────────────────────────
        // MVI ViewModels route sealed-interface events via exhaustive `when`.
        // Clause count is inherent to the event contract, not accidental complexity.
        // e7: MVI ViewModels
        property("sonar.issue.ignore.multicriteria.e7.ruleKey", "kotlin:S1479")
        property("sonar.issue.ignore.multicriteria.e7.resourceKey", "**/presentation/viewmodel/**/*.kt")

        // ── kotlin:S1481 — Unused local variable (false positives) ─────────
        // Sonar's Kotlin analyzer misreports destructuring declarations,
        // Compose remember{} blocks, and lambda-scoped variables as unused.
        // All 15 flagged instances were verified as false positives (see #786).
        // Broad scope; narrow once SonarSource fixes the analyzer.
        // e8: All Kotlin sources
        property("sonar.issue.ignore.multicriteria.e8.ruleKey", "kotlin:S1481")
        property("sonar.issue.ignore.multicriteria.e8.resourceKey", "**/*.kt")

        // ── kotlin:S1135 — TODO / FIXME tags ───────────────────────────────
        // Navigation host contains tracked TODOs (e.g., splash screen → #787).
        // e9: Navigation files
        property("sonar.issue.ignore.multicriteria.e9.ruleKey", "kotlin:S1135")
        property("sonar.issue.ignore.multicriteria.e9.resourceKey", "**/navigation/**/*.kt")

        // ── kotlin:S1479 — Too many "when" clauses (design-system extensions) ──
        // CurrencyExtensions.kt maps the Currency enum to Android string resource IDs
        // via an exhaustive `when` expression. Each branch is a single constant lookup;
        // there is no procedural logic. The `when` form is intentional: the Kotlin
        // compiler enforces exhaustiveness at compile time, catching missing branches
        // whenever a new Currency entry is added. The clause count will grow with the
        // supported currency list — refactoring to a Map would lose that guarantee.
        // e10: Design-system extension functions
        property("sonar.issue.ignore.multicriteria.e10.ruleKey", "kotlin:S1479")
        property("sonar.issue.ignore.multicriteria.e10.resourceKey", "**/designsystem/extension/**/*.kt")
        // ── Duplication exclusions ───────────────────────────────────────────────
        // Sonar's own CPD runs independently of the Gradle CPD plugin and has a lower
        // threshold. Compose's slot API / padding-parameter patterns produce structural
        // repetition that isn't meaningful duplication — exclude the UI layer.
        property(
            "sonar.cpd.exclusions",
            listOf(
                "**/*Module.kt",
                "**/presentation/component/**/*.kt",
                "**/presentation/screen/**/*.kt",
                "**/presentation/feature/**/*.kt",
                "**/designsystem/presentation/component/**/*.kt",
                // Auto-generated Tabler icon ImageVector constants (template repetition)
                "**/designsystem/icon/**/*.kt",
            ).joinToString(","),
        )
    }
}

tasks.register<Exec>("generateAndaluzStrings") {
    group = "localization"
    description = "Automatically generates Andaluz string resources from Spanish strings.xml."
    val pythonPath = project.file("/opt/homebrew/bin/python3").let {
        if (it.exists()) it.absolutePath else "python3"
    }
    commandLine(pythonPath, "${rootDir}/scripts/generate_andaluz_strings.py")
}
