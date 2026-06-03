# AGENTS.md — SplitTrip

## Project Overview

Kotlin Android app (Jetpack Compose, Material 3) for shared travel expenses. Multi-module Clean Architecture, offline-first with Room + Firestore, Koin DI. See `.github/copilot-instructions.md` for the full technical manifesto.

## Module Structure & Visibility Rules

```
:app                    → Wires DI only. Sees everything.
:core:common            → Constants, UiText, providers (LocaleProvider, ResourceProvider)
:core:design-system     → UI components, Routes, NavigationProvider, TabGraphContributor, ScreenUiProvider, preview utils
:domain                 → Pure Kotlin: models, repository interfaces, use cases, domain services
:data                   → Repository implementations (offline-first)
:data:local             → Room DAOs, entities, DataStore
:data:firebase          → Firestore/Auth cloud data sources
:data:remote            → Retrofit (currency API)
:features:authentication → Login / auth state management
:features:balances      → Read-only balance dashboard (member balances, contribution/withdrawal history)
:features:contributions → Add contribution write-flow (standalone, non-tab)
:features:expenses      → Expense listing + add/edit expense workflow
:features:groups        → Group lifecycle (list, create, delete)
:features:main-entry    → MainScreen orchestrator (bottom nav, top bar, FAB hosting)
:features:onboarding    → Onboarding wizard
:features:profile       → User profile display + edit
:features:settings      → App settings
:features:subunits      → Subunit management lifecycle — CRUD (standalone, non-tab)
:features:withdrawals   → Add cash withdrawal write-flow (standalone, non-tab)
:features:activity-logging → (Planned) Activity log feature
```

**Strict:** Features cannot see other features or `:data`. Features only depend on `:domain` interfaces and `:core`.

## Architecture Constraints

- **ViewModels NEVER depend on Repositories.** Only inject UseCases, Mappers, and Domain Services.
- **ViewModels NEVER inject** `Context`, `LocaleProvider`, or other ViewModels.
- **Feature vs Screen pattern:** `*Feature` (orchestrator composable) holds ViewModel, collects flows, consumes `LocalTopPillController`/`LocalTabNavController`. `*Screen` is stateless — takes `UiState` + event lambdas only. See `features/profile/src/main/kotlin/.../ProfileFeature.kt` and `ProfileScreen.kt`.
- **MVI triad per screen:** `UiState` (data class, `ImmutableList`), `UiEvent` (sealed interface), `UiAction` (side-effects via `Channel`/`SharedFlow`). Never put one-shot events in `UiState`.
- **Hot flows:** Use `stateIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME, replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION), initial)`. The constants live in `core/common/.../AppConstants.kt` (5000ms / 0ms). Never hardcode. The `FLOW_REPLAY_EXPIRATION = 0` resets the replay cache to `initialValue` after the upstream stops, preventing stale-state flashes on tab re-entry.
- **UiText pattern:** ViewModels emit `UiText.StringResource(R.string.x)` — resolved to String in Feature via `asString(context)`.
- **Formatting belongs in Mappers,** not ViewModels **and not Domain Services**. Mappers receive `LocaleProvider`. See `GroupUiMapperImpl`, `ProfileUiMapperImpl`. Domain Services must NEVER contain `formatShareForInput()`, `formatAmountForDisplay()`, or any human-readable formatting method.
- **Decimal precision:** ALL decimal math in Domain Services, Repositories, and Use Cases MUST use `BigDecimal` with explicit `RoundingMode` and `scale` — NEVER `Double` or `Float`. All domain model fields that represent decimal values (e.g., `Subunit.memberShares`, exchange rates) use `BigDecimal`. Boundary serialization at the Firestore document layer uses `String` (via `toPlainString()` / safe `toBigDecimalOrNull()`) to avoid IEEE 754 floating-point precision loss that `Double` would introduce. See `SplitPreviewService` as reference.
- **Handler delegation:** When a ViewModel's `onEvent()` handles >5 event categories or exceeds ~200 lines, extract logic into plain **Event Handler** classes (NOT ViewModels) that receive `MutableStateFlow<UiState>`, `MutableSharedFlow<UiAction>`, and `CoroutineScope` via `bind()`. See `AddExpenseEventHandler`, `ConfigEventHandler`, `SplitEventHandler` in `:features:expenses`.
- **Delegate sub-pattern:** When an Event Handler exceeds ~600 lines, extract cohesive logic sections into **Delegate** classes (`*Delegate` suffix, NOT `*EventHandler`). Delegates don't implement handler interfaces or participate in `bind()`. Two patterns: lambda-based state access (async operations) or stateless/pure (calculations). See `AddOnExchangeRateDelegate`, `IntraSubunitSplitDelegate` in `:features:expenses`.
- **File size limit:** Production source files must NOT exceed **600 lines**. Enforced by a Konsist architecture test in CI. Test files are exempt.
- **Bottom padding:** All tab screens (hosted via `ScreenUiProvider`) MUST read `LocalBottomPadding.current` and apply it as bottom content padding to lists, FABs, and bottom-anchored buttons. Never hardcode `padding(bottom = 80.dp)`. See `ExpensesScreen`, `GroupsScreen`, `BalancesScreen`.
- **Compose resource access:** ALWAYS use Compose-native APIs (e.g., `stringResource(...)`, `painterResource(...)`) rather than querying resources via `LocalContext.current` (which is not configuration-aware). For non-Composable contexts (like callbacks, click handlers, or `LaunchedEffect` lambdas), resolve the resource strings in the Composable scope first and pass the resolved strings into the block.
- **Global UI orchestrators:** Global controllers and notifications (e.g., `LocalTopPillController`, `TopPillNotification`) must be declared exactly once at the root nav host level (`AppNavHost`). Feature screens and tab screens must never define, instantiate, or render nested instances of these controllers or notifications.
- **Enum centralization:** Domain enums (e.g., `AppLanguage`, `Currency`) must be the single source of truth for parsing codes, fallback defaults, and validation. Never duplicate string-matching logic (e.g., `if (code == "es" || code == "en")`) or locale fallback checks in ViewModels or presentation layers; delegate directly to the enum's helper functions.
- **Accessibility (a11y) for decorative icons:** Purely decorative images or icons (like checkmarks indicating selection in a row where the row itself already conveys status) must have `contentDescription = null` to avoid screen reader noise. Interactive icons or status icons that convey non-redundant information must use localized string resources.
- **Unused/dead ViewModel code:** Do not retain unused methods, inputs, or dependencies in ViewModels. If a sub-feature or delegated EventHandler takes over a write-flow, delete the duplicate methods and dependencies from the parent ViewModel.

## Navigation

- Routes are `const val` in `core/design-system/.../Routes.kt`.
- Two nav controllers: `LocalRootNavController` (full-screen flows) and `LocalTabNavController` (within bottom tabs). Consumed via CompositionLocals in Feature layer only.
- Notifications: `LocalTopPillController` — top pill notifications replace snackbars. Never use `Scaffold(snackbarHost=...)` in features.
- **Tab features** register as bottom tabs via `NavigationProvider` interface + Koin `bind`. See `GroupsNavigationProviderImpl`.
- **Non-tab features** (write-flows extracted into standalone modules) implement `TabGraphContributor` instead. The host tab's `NavigationProvider` injects all `TabGraphContributor` instances via Koin and calls `contributeGraph(builder)` inside `buildGraph()`. This allows runtime route merging without compile-time cross-feature dependencies. See `ContributionsTabGraphContributorImpl`, `WithdrawalsTabGraphContributorImpl`, `SubunitsTabGraphContributorImpl`.
- Tab screens define TopBar/FAB via `ScreenUiProvider` implementations (not their own Scaffold).

## Offline-First Data Flow

1. **Read:** UI observes Room Flow only. Repository subscribes to Firestore snapshots via `onStart` and reconciles Room using `@Transaction` upsert + selective delete.
2. **Write:** Save to Room first (instant UI update), then `syncScope.launch { cloudDataSource.upsert(...) }`.
3. **IDs:** Always generate `UUID` locally — never use Firestore `.add()`.
4. **Cloud subscriptions:** Track as `Job` and cancel before re-launching to prevent duplicate listeners. See `GroupRepositoryImpl`, `ExpenseRepositoryImpl`.
5. **Inject `CoroutineDispatcher`** (default `Dispatchers.IO`) for testability.

### Reusable Sync Delegates (MANDATORY for new repositories)

All offline-first coordination patterns are encapsulated in reusable utility functions in `es.pedrazamiguez.splittrip.data.sync` (`:data` module, `internal` visibility). **New repositories MUST use these delegates** instead of duplicating boilerplate.

| Delegate | Purpose |
|---|---|
| `KeyedSubscriptionTracker` | Manages keyed cloud subscription `Job`s. One active listener per key. Use for group-keyed repos. |
| `subscribeAndReconcile<T>()` | Cloud Flow → reconcile local → confirm PENDING_SYNC items. Replaces manual `subscribeToCloudChanges()` + `confirmPendingSyncXxx()`. |
| `syncCreateToCloud()` | Background sync: cloud write → `SYNCED` / `SYNC_FAILED`. Use for create + update methods. |
| `syncDeletionToCloud()` | Background sync: cloud delete. Always queues (Firestore SDK guarantees write ordering). |

**Reference:** `SubunitRepositoryImpl` (cleanest, all delegates), `CashWithdrawalRepositoryImpl` (mixed with batch ops), `GroupRepositoryImpl` (`subscribeAndReconcile` only).
**Docs:** See `wiki/offline-first-architecture.md` § "Reusable Sync Delegates" and `wiki/core-services-catalog.md` § G.

## DI Pattern (Koin)

Each feature has a set of modules wired in `app/.../FeatureModuleAggregations.kt`:
```
groupsDomainModule + groupsDataModule + groupsUiModule → groupsFeatureModules
subunitsDomainModule + subunitsDataModule + subunitsUiModule → subunitsFeatureModules
contributionsDomainModule + contributionsUiModule → contributionsFeatureModules  (no dedicated contributions data module — relies on `ContributionRepository` impl from `balancesDataModule` in :data)
withdrawalsDomainModule + withdrawalsUiModule → withdrawalsFeatureModules  (no dedicated withdrawals data module — relies on `CashWithdrawalRepository` impl from `balancesDataModule` in :data)
```
- **Tab features** UI modules declare: ViewModel, Mapper, `NavigationProvider` (factory + bind), `ScreenUiProvider` (single + bind).
- **Non-tab features** UI modules declare: ViewModel, Mapper, `TabGraphContributor` (factory + bind). They typically do **not** implement `NavigationProvider` but still register a `ScreenUiProvider` when they need a top bar or FAB (e.g. write-flow screens).
- See `features/groups/.../GroupsUiModule.kt` (tab), `features/contributions/.../ContributionsUiModule.kt` (non-tab), and `features/profile/.../ProfileUiModule.kt` as templates.

## Testing

- **Framework:** Unit tests are primarily JUnit 5 + MockK. Some legacy/Robolectric unit tests still use JUnit 4 and run via the JUnit Vintage engine. Android instrumentation tests use `AndroidJUnit4`.
- **Assertions:** NEVER use Kotlin's `assert()` — it's a no-op on Android. ALWAYS use JUnit `Assert.assertTrue(...)`, `Assert.assertEquals(...)`, etc.
- **Repository tests:** Inject `StandardTestDispatcher()` into both the repo and `runTest(testDispatcher)`. Call `advanceUntilIdle()` before assertions. See `data/src/test/.../ContributionRepositoryImplTest.kt`.
- **ViewModel tests:** Test via `onEvent()` inputs and StateFlow/SharedFlow outputs.
- **Mapper tests:** Use `LocaleProvider` fakes with fixed `Locale`. See `features/groups/src/test/.../GroupUiMapperImplTest.kt`.
- **Instrumentation tests:** Compose UI + navigation tests live in `app/src/androidTest/`. Custom `TestRunner` + `TestApp` bypass the production Koin graph. Tests use `KoinApplication` wrapper to inject mocks per-test. See `AppNavHostTest.kt` and `MainScreenTest.kt`.
- **Instrumentation test pattern:** `AppNavHost` uses `getKoin()` (composable-scoped) instead of `GlobalContext.get()`, so tests can wrap it in `KoinApplication { modules(testModule) }`.
- **Smoke tests:** Stateless Screen composables (LoginScreen, OnboardingScreen, ProfileScreen) are tested directly with different UiState configurations — no ViewModel or Koin needed. See `app/src/androidTest/.../screens/`.
- **Shared test helpers:** `FakeNavigationProvider` in `app/src/androidTest/.../helpers/` provides a minimal `NavigationProvider` that renders plain `Text("Content: $label")`, avoiding all feature dependencies.
- **Reusable test modules:** `TestModules.kt` in `app/src/androidTest/.../di/` provides `createAppNavHostTestModule()` with configurable auth/onboarding flows.
- Run unit tests: `./gradlew test`
- Run instrumentation tests: `./gradlew connectedAndroidTest`
- **CI:** Instrumentation tests run via `.github/workflows/instrumentation-tests.yml` — triggers on `main` push and `workflow_dispatch` (manual). Uses `reactivecircus/android-emulator-runner@v2` with API 30 (configurable).

## Compose Previews

- Wrap in `PreviewThemeWrapper`. Use `@PreviewLocales` (EN/ES), `@PreviewThemes` (Light/Dark), or `@PreviewComplete` (all 4).
- Use `MappedPreview` + `*PreviewHelper` in `src/debug` for domain→mapper→UiModel previews.
- Preview utilities live in `core/design-system/src/debug/.../preview/`.

## Build & Run

- JDK 21, Android SDK 36, min SDK 26.
- Place `google-services.json` in `app/`.
- API keys are Gradle properties read via `providers.gradleProperty()` — they must go in `~/.gradle/gradle.properties` (never `local.properties`, which is only for `sdk.dir`):
  - Debug builds: `OER_APP_ID_DEBUG=your_key`
  - Release builds: `OER_APP_ID_RELEASE=your_key` (or set as an environment variable `OER_APP_ID_RELEASE` — env var takes precedence on CI)
- Version managed in `version.properties` (major.minor.patch + snapshot flag).
- `./gradlew test` — unit tests. `./gradlew connectedAndroidTest` — UI tests.

## Static Analysis & Code Quality

- **Detekt** (code quality/complexity), **Ktlint** (formatting), and **CodeQL** (security) are configured in `build.gradle.kts` for all subprojects.
- **CPD** (copy-paste detection) uses the `de.aaschmid.cpd` Gradle plugin at root level. Minimum token count: 100. Reports in `build/reports/cpd/`.
- **JaCoCo** (code coverage) is configured for all subprojects. Per-module reports via `jacocoTestReport`, merged report via `jacocoMergedReport`.
- **Konsist** (architecture rule enforcement) tests live in `:konsist-tests` module. Enforces naming conventions, dependency rules, and structural patterns from this manifesto.
- Detekt config lives at `config/detekt/detekt.yml`. Ktlint rules are in `.editorconfig`.
- CI runs static analysis via `.github/workflows/static-analysis.yml` (ktlint + detekt + CPD) — parallel to and independent of `build-and-test.yml`.
- JaCoCo and Konsist run in a separate `.github/workflows/coverage-and-architecture.yml` workflow — also independent from `build-and-test.yml`.
- Detekt uses `ignoreFailures = true` locally; gating is done by GitHub Code Scanning's "Code scanning results" check (only new alerts block PRs).
- CPD uses `ignoreFailures = true` — duplications are informational, not blocking.
- Pre-commit hook runs **ktlint only** (fast). Detekt, CPD, JaCoCo, and Konsist run in CI only.
- New code must not introduce new detekt findings. Formatting must comply with ktlint / `.editorconfig`.
- See `wiki/code-quality-and-static-analysis.md` for full details.

## Naming Conventions

### Services
- **Domain service interfaces:** `*Service` (e.g., `ExpenseValidationService`, `LocalDatabaseCleanerService`)
- **Data service implementations:** `*ServiceImpl` (e.g., `LocalDatabaseCleanerServiceImpl`)
- **Calculators/Factories in domain:** use their own suffix (`*Calculator`, `*Factory`) — they are NOT services even when co-located in `domain/service/split/`. Do not rename them to `*Service`.
- **Domain converter `object`s:** use `*Converter` (e.g., `CurrencyConverter`). These are pure stateless utilities, not services.

### Mappers — Feature Layer (Presentation)
- All mapper types in `..presentation.mapper..` packages **MUST** follow the `UiMapper` naming pattern to distinguish them from data-layer mappers:
  - Interfaces and concrete-only classes end with `UiMapper`.
  - Concrete implementations in the Interface+Impl pattern end with `UiMapperImpl`.
  - ✅ `AddExpenseUiMapper`, `AddExpenseSplitUiMapper`, `AddExpenseOptionsUiMapper`, `BalancesUiMapper`
  - ✅ `GroupUiMapperImpl`, `ProfileUiMapperImpl` (implementations of `GroupUiMapper` / `ProfileUiMapper`)
  - ❌ `AddExpenseSplitMapper`, `AddExpenseOptionsMapper`
- Two valid structural patterns — pick one per mapper, do not mix:
  1. **Concrete-only** — a plain `class` with no interface. Preferred when tests instantiate the mapper directly (e.g., `AddExpenseSplitUiMapper`, `AddExpenseOptionsUiMapper`, `ExpenseUiMapper`).
  2. **Interface + Impl** — when the mapper must be faked/mocked in tests (`GroupUiMapper` → `GroupUiMapperImpl`, `ProfileUiMapper` → `ProfileUiMapperImpl`). The `Impl` lives alongside or in an `impl/` subfolder.
- Enforced by Konsist: `ArchitectureTest.NamingConventions.presentation layer mappers must end with UiMapper or UiMapperImpl (interfaces vs implementations)`.

### Mappers — Data Layer
- Data-layer mappers use **top-level extension functions** (not classes):
  - `:data:firebase` — `*DocumentMapper.kt` (e.g., `fun Expense.toDocument()`)
  - `:data:local` — `*EntityMapper.kt` (e.g., `fun Expense.toEntity()`)
  - `:data:remote` — `*DtoMapper.kt` (e.g., `fun CurrencyDto.toDomain()`)
- This is intentionally different from the class-based feature-layer pattern.

### DI Module Variable Names
- Variables inside `viewModel { }` and `factory { }` blocks **MUST** use the full class name in camelCase.
  - ✅ `val addExpenseUiMapper = get<AddExpenseUiMapper>()`
  - ✅ `val addExpenseOptionsUiMapper = get<AddExpenseOptionsUiMapper>()`
  - ❌ `val mapper = get()`, `val optionsMapper = get()`
- Constructor arguments passed to handlers/mappers **MUST** use the full descriptive parameter name that matches the class.
  - ✅ `addCashWithdrawalUiMapper = cashWithdrawalUiMapper`
  - ❌ `mapper = cashWithdrawalUiMapper`

## Service & Component Catalog (Quick Reference)

> **Full details:** See [`wiki/core-services-catalog.md`](wiki/core-services-catalog.md) for complete method signatures, parameters, and "when to use" guidance.

Before creating any new service, utility, formatter, or UI component, **check the catalog first** to avoid duplication.

### Design-System UI Components (`:core:design-system`)

| Category | Components |
|---|---|
| **Scaffold & Nav** | `FeatureScaffold`, `ExpressiveFab`, `LargeExpressiveFab`, `StickyActionBar`, `rememberScrollAwareFabVisibility`, `ScrollAwareFabContainer`, `NavigationBarIcon`, `TabGraphContributor` |
| **Layout** | `ShimmerLoadingList`, `ShimmerItemCard`, `EmptyStateView`, `FlatCard`, `SectionCard`, `AnimatedAmount`, `DeferredLoadingContainer` |
| **Input** | `StyledOutlinedTextField`, `SearchableChipSelector<T>`, `AsyncSearchableChipSelector<T>` |
| **Currency** | `CurrencyDropdown`, `AmountCurrencyCard`, `CurrencyConversionCard` |
| **Wizard** | `WizardStepLayout`, `WizardStepIndicator`, `WizardNavigationBar` |
| **Form** | `GradientButton`, `SecondaryButton`, `DestructiveButton`, `FormErrorBanner`, `FormSubmitButton` |
| **Chip** | `PassportChip` |
| **Dialog/Sheet** | `DestructiveConfirmationDialog`, `ActionBottomSheet`, `CopyableTextSheet` |
| **Transitions** | `SharedTransitionSurface`, `LocalSharedTransitionScope`, `LocalAnimatedVisibilityScope` |
| **Foundation** | `GlassmorphismDefaults`, `Modifier.horizonGlassEffect()` |

### Formatters (`:core:design-system`)

| Formatter | Key Functions |
|---|---|
| `NumberFormatter` | `String.formatNumberForDisplay()`, `String.formatRateForDisplay()`, `BigDecimal.formatForDisplay()` |
| `AmountFormatter` | `formatCurrencyAmount()`, `parseAmountToSmallestUnit()`, `formatAmountWithCurrency()`, `Expense.formatAmount()` |
| `DateFormatter` | `LocalDateTime.formatShortDate()`, `LocalDateTime.formatMediumDate()` |
| `FormattingHelper` | Injectable class wrapping all above with `LocaleProvider`. Inject into mappers. |

### Domain Services (`:domain`)

| Service | Responsibility |
|---|---|
| `ExpenseCalculatorService` | Cents conversion, proportional amounts, fair distribution, FIFO cash |
| `AddOnCalculationService` | Add-on resolution, totals, effective amounts, base cost decomposition |
| `ExchangeRateCalculationService` | Forward/inverse rate conversion, blended rates, string convenience methods |
| `RemainderDistributionService` | Proportional weight distribution, rescaling (guarantees sum == total) |
| `ExpenseSplitCalculatorFactory` | Strategy factory → `EqualSplitCalculator`, `ExactSplitCalculator`, `PercentSplitCalculator` |
| `SubunitAwareSplitService` | Two-level entity + intra-subunit splitting |
| `SplitPreviewService` | Live preview math for percentage splits (ephemeral, not authoritative) |
| `SubunitShareDistributionService` | Even/manual subunit share percentage math |
| `AddOnResolverFactory` | Strategy factory → `ExactAddOnResolver`, `PercentageAddOnResolver` |
| `ExpenseValidationService` | Title, amount, split, add-on validation |
| `SubunitValidationService` | Subunit name, members, shares, overlap validation |
| `ContributionValidationService` | Contribution amount and scope validation |
| `CashWithdrawalValidationService` | Cash withdrawal field validation |
| `EmailValidationService` | Pure Kotlin regex email validation |
| `GroupMembershipService` | Enforces user is a group member before writes |
| `CurrencyConverter` (object) | Currency conversion, amount parsing, string normalization |

### Data Layer Sync Delegates (`:data` — `internal`)

| Delegate | Purpose |
|---|---|
| `KeyedSubscriptionTracker` | Manages keyed cloud subscription `Job`s. One active listener per key. Use for group-keyed repos. |
| `subscribeAndReconcile<T>()` | Cloud Flow → reconcile local → confirm PENDING_SYNC items. Replaces manual `subscribeToCloudChanges()` + `confirmPendingSyncXxx()`. |
| `confirmPendingSync()` | PENDING_SYNC → SYNCED verification loop. Called automatically by `subscribeAndReconcile`, also available standalone. |
| `syncCreateToCloud()` | Background sync: cloud write → `SYNCED` / `SYNC_FAILED`. Use for create + update methods. |
| `syncDeletionToCloud()` | Background sync: cloud delete. Always queues (Firestore SDK guarantees write ordering). |

## AI Agent Behavior Rules (CRITICAL)

- **Internalize Core Architecture Documents:** Before proposing any action or writing code, you MUST read and internalize all project documentation, including `.github/copilot-instructions.md`, `DESIGN.md`, `AGENTS.md`, and all files in the `wiki/` directory (e.g., `add-ons-architecture.md`, `offline-first-architecture.md`, `horizon-narrative-design-language.md`, `core-services-catalog.md`). Do not rely solely on baseline knowledge.
- **No Pragmatic Patches:** You will provide clean, production-ready code. Quick hacks, "temporary" patches, or code that compromises modularity and Clean Architecture boundaries are strictly prohibited.
- **Strict Math:** Precision-sensitive math (e.g., balances, shares, exchange rates, currency amounts) MUST use `BigDecimal` with an explicit scale and rounding mode. Using `Double` or `Float` is strictly prohibited.
- **Offline-First Protocol:** Save to Room first (instant UI update) and generate UUIDs and timestamps locally, then sync to Firestore in the background using the reusable sync delegates.
- **The 600-Line Limit:** Production source files must NOT exceed 600 lines. Extract ViewModels event handlers, delegates, or component composables to keep files clean and within this hard limit.
- **Design System (Horizon Narrative):** Adhere to the "Horizon Narrative" design language. This includes using Outfit/Inter/Jakarta Sans typography, tonal layering, glassmorphism, gradients, no raw 1px borders, and applying `LocalBottomPadding` to all bottom-anchored elements on tab screens.
- **Establish a Baseline:** Before editing any files, run the full local validation check suite (`make check`) to establish the initial status of the codebase. Resolve any pre-existing violations on files you will touch before making other changes.
- **File-Size Guards:** Always verify a file's line count using `wc -l` before and after editing it. If editing will push the file near or over 600 lines, extract event handlers, delegates, or components first.
- **Commenting Policy:** Comment the *why*, never the *what*. Completely avoid/delete redundant comments that merely restate what code does.
- **Local Verification Gate (MANDATORY):** Before declaring any task, issue, or review comment done, completed, addressed, or accomplished, you MUST run `make check` locally and ensure there are 0 failures. Never leave verification for CI/CD or the user to discover.
- **NEVER push code** to any remote branch without explicit user permission.
- **NEVER create Pull Requests** without explicit user permission and confirmation of branch naming convention (see `wiki/branching-versioning-release-strategy.md`), target branch, and PR format.
- **NEVER comment on GitHub issues or PRs** without the user explicitly requesting it (note: initiating a review/triage task, such as via the review-pr skill, constitutes an explicit request/mandate to reply to all relevant comments/threads on that PR with their outcomes).
- **NEVER merge PRs or close issues** autonomously.
- **Compliance checklist before generating code:** (1) ViewModels only inject UseCases/Mappers/Services? (2) Formatting only in Mappers? (3) BigDecimal for all decimal math? (4) Handler delegation for >5 events? (5) `LocalBottomPadding` for tab screens? (6) Feature/Screen split correct? (7) MVI triad complete? (8) Hot flows with `AppConstants.FLOW_RETENTION_TIME` and `AppConstants.FLOW_REPLAY_EXPIRATION`? (9) Offline-first Room-first reads? (10) `ImmutableList` in UiState? (11) Local verification suite (`make check`) executed and passing with 0 failures?

