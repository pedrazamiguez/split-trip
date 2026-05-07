/**
 * Single source of truth for JaCoCo exclusions — used by per-module reports AND
 * jacocoMergedReport. Keeping one list avoids the two getting out of sync.
 */
object JacocoExclusions {

    val classExcludes = listOf(
        // Android generated
        "**/R.class",
        "**/R\$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        // Koin DI modules (hand-written, not business logic)
        "**/*Module.*",
        "**/*Module\$*.*",
        // Koin DI modules — Kotlin top-level function files (compile to *ModuleKt.class)
        "**/*ModuleKt.*",
        "**/*ModuleKt\$*.*",
        // Koin DI module aggregation wiring (app module)
        "**/*ModuleAggregations*.*",
        // Compose generated
        "**/*ComposableSingletons*.*",
        // Room generated
        "**/*_Impl.*",
        "**/*Dao_Impl.*",
        // Room generated — anonymous/numbered inner classes inside _Impl
        "**/*_Impl\$*.*",
        // Preview helpers (debug source set)
        "**/*PreviewHelper*.*",
        // Sealed/data class companion objects
        "**/*\$Companion.*",
        // ── Compose UI — only reachable via instrumentation tests, not JUnit ──────
        // Feature orchestrators (hold NavController / ViewModel, not unit-testable)
        "**/presentation/feature/**",
        // Stateless screen composables + ScreenUiProvider impls
        "**/presentation/screen/**",
        // Reusable composable components
        "**/presentation/component/**",
        // Preview files (debug source set; PreviewHelper already excluded above)
        "**/presentation/preview/**",
        // Static Compose data lists (icons, UI constants — e.g. SettingsData.kt)
        "**/presentation/data/**",
        // Design-system: shared composable components, theme, navigation primitives
        "**/designsystem/presentation/**",
        "**/designsystem/foundation/**",
        "**/designsystem/navigation/**",
        "**/designsystem/permission/**",
        // Design-system: debug-only preview utilities
        "**/designsystem/preview/**",
        // Design-system: Compose transitions (SharedElement API) — UI-only
        "**/designsystem/transition/**",
        // Design-system: Compose extension functions (NavGraph, Currency display) — UI-only
        "**/designsystem/extension/**",
        // ── Compose navigation graphs — only testable via instrumentation ─────────
        "**/navigation/*NavigationKt.*",
        "**/navigation/*NavigationKt\$*.*",
        "**/navigation/*NavHostKt.*",
        "**/navigation/*NavHostKt\$*.*",
        "**/navigation/*NavigationProviderImpl.*",
        "**/navigation/*NavigationProviderImpl\$*.*",
        "**/navigation/**/*NavigationProviderImpl.*",
        "**/navigation/**/*NavigationProviderImpl\$*.*",
        // Deep-link utilities (Compose navigation wiring)
        "**/navigation/DeepLink*.*",
        // Tab graph contributor implementations (trivial delegation to NavGraphBuilder)
        "**/navigation/**/*TabGraphContributorImpl.*",
        "**/navigation/**/*TabGraphContributorImpl\$*.*",
        // ── DataStore — requires Android Context, not unit-testable ──────────────
        "**/datastore/**",
        // Preference repository impls — thin DataStore delegate wrappers (DataStore excluded above)
        // Wildcard prefix covers OnboardingPreferenceRepositoryImpl, GroupPreferenceRepositoryImpl,
        // UserPreferenceRepositoryImpl, and any future focused splits.
        "**/*PreferenceRepositoryImpl.*",
        // AndroidViewModel — requires Application, not unit-testable
        "**/AppVersionViewModel.*",
        "**/AppVersionViewModel\$*.*",
        // InstallationIdViewModel — requires Firebase Installation ID (Android runtime)
        "**/InstallationIdViewModel.*",
        "**/InstallationIdViewModel\$*.*",
        // ── Android entry points — require Android runtime ───────────────────────
        "**/splittrip/App.*",
        "**/splittrip/App\$*.*",
        "**/splittrip/MainActivity.*",
        "**/splittrip/MainActivity\$*.*",
        // Crashlytics logging tree — requires Android Crashlytics SDK
        "**/logging/**",
        // Context-dependent provider implementations (require Android Context)
        "**/provider/impl/**",
        // ── WorkManager — requires Android runtime ──────────────────────────────
        "**/worker/**",
        // ── Firebase cloud infra — suspend functions use Tasks.await() which requires
        // Android's main looper, making them untestable with pure JVM unit tests.
        // Tested via integration/instrumentation tests instead.
        "**/firestore/datasource/impl/**",
        // Firestore document data classes (pure DTOs with Firebase Timestamp/DocumentReference)
        "**/firestore/document/**",
        "**/auth/service/impl/**",
        "**/messaging/repository/impl/**",
        "**/installation/service/impl/**",
        // FCM messaging service + notification channels (require Android runtime)
        "**/messaging/service/**",
        "**/messaging/channel/**",
        // ── Local data sources — thin Room DAO wrappers requiring Android/Robolectric
        "**/local/datasource/impl/**",
        // Room DAO interfaces — abstract; generated _Impl already excluded above.
        // @Transaction default methods require instrumented tests with inMemoryDatabaseBuilder.
        "**/local/dao/*Dao.*",
        "**/local/dao/*Dao\$*.*",
        // Room entity projection data classes — pure data holders with no testable logic
        "**/local/entity/SyncStatusEntry*.*",
        // ── Remote data sources — Retrofit HTTP (requires OkHttp/network runtime)
        "**/remote/datasource/impl/**",
        // ── Settings UI models — sealed subclasses with @Composable lambda fields
        "**/presentation/model/SettingsItemModel*.*",
        "**/presentation/model/SettingsSectionModel*.*",
        // ── Compiler-generated lambda inner classes from Repository implementations
        // (e.g., CashWithdrawalRepositoryImpl$updateRemainingAmounts$2)
        "**/*RepositoryImpl\$*.*",
        // ── Database migrations — raw DDL SQL strings; no meaningful unit-test path
        // and triggers Sonar string-duplication false positives (table names repeated).
        // Covers both the aggregated file AND individual Migration*To* files.
        "**/database/DatabaseMigrations*.*",
        "**/database/migration/**",
        // ── Auto-generated Tabler icon ImageVector constants ─────────────────────
        // These are template-repeated SVG path strings — no testable logic.
        // Mirrored from sonar.exclusions so local JaCoCo numbers match SonarQube.
        "**/designsystem/icon/**",
    )
}

