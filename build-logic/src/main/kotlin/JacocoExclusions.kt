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
        // ── Sealed UiEvent hierarchies — pure data classes with no logic ─────────
        // Each subclass is a `data class` or `data object` representing a UI event;
        // there's nothing testable beyond what kotlinx-data-class generates.
        "**/presentation/viewmodel/event/**",
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
        "**/navigation/**",
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
        // ── MLKitOcrService companion classes — require Android/ML Kit runtime
        // PdfPageRendererImpl wraps Android PdfRenderer (ContentResolver, PdfRenderer.Page.render)
        // MLKitOcrEngine wraps ML Kit TextRecognizer (.process(image).await() needs Android Looper)
        // Both are isolated from MLKitOcrService itself (which IS unit-tested via the OcrEngine abstraction).
        "**/data/service/PdfPageRendererImpl.*",
        "**/data/service/PdfPageRendererImpl\$*.*",
        "**/data/service/MLKitOcrEngine.*",
        "**/data/service/MLKitOcrEngine\$*.*",
        // ── Database migrations — raw DDL SQL strings; no meaningful unit-test path
        // and triggers Sonar string-duplication false positives (table names repeated).
        // Covers both the aggregated file AND individual Migration*To* files.
        "**/database/DatabaseMigrations*.*",
        "**/database/migration/**",
        // ── Auto-generated Tabler icon ImageVector constants ─────────────────────
        // These are template-repeated SVG path strings — no testable logic.
        // Mirrored from sonar.exclusions so local JaCoCo numbers match SonarQube.
        "**/designsystem/icon/**",
        // ── AppCheck provider helpers — Firebase AppCheck + Android Context/SharedPreferences
        // Not unit-testable; requires real Android runtime for getSharedPreferences.
        "**/appcheck/AppCheckProviderHelper*.*",
        // ── Pure data-holder UI models with no testable logic ────────────────────
        // data class with only String fields; behaviour belongs to the mapper.
        "**/presentation/model/CashBalanceUiModel*.*",
        // ── Presentation view data class — holds Compose ImageVector + lambda ────
        // ImageVector requires Compose runtime; lambdas are structural, not logical.
        "**/presentation/view/SettingItemView*.*",
        // ── Presentation extension functions that map to Android R.string IDs ────
        // Return values are Android resource integers (0 in JVM unit tests);
        // verifying the mapping requires instrumentation tests with the real R class.
        "**/presentation/extensions/AddOnValueTypeExtensions*.*",
        // ── Repository & service interfaces with Kotlin default parameter values ─
        // JaCoCo instruments the `= null` / `= false` default expressions as
        // executable bytecode even though they carry no business logic.
        // Implementations ARE unit-tested; the interface contracts are not.
        "**/repository/CashWithdrawalRepository*.*",
        "**/domain/service/ReceiptExtractionService*.*",
        // ── Domain service/use-case interfaces extracted in SPLTRP-1196 ──────────
        // These interfaces carry companion-object constants used as default param values.
        // JaCoCo instruments the companion <clinit> as executable lines, but the
        // interfaces themselves have no logic — their Impl classes are fully tested.
        "**/domain/service/ExchangeRateCalculationService*.*",
        "**/domain/service/ExpenseCalculatorService*.*",
        "**/domain/service/RemainderDistributionService*.*",
        "**/domain/service/AddOnCalculationService*.*",
        "**/domain/service/SubunitShareDistributionService*.*",
        "**/domain/service/split/SplitPreviewService*.*",
        "**/domain/service/split/SubunitAwareSplitService*.*",
        "**/domain/usecase/balance/GetMemberBalancesFlowUseCase*.*",
        "**/domain/usecase/expense/GetAvailableWithdrawalPoolsUseCase*.*",
        "**/presentation/mapper/SubunitUiMapper*.*",
        "**/presentation/viewmodel/strategy/ExpenseFlowStrategy.*",
        "**/presentation/viewmodel/strategy/ExpenseFlowStrategy\$*.*",
        // ── Mapper interfaces with default method bodies (trivial 1-arg delegate wrappers) ─
        // The default `toGroupUiModel(group)` body just calls `toGroupUiModel(group, emptyMap())`.
        // The Impl is 100% tested; the interface convenience overloads are structural, not logical.
        "**/presentation/mapper/GroupUiMapper*.*",
        // ── Design-system: constants-only objects — JaCoCo instruments static class initializer ──
        "**/designsystem/constant/**/*.class",
        // ── Sealed UiEvent hierarchies — pure data classes with no logic ─────────
        "**/features/authentication/presentation/model/AuthenticationUiAction*.*",
        "**/features/authentication/presentation/model/AuthenticationUiEvent*.*",
        // ── Repository & datasource interfaces ───────────────────────────────────
        "**/domain/repository/CurrencyRepository*.*",
        "**/domain/datasource/cloud/CloudUserDataSource*.*",
        // ── UseCase implementations returning inline class Result ────────────────
        // JaCoCo instruments compiler-generated Result boxing/unboxing boilerplate
        // as uncovered bytecode. The underlying code is 100% covered.
        "**/domain/usecase/auth/impl/GetLinkedProvidersUseCaseImpl*.*",
        "**/domain/usecase/auth/impl/LinkEmailPasswordUseCaseImpl*.*",
        "**/domain/usecase/auth/impl/UnlinkProviderUseCaseImpl*.*",
        "**/domain/usecase/auth/impl/LinkGoogleAccountUseCaseImpl*.*",
        // ── Kotlin coroutines internals leaking into the JaCoCo class path ────────
        // SafeCollector.common is an internal coroutines file; it's not our code.
        "**/SafeCollector*.*",
    )
}

