# AGENTS.md — SplitTrip

> [!IMPORTANT]
> **Source of Truth:** Single source of truth for architectural constraints and agent behaviors. See `.github/copilot-instructions.md` for the full technical manifesto.

## Project Overview

Kotlin Android app (Jetpack Compose, Material 3) for shared travel expenses. Multi-module Clean Architecture, offline-first with Room + Firestore, Koin DI.

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

### ViewModel Rules
ViewModels ONLY inject UseCases, Mappers, and Domain Services. NEVER inject `Context`, `LocaleProvider`, Repositories, or other ViewModels.
- **Semantic Navigation Intent:** ViewModels and feature event handlers MUST ONLY emit high-level semantic UI actions (`UiAction.NavigateToSubscriptions`, `UiAction.RequestUpgrade`, `UiAction.NavigateBack`). ViewModels must NEVER evaluate session/auth state or cross-cutting preconditions to resolve global route strings (e.g., `Routes.SETTINGS_ACCOUNT_STATUS` vs `Routes.SETTINGS_SUBSCRIPTIONS`). Routing decisions belong to `:core:design-system:navigation` orchestrators (e.g., `UpgradeNavigator`), `*Feature` composables, or `ScreenUiProvider`s.
- **Domain Service Injection Scope:** ViewModels must NOT inject domain services merely for presentation or routing decisions. Delegate routing branches to a navigator or observe a domain UseCase.
- **Anti-Duplication of Cross-Cutting Logic:** Forbid repetitive state branching (e.g., `isAnonymous()` or subscription checks) across feature modules; centralize in `:core:design-system` navigators or `:domain` services/use cases.
- **Baseline Principle:** *Linter and Konsist compliance is a necessary baseline, NOT proof of clean architecture.*

### MVI Triad
Every screen gets `UiState` (ImmutableList), `UiEvent` (sealed interface), and `UiAction` (side-effects via Channel/SharedFlow). Never put one-shot events in `UiState`.

### Formatting in Mappers
Formatting belongs in Mappers (which receive `LocaleProvider`), never ViewModels or Domain Services. Domain Services must NEVER contain `formatShareForInput()`, `formatAmountForDisplay()`, or any human-readable formatting method.

### Big Decimal Math
All decimal math MUST use `BigDecimal` with explicit scale + `RoundingMode`. `Double` or `Float` are strictly prohibited. Firestore boundary serialization must use `String` (`toPlainString()` / `toBigDecimalOrNull()`).

### File Size Limit
Production source files MUST NOT exceed 600 lines (enforced by Konsist). Extract handlers/delegates/components before adding code; always check with `wc -l`.

### Enum Centralization
Domain enums (`AppLanguage`, `Currency`) are the single source of truth for parsing codes, fallback defaults, and validation. Never duplicate string-matching or locale fallback checks in presentation layers.

### Single Composable Per File
Every production Kotlin file under feature `presentation` packages containing a `@Composable` function must define exactly one top-level `@Composable` function whose name matches the file name.

### Feature vs Screen Pattern
`*Feature` (orchestrator composable) holds ViewModel, collects flows, and consumes `LocalTopPillController`/`LocalTabNavController`. `*Screen` is stateless — takes `UiState` + event lambdas only.

### No Fully Qualified Names (No FQN)
NEVER use Fully Qualified Names inside production or test code (e.g., `java.util.UUID.randomUUID()`, `com.google.firebase.Timestamp.now()`, `kotlinx.coroutines.Dispatchers.IO`). Always import all types at the top of the file. Enforced by Konsist rule `Production code must not use Fully Qualified Names (FQN) for classes or objects`.

## UI Clicks & Navigation — Debouncing

> [!IMPORTANT]
> **Always use debounced modifiers for UI navigation or expensive side-effects.**

NEVER use standard `Modifier.clickable` or `Modifier.combinedClickable` directly on list items, action buttons, or FABs—rapid double taps can push duplicate routes onto the back stack. ALWAYS use `Modifier.debouncedClickable` and `Modifier.debouncedCombinedClickable` from `es.pedrazamiguez.splittrip.core.designsystem.extension.ModifierExtensions`.

## Read-Only Chips, Pills & PassportTags — Semantic Colors Only

> [!IMPORTANT]
> **NEVER use `secondaryContainer` (`#97F0FF`), `secondaryFixed`, `secondaryFixedDim` (`#4FD8EB`), or bright cyan on read-only chips, pills, badges, or `PassportTag`s.**

Bright cyan is strictly reserved for interactive selected controls (e.g., selected `PassportChip`). For all read-only metadata, status, quota, or allowance pills, ALWAYS use `PassportTag` (or `PassportTagDefaults.colors(...)`) with semantic styles:
- `PassportTagStyle.INFO` (soft sky blue) — informational values, allowances, unlimited states
- `PassportTagStyle.SUCCESS` (soft sage green) — healthy quotas, confirmed/settled/completed states
- `PassportTagStyle.WARNING` (soft warm amber) — approaching limits, awaiting confirmation
- `PassportTagStyle.ERROR` (soft rose red) — exhausted limits, disputed/failed states
- `PassportTagStyle.NEUTRAL` (`surfaceContainerHigh`) — dates, locked perks, secondary metadata
- `PassportTagStyle.PRIMARY` — strong brand emphasis (e.g., category/Pro tier header badge)
Never map `HEALTHY`, `INCLUDED`, or `INFO` badges to `MaterialTheme.colorScheme.secondaryContainer` or `PassportTagStyle.SECONDARY`.

## Navigation

- Routes are `const val` in `core/design-system/.../Routes.kt`.
- **Paywall & Upgrade Routing:** Outside `:features:settings`, route exclusively via `UpgradeNavigator` (`navigateToUpgrade(navController)` / `getUpgradeDestination()`), never `Routes.SETTINGS_SUBSCRIPTIONS` directly.
- Two nav controllers: `LocalRootNavController` (full-screen) and `LocalTabNavController` (within tabs), consumed in Feature layer only.
- Notifications: `LocalTopPillController` replaces snackbars; never use `Scaffold(snackbarHost=...)` in features.
- **Tab features** register via `NavigationProvider` + Koin `bind` (see `GroupsNavigationProviderImpl`).
- **Non-tab features** implement `TabGraphContributor`; the host tab's `NavigationProvider` injects all `TabGraphContributor` instances via Koin and calls `contributeGraph(builder)` inside `buildGraph()` (e.g., `ContributionsTabGraphContributorImpl`, `WithdrawalsTabGraphContributorImpl`, `SubunitsTabGraphContributorImpl`, `SettlementsTabGraphContributorImpl`).
- Tab screens define TopBar/`MainAction` via `ScreenUiProvider` implementations (not their own `Scaffold`).

## Offline-First Data Flow

### The "True Offline" Write Protocol
UI observes Room Flow only; Cloud is a replication target, never the UI source of truth.
1. **Local ID & Metadata:** Generate `UUID` and `createdAt = System.currentTimeMillis()` locally; never use Firestore `.add()`.
2. **Read & Reconcile:** Observe Room Flow; subscribe to Firestore snapshots via `onStart`, track subscription `Job`s (cancel before re-launching, see `GroupRepositoryImpl`, `ExpenseRepositoryImpl`), and reconcile Room via `@Transaction` upsert + selective delete.
3. **Write Order:** Save to Room first (instant UI update), then launch background sync (`syncScope.launch`) using an injected `CoroutineDispatcher` (default `Dispatchers.IO`).
4. **Subcollection Cleanup on Deletion:** Firestore does NOT auto-delete subcollections when a parent document is deleted. Delete subcollection documents **BEFORE** the parent document.

### Reusable Sync Delegates (MANDATORY for new repositories)
Encapsulated in `es.pedrazamiguez.splittrip.data.sync` (`:data`, `internal`):

| Delegate | Purpose |
|---|---|
| `KeyedSubscriptionTracker` | Manages keyed cloud subscription `Job`s (one active listener per key). |
| `subscribeAndReconcile<T>()` | Cloud Flow → reconcile local → confirm `PENDING_SYNC` items. |
| `syncCreateToCloud()` | Background sync: cloud write → `SYNCED` / `SYNC_FAILED` (create + update). |
| `syncDeletionToCloud()` | Background sync: cloud delete (Firestore SDK guarantees write ordering). |

**Reference:** `SubunitRepositoryImpl` (all delegates), `CashWithdrawalRepositoryImpl` (mixed), `GroupRepositoryImpl` (`subscribeAndReconcile`). See `docs/architecture/patterns/offline-first.md` and `docs/architecture/patterns/core-services-catalog.md` §G.

## DI Pattern (Koin)

Feature modules are wired in `app/.../FeatureModuleAggregations.kt` (e.g., `groupsDomainModule + groupsDataModule + groupsUiModule → groupsFeatureModules`; `contributions` and `withdrawals` reuse `balancesDataModule`; `settlementsUiModule` contributes `SettlementsTabGraphContributorImpl`).
- **Tab UI modules:** ViewModel, Mapper, `NavigationProvider` (factory + bind), `ScreenUiProvider` (single + bind) — see `GroupsUiModule.kt`, `ProfileUiModule.kt`.
- **Non-tab UI modules:** ViewModel, Mapper, `TabGraphContributor` (factory + bind), plus `ScreenUiProvider` if a top bar is needed — see `ContributionsUiModule.kt`.

## Testing

### Coroutine Testing (CRITICAL)
Inject `CoroutineDispatcher` (default `= Dispatchers.IO`) into any class launching background coroutines. In tests, pass the same `StandardTestDispatcher()` to both the class and `runTest(testDispatcher)`, and call `advanceUntilIdle()` before assertions.

- **Frameworks:** JUnit 5 + MockK for unit tests (some legacy/Robolectric tests use JUnit 4 via Vintage engine); `AndroidJUnit4` for instrumentation tests.
- **Assertions:** NEVER use Kotlin `assert()` (no-op on Android). ALWAYS use JUnit `Assert.assertTrue(...)`, `Assert.assertEquals(...)`, etc.
- **Layer patterns:** Repos (`ContributionRepositoryImplTest.kt`), ViewModels (`onEvent()` + StateFlow/SharedFlow), Mappers (fixed-`Locale` `LocaleProvider` fakes, `GroupUiMapperImplTest.kt`).
- **Instrumentation (`app/src/androidTest/`):** Custom `TestRunner` + `TestApp` bypass production Koin; `AppNavHost` uses composable-scoped `getKoin()` so tests wrap with `KoinApplication { modules(testModule) }` (see `AppNavHostTest.kt`, `MainScreenTest.kt`, `FakeNavigationProvider`, `TestModules.createAppNavHostTestModule()`). Stateless `*Screen` composables have direct smoke tests in `app/src/androidTest/.../screens/`.
- **Commands & CI:** `./gradlew test` (unit), `./gradlew connectedAndroidTest` (UI; CI via `.github/workflows/instrumentation-tests.yml` on `main` push / `workflow_dispatch`, API 34).

## Compose Previews

Wrap in `PreviewThemeWrapper` with `@PreviewLocales`, `@PreviewThemes`, or `@PreviewComplete`. Use `MappedPreview` + `*PreviewHelper` in `src/debug` (`core/design-system/src/debug/.../preview/`).

## Build, Run & Static Analysis

- **Toolchain:** JDK 21, Android SDK 36, min SDK 26; `google-services.json` in `app/`; version in `version.properties`.
- **API Keys (Gradle properties in `~/.gradle/gradle.properties`, never `local.properties`):** `OER_APP_ID_DEBUG` (debug), `OER_APP_ID_RELEASE` (release, or env var on CI).
- **Tools:** Detekt (`config/detekt/detekt.yml`, `ignoreFailures = true` locally; Code Scanning blocks new alerts on PRs), Ktlint (`.editorconfig`; runs in pre-commit hook), CodeQL, CPD (`de.aaschmid.cpd`, min 100 tokens, `ignoreFailures = true`), JaCoCo (`jacocoTestReport` / `jacocoMergedReport`), Konsist (`:konsist-tests`).
- **Local & CI Gates:** `make fast-check` (incremental, ~15–30s), `make check` (cold CI parity, ~1.5–2min). CI runs `.github/workflows/static-analysis.yml` (ktlint + detekt + CPD) and `.github/workflows/coverage-and-architecture.yml` (JaCoCo + Konsist). New code must introduce zero detekt findings and comply with ktlint. See `docs/engineering/quality-and-static-analysis.md`.

## Naming Conventions

- **Services:** Domain interfaces `*Service` (e.g., `ExpenseValidationService`); data impls `*ServiceImpl` (`LocalDatabaseCleanerServiceImpl`). Calculators/factories keep `*Calculator`/`*Factory`; stateless domain converter `object`s use `*Converter` (`CurrencyConverter`).
- **Feature Mappers (`..presentation.mapper..`):** Concrete-only classes and interfaces end with `UiMapper` (`AddExpenseUiMapper`); implementations in Interface+Impl end with `UiMapperImpl` (`GroupUiMapperImpl`). Enforced by Konsist (`ArchitectureTest.NamingConventions.presentation layer mappers must end with UiMapper or UiMapperImpl (interfaces vs implementations)`).
- **Data Mappers:** Top-level extension functions in `*DocumentMapper.kt` (`:data:firebase`), `*EntityMapper.kt` (`:data:local`), `*DtoMapper.kt` (`:data:remote`).
- **DI Module Names:** Variables inside `viewModel { }` / `factory { }` and constructor args passed to handlers/mappers MUST use full camelCase class names (e.g., `val addExpenseUiMapper = get<AddExpenseUiMapper>()`, `addCashWithdrawalUiMapper = cashWithdrawalUiMapper`, never `val mapper = get()`).

## Service & Component Catalog (Quick Reference)

Before creating any new service, formatter, sync delegate, or UI component, **check the full catalog first** to avoid duplication:
- [`docs/architecture/patterns/core-services-catalog.md`](docs/architecture/patterns/core-services-catalog.md) (complete signatures, parameters, and usage guidance across `:core:design-system`, `:domain`, and `:data`)
- `splittrip-rag` MCP (`search_project_knowledge`) for fast semantic lookup.

## AI Agent Behavior Rules (CRITICAL)

### 🛑 Language Preference (STRICT)
ALWAYS communicate and write chat responses in **English** when interacting with the user on SplitTrip, regardless of the language in user prompts, issue descriptions, screenshots, or app resources.

### 🛑 No Git Operations (STRICT)
NEVER stage, commit, push, or create PRs autonomously (`git add`, `git commit`, `git push` are strictly under the user's manual control). `git fetch`, `git checkout`, and `git pull` are allowed for setup/automation (e.g., `start-issue`).

### 🛑 No Manual Conflict Resolution (STRICT)
NEVER manually resolve git merge conflicts with file-editing tools. If conflicts occur (e.g., after pulling `develop`), STOP immediately and instruct the user to resolve them in their IDE.

## No Pragmatic Patches

No quick hacks, temporary patches, or Clean Architecture compromises. Always write clean, production-ready code.

## Make Check Gate

Run `make check` locally before declaring any task done (0 failures required). Do NOT run `make check` or `make fast-check` before modifying code.
- **Fast iteration:** `make fast-check > build.log 2>&1 && echo "Fast check passed" || (echo "Fast check failed. Last 100 lines:" && tail -n 100 build.log)`
- **Final validation:** `make check > build.log 2>&1 && echo "Check passed successfully" || (echo "Check failed. Last 100 lines:" && tail -n 100 build.log)`
Always redirect output as shown so massive logs do not saturate the context window.

## Commenting Policy

Comment the *why*, never the *what*. Delete redundant comments.

## Agent Plan Strict Stop

When a plan-only skill/command states the task ends after writing/posting the plan with no codebase changes, that instruction takes absolute precedence—do NOT edit or create production files.

## Multi-Agent Architecture & Phased SDD Pipeline

Feature development follows a 5-phase SDD lifecycle: (1) **Specification Agent** (`docs/domain/`), (2) **Architect Agent** (`docs/architecture/adrs/0003-clean-architecture.md`), (3) **Domain Implementer** (`:domain`, `docs/architecture/adrs/0002-bigdecimal-math.md`), (4) **Infra & UI Implementer** (`docs/architecture/patterns/offline-first.md`, `mvi-and-stateless-screens.md`), (5) **Auditor / QA Agent** (Konsist, 0 FQN, ≤600 lines, `make check`). See `docs/ai/agent-roles-and-taxonomy.md` and `docs/ai/ai-ecosystem.md`.

## String Translations & Localization

- **Spanish (`values-es/`):** Always add Spanish translations in `values-es/strings.xml` when adding `strings.xml` entries.
- **Andaluz (`values-es-rAN/`):** Andalusian (Andaluz / EPA) dialect, NEVER Aragonese. Auto-generated on compile via `generateAndaluzStrings` (`make andaluz`) — NEVER edit `values-es-rAN/strings.xml` manually.

## Workspace Resolution Protocol

Do not prompt the user for full GitHub URLs when an ID (issue/PR number) is given. Infer the repo from `.git/config` / `git remote -v` and follow `.agents/rules/github-operations.md` (which enforces `gh` CLI for writes and URL verification).

## Code-Intelligence MCP Tools

Prefer graph-based tools over raw `grep`/globbing for structural queries: **codebase-memory-mcp** (call graphs, implementations, impact analysis) and **Graphify** (`.agents/rules/graphify.md` — always-on rule).

## Headroom Context Compression & RTK

Sessions use Headroom (`headroom proxy --port 8787`, `headroom wrap opencode`, `HEADROOM_OUTPUT_SHAPER=1`, `headroom learn`) and **RTK** (`.agents/plugins/rtk/rules/AGENTS.md` + `.agents/plugins/rtk/hooks.json` — automatically rewrites shell commands to `rtk` for token savings).
