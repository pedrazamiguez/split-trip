# AGENTS.md — SplitTrip

> [!IMPORTANT]
> **Source of Truth:** This file is the single source of truth for architectural constraints and agent behaviors.

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
:features:settlements   → "Tu posición" / "Your position" screen & settlement consensus sub-flow (standalone, non-tab)
:features:subunits      → Subunit management lifecycle — CRUD (standalone, non-tab)
:features:withdrawals   → Add cash withdrawal write-flow (standalone, non-tab)
```

**Strict:** Features cannot see other features or `:data`. Features only depend on `:domain` interfaces and `:core`.

## Architecture Constraints

## ViewModel Rules

ViewModels ONLY inject UseCases, Mappers, and Domain Services. NEVER inject `Context`, `LocaleProvider`, Repositories, or other ViewModels.

## MVI Triad

Every screen gets `UiState` (ImmutableList), `UiEvent` (sealed interface), and `UiAction` (side-effects via Channel/SharedFlow). Never put one-shot events in `UiState`.

## Formatting in Mappers

Formatting belongs in Mappers, not ViewModels and not Domain Services. Mappers receive `LocaleProvider`. Domain Services must NEVER contain `formatShareForInput()`, `formatAmountForDisplay()`, or any human-readable formatting method.

## Big Decimal Math

All decimal math MUST use `BigDecimal` with explicit scale + RoundingMode. `Double` or `Float` are strictly prohibited to prevent IEEE 754 precision loss. Boundary serialization at the Firestore document layer must use `String` (via `toPlainString()` / `toBigDecimalOrNull()`).

## File Size Limit

Production source files MUST NOT exceed 600 lines. This is enforced by a Konsist architecture test. Extract handlers/delegates/components before adding code if approaching the limit. Always check file sizes with `wc -l`.

## Enum Centralization

Domain enums (e.g., `AppLanguage`, `Currency`) must be the single source of truth for parsing codes, fallback defaults, and validation. Never duplicate string-matching logic or locale fallback checks in ViewModels or presentation layers.

## Single Composable Per File

Every production Kotlin file under feature `presentation` packages that contains a `@Composable` function must define exactly one top-level `@Composable` function. The name of the Composable function must match the file name.

## Feature vs Screen Pattern

`*Feature` (orchestrator composable) holds ViewModel, collects flows, consumes `LocalTopPillController`/`LocalTabNavController`. `*Screen` is stateless — takes `UiState` + event lambdas only.

## No Fully Qualified Names (No FQN)

NEVER use Fully Qualified Names (FQN) inside production or test source code (e.g., `java.util.UUID.randomUUID()`, `com.google.firebase.Timestamp.now()`, `kotlinx.coroutines.Dispatchers.IO`).
ALWAYS import all classes, interfaces, objects, and types at the top of the file using standard `import` statements. This is strictly enforced by Konsist rule `Production code must not use Fully Qualified Names (FQN) for classes or objects`.

# UI Clicks & Navigation — Debouncing

> [!IMPORTANT]
> **Always use debounced modifiers for UI navigation or expensive side-effects.**

To prevent multiple rapid navigations or API calls from "double tapping" UI components:
- NEVER use standard `Modifier.clickable` or `Modifier.combinedClickable` directly on list items, action buttons, or FABs.
- ALWAYS use `Modifier.debouncedClickable` and `Modifier.debouncedCombinedClickable` from `es.pedrazamiguez.splittrip.core.designsystem.extension.ModifierExtensions`.

**Why:** Compose can process multiple touch events before a navigation transition completes, pushing the destination route multiple times onto the back stack.

### Examples

```kotlin
// ❌ BAD
Modifier.clickable { navigate() }

// ✅ GOOD
Modifier.debouncedClickable { navigate() }
```

```kotlin
// ❌ BAD
Modifier.combinedClickable(
    onClick = { navigateToDetail() },
    onLongClick = { showOptions() }
)

// ✅ GOOD
Modifier.debouncedCombinedClickable(
    onClick = { navigateToDetail() },
    onLongClick = { showOptions() }
)
```

## Navigation

- Routes are `const val` in `core/design-system/.../Routes.kt`.
- Two nav controllers: `LocalRootNavController` (full-screen flows) and `LocalTabNavController` (within bottom tabs). Consumed via CompositionLocals in Feature layer only.
- Notifications: `LocalTopPillController` — top pill notifications replace snackbars. Never use `Scaffold(snackbarHost=...)` in features.
- **Tab features** register as bottom tabs via `NavigationProvider` interface + Koin `bind`. See `GroupsNavigationProviderImpl`.
- **Non-tab features** (write-flows and sub-flows extracted into standalone modules) implement `TabGraphContributor` instead. The host tab's `NavigationProvider` injects all `TabGraphContributor` instances via Koin and calls `contributeGraph(builder)` inside `buildGraph()`. This allows runtime route merging without compile-time cross-feature dependencies. See `ContributionsTabGraphContributorImpl`, `WithdrawalsTabGraphContributorImpl`, `SubunitsTabGraphContributorImpl`, `SettlementsTabGraphContributorImpl`.
- Tab screens define TopBar/MainAction via `ScreenUiProvider` implementations (not their own Scaffold).

## Offline-First Data Flow

### 🛑 The "True Offline" Write Protocol

We use a strictly **"Offline-First"** approach. The UI only observes the Local DB. The Cloud is a replication target, not the source of truth for the UI.
1. **Local ID Generation:** NEVER let Firestore generate the ID. ALWAYS generate a `UUID` locally.
2. **Local Metadata Generation:** Generate `createdAt = System.currentTimeMillis()` locally.
3. **Repository Write Order:** Save to Room (Local) FIRST -> Launch Background Job -> Sync to Cloud (Upsert).

**Critical: Subcollection Cleanup on Deletion**
Firestore does **NOT** auto-delete subcollections when a parent document is deleted. If a real-time listener watches a subcollection, you **MUST** delete subcollection documents **BEFORE** the parent document.


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
**Docs:** See `docs/architecture/patterns/offline-first.md` § "Reusable Sync Delegates" and `docs/architecture/patterns/core-services-catalog.md` § G.

## DI Pattern (Koin)

Each feature has a set of modules wired in `app/.../FeatureModuleAggregations.kt`:
```
groupsDomainModule + groupsDataModule + groupsUiModule → groupsFeatureModules
subunitsDomainModule + subunitsDataModule + subunitsUiModule → subunitsFeatureModules
contributionsDomainModule + contributionsUiModule → contributionsFeatureModules  (no dedicated contributions data module — relies on `ContributionRepository` impl from `balancesDataModule` in :data)
withdrawalsDomainModule + withdrawalsUiModule → withdrawalsFeatureModules  (no dedicated withdrawals data module — relies on `CashWithdrawalRepository` impl from `balancesDataModule` in :data)
settlementsUiModule → settlementsFeatureModules  (contributes `SettlementsTabGraphContributorImpl` for the "Tu posición" / "Your position" sub-flow inside Balances tab)
```
- **Tab features** UI modules declare: ViewModel, Mapper, `NavigationProvider` (factory + bind), `ScreenUiProvider` (single + bind).
- **Non-tab features** UI modules declare: ViewModel, Mapper, `TabGraphContributor` (factory + bind). They typically do **not** implement `NavigationProvider` but still register a `ScreenUiProvider` when they need a top bar (e.g. write-flow screens).
- See `features/groups/.../GroupsUiModule.kt` (tab), `features/contributions/.../ContributionsUiModule.kt` (non-tab), and `features/profile/.../ProfileUiModule.kt` as templates.

## Testing

### Coroutine Testing (CRITICAL - Prevents Flaky Tests)

When testing classes that launch background coroutines (e.g., Repositories with `syncScope.launch {}`), you **MUST** inject the `CoroutineDispatcher` to ensure deterministic test behavior.
- Always inject `CoroutineDispatcher` into classes that launch background coroutines.
- Provide a default (`= Dispatchers.IO`) so production code doesn't need to specify it.
- Use `StandardTestDispatcher()` in tests and pass it to both the class and `runTest()`.
- Call `runTest(testDispatcher)` - the dispatcher must match what's used in the class.
- Call `advanceUntilIdle()` before assertions to ensure background work completes.


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
- **CI:** Instrumentation tests run via `.github/workflows/instrumentation-tests.yml` — triggers on `main` push and `workflow_dispatch` (manual). Uses `reactivecircus/android-emulator-runner@v2` with API 34 (configurable, API 31+).

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
- `make fast-check` — fast incremental quality check (uses Gradle daemon & build cache, ~15–30s).
- `make check` — full cold quality check (single-pass Gradle execution, mirrors CI, ~1.5–2min).

## Static Analysis & Code Quality

- **Detekt** (code quality/complexity), **Ktlint** (formatting), and **CodeQL** (security) are configured in `build.gradle.kts` for all subprojects.
- **CPD** (copy-paste detection) uses the `de.aaschmid.cpd` Gradle plugin at root level. Minimum token count: 100. Reports in `build/reports/cpd/`.
- **JaCoCo** (code coverage) is configured for all subprojects. Per-module reports via `jacocoTestReport`, merged report via `jacocoMergedReport`.
- **Konsist** (architecture rule enforcement) tests live in `:konsist-tests` module. Enforces naming conventions, dependency rules, and structural patterns from this manifesto.
- Detekt config lives at `config/detekt/detekt.yml`. Ktlint rules are in `.editorconfig`.
- Local verification uses `make fast-check` (rapid iterative check) and `make check` (single-pass cold quality gate).
- CI runs static analysis via `.github/workflows/static-analysis.yml` (ktlint + detekt + CPD) — parallel to and independent of `build-and-test.yml`.
- JaCoCo and Konsist run in a separate `.github/workflows/coverage-and-architecture.yml` workflow — also independent from `build-and-test.yml`.
- Detekt uses `ignoreFailures = true` locally; gating is done by GitHub Code Scanning's "Code scanning results" check (only new alerts block PRs).
- CPD uses `ignoreFailures = true` — duplications are informational, not blocking.
- Pre-commit hook runs **ktlint only** (fast). Detekt, CPD, JaCoCo, and Konsist run in CI only.
- New code must not introduce new detekt findings. Formatting must comply with ktlint / `.editorconfig`.
- See `docs/engineering/quality-and-static-analysis.md` for full details.

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

> **Full details:** See [`docs/architecture/patterns/core-services-catalog.md`](docs/architecture/patterns/core-services-catalog.md) for complete method signatures, parameters, and "when to use" guidance.

Before creating any new service, utility, formatter, or UI component, **check the catalog first** to avoid duplication.

### Design-System UI Components (`:core:design-system`)

| Category | Components |
|---|---|
| **Scaffold & Nav** | `FeatureScaffold`, `ExpressiveFab`, `LargeExpressiveFab`, `MainAction`, `rememberScrollAwareFabVisibility`, `ScrollAwareFabContainer`, `NavigationBarIcon`, `TabGraphContributor` |
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

### 🛑 Language Preference (STRICT)
ALWAYS communicate and write chat responses in **English** when interacting with the user on SplitTrip, regardless of the language used in user prompts, issue descriptions, UI screenshots, or localized app resources.

### 🛑 No Git Operations (STRICT)
NEVER stage, commit, push, or create PRs autonomously. NEVER execute, propose, suggest, or ask for permission to run `git add`, `git commit`, or `git push`. Git operations are strictly under the user's manual control. `git fetch`, `git checkout`, and `git pull` are allowed for setup/automation (e.g., when running `start-issue`).

### 🛑 No Manual Conflict Resolution (STRICT)
NEVER attempt to manually resolve git merge conflicts using file editing tools token-by-token. Resolving conflicts via AI file edits is highly inefficient, error-prone, and wastes token limits. If you encounter git conflicts (e.g., after pulling `develop`), STOP immediately. Do NOT attempt to fix them. Instruct the user to resolve the conflicts locally in their IDE and return to you once they are resolved.

## No Pragmatic Patches

No quick hacks, temporary patches, or code that compromises Clean Architecture boundaries. Always write clean, production-ready code.

## Make Check Gate

Run `make check` locally before declaring any task done, completed, addressed, or accomplished. 0 failures are required. Never leave verification for CI/CD or the user to discover. Do NOT run `make check` or `make fast-check` before modifying code to verify the codebase state. It is redundant because `develop` is always compiling. Run `make check` locally ONLY *after* changes have been made.

### Fast Iteration (`make fast-check`)
During active development and rapid iteration, run `make fast-check` for fast incremental feedback using the Gradle daemon and build cache (~15–30s):
```bash
make fast-check > build.log 2>&1 && echo "Fast check passed" || (echo "Fast check failed. Last 100 lines:" && tail -n 100 build.log)
```

### Final Validation (`make check`)
Before completing any task or PR, run `make check` to verify full cold-start CI parity (~1.5–2 min):
```bash
make check > build.log 2>&1 && echo "Check passed successfully" || (echo "Check failed. Last 100 lines:" && tail -n 100 build.log)
```

**CRITICAL (Context Window Optimization):**
Do not run `make check` or `make fast-check` directly without output redirection, as the massive stdout/stderr will saturate your context window. Always buffer and filter the output as shown above.

## Commenting Policy

Comment the *why*, never the *what*. Delete redundant comments that merely restate what the code does.

## Agent Plan Strict Stop

Whenever a plan-only/planning-focused skill or command explicitly dictates that the task is completed after writing/posting the plan and that no codebase modifications should be performed, this instruction takes absolute precedence. The agent MUST NOT write code, create production files, or execute any modifications under those circumstances.

## Multi-Agent Architecture & Phased SDD Pipeline

Feature development follows a 5-phase Specification-Driven Development (SDD) lifecycle with specialized agent personas:
1. **Specification Agent:** Analyzes briefs, retrieves domain invariants from `docs/domain/`, outputs formal change spec.
2. **Architect Agent:** Validates module boundaries, UseCase contracts, and MVI state models (`docs/architecture/adrs/0003-clean-architecture.md`).
3. **Domain Implementer:** Implements pure business logic and unit tests in `:domain` with zero platform dependencies (`docs/architecture/adrs/0002-bigdecimal-math.md`).
4. **Infra & UI Implementer:** Connects Room persistence, Firestore sync delegates, ViewModels, and stateless Compose screens (`docs/architecture/patterns/offline-first.md`, `docs/architecture/patterns/mvi-and-stateless-screens.md`).
5. **Auditor / QA Agent:** Verifies Konsist rules, 0 FQN violations, 600-line file limits, and executes `make fast-check` / `make check`.

See `docs/ai/agent-roles-and-taxonomy.md` and `docs/ai/ai-ecosystem.md` for full details.

## String Translations

When adding new string resources (e.g. in `strings.xml`), you MUST always add the corresponding Spanish translations. Andaluz translations are not needed as they are generated automatically based on the Spanish ones.

## Workspace Resolution Protocol

Do not prompt the user for full GitHub URLs if they provide an ID (like an issue or PR number).
You are bound to a local Git workspace. To resolve the target URL:
1. Infer the repository by checking `.git/config` or running `git remote -v`.
2. Construct the required `$PR_URL` or `$ISSUE_URL` argument automatically before executing any skill.

## Code-Intelligence MCP Tools

When performing structural code queries (finding implementations, call graphs, entity definitions, or dependency analysis), prefer using graph-based MCP tools over raw `grep` or file globbing:

- **codebase-memory-mcp** (14 tools) — for structural queries: `get-entity`, `get-call-graph`, `find-implementations`, `impact-analysis`, etc.
- **Graphify** — for semantic / cross-modal queries and HTML visualization reports.

This dramatically reduces token consumption (80–99% for structural queries vs. grep/read cycles).

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

When the user types `/graphify`, use the installed graphify skill or instructions before doing anything else.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- Dirty graphify-out/ files are expected after hooks or incremental updates; dirty graph files are not a reason to skip graphify. Only skip graphify if the task is about stale or incorrect graph output, or the user explicitly says not to use it.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).

## Headroom Context Compression

This project uses Headroom (`headroom proxy --port 8787`) to compress LLM prompt payloads, tool outputs, and context logs.

Rules:
- `opencode` sessions run wrapped via Headroom (`headroom wrap opencode`).
- Output shaping (`HEADROOM_OUTPUT_SHAPER=1`) can be enabled for verbose sessions.
- Run `headroom learn` after failed or complex sessions to extract failure patterns into `AGENTS.md`.


<!-- headroom:rtk-instructions -->
# RTK (Rust Token Killer) - Token-Optimized Commands

When running shell commands, **always prefix with `rtk`**. This reduces context
usage by 60-90% with zero behavior change. If rtk has no filter for a command,
it passes through unchanged — so it is always safe to use.

## Key Commands
```bash
# Git (59-80% savings)
rtk git status          rtk git diff            rtk git log

# Files & Search (60-75% savings)
rtk ls <path>           rtk read <file>         rtk grep <pattern>
rtk find <pattern>      rtk diff <file>

# Test (90-99% savings) — shows failures only
rtk pytest tests/       rtk cargo test          rtk test <cmd>

# Build & Lint (80-90% savings) — shows errors only
rtk tsc                 rtk lint                rtk cargo build
rtk prettier --check    rtk mypy                rtk ruff check

# Analysis (70-90% savings)
rtk err <cmd>           rtk log <file>          rtk json <file>
rtk summary <cmd>       rtk deps                rtk env

# GitHub (26-87% savings)
rtk gh pr view <n>      rtk gh run list         rtk gh issue list

# Infrastructure (85% savings)
rtk docker ps           rtk kubectl get         rtk docker logs <c>

# Package managers (70-90% savings)
rtk pip list            rtk pnpm install        rtk npm run <script>
```

## Rules
- In command chains, prefix each segment: `rtk git add . && rtk git commit -m "msg"`
- For debugging, use raw command without rtk prefix
- `rtk proxy <cmd>` runs command without filtering but tracks usage
<!-- /headroom:rtk-instructions -->
