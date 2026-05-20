# SplitTrip Technical Manifesto & Copilot Instructions

You are acting as the **Lead Android Architect** for **SplitTrip**.
This project is NOT a standard Android app. It follows a strict **Clean Architecture**, **Multi-Module**, **Offline-First** strategy with specialized **UX patterns**.

Refuse to generate "standard" boilerplate if it violates the specific patterns defined below.

---

## 1. 🏛️ Architecture & Dependency Injection (Strict)

**The Golden Rule:** ViewModels **NEVER** depend on Repositories.
* ❌ **Bad:** `class MyViewModel(private val repository: GroupRepository)`
* ✅ **Good:** `class MyViewModel(private val getGroupsUseCase: GetGroupsUseCase)`

**ViewModel Lifecycle & Injection Rules (CRITICAL):**
* ❌ **Strict Prohibition:** NEVER inject a `ViewModel` into another `ViewModel` (e.g., via constructor).
    * *Reason:* This creates "Zombie Instances" detached from the UI lifecycle.
* ✅ **SharedViewModel Pattern:**
    * Must be injected into the **Feature** (Composable) using the Activity Scope:
      `viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner`.
    * Pass the necessary data (e.g., `selectedGroupId`) from the Feature to the screen's ViewModel via public methods or `LaunchedEffect`.

**ViewModel Dependencies (STRICT):**
* ✅ **ONLY inject:** UseCases, Mappers, and Domain Services (e.g., `ExpenseCalculatorService`)
* ❌ **NEVER inject:** 
    * `LocaleProvider` - This belongs in Mappers for formatting/mapping logic
    * `Context` - Use `UiText` pattern instead
    * Repositories - Always use UseCases
    * Formatters directly - Use Mappers which wrap formatters
* **Rationale:** ViewModels manage state and orchestrate use cases. Data transformation, formatting, and locale-aware operations are mapping concerns, not state management concerns.

**ViewModel Complexity & Handler Delegation (CRITICAL):**
* When a ViewModel's `onEvent()` handles **more than ~5 event categories** or the file exceeds ~200 lines, you **MUST** extract logic into **Event Handler** classes.
* Handlers are **plain classes (NOT ViewModels)** that receive the shared `MutableStateFlow<UiState>`, `MutableSharedFlow<UiAction>`, and `CoroutineScope` via a `bind()` method.
* The ViewModel becomes a thin router: it delegates each event to the appropriate handler.
* Handlers are co-created inside the `viewModel { }` Koin block so cross-handler references work.
* **Reference implementation:** See `AddExpenseEventHandler` interface and `ConfigEventHandler`, `SplitEventHandler`, `SubmitEventHandler` in `:features:expenses`.
    ```kotlin
    // Handler interface contract
    interface MyFeatureEventHandler {
        fun bind(
            stateFlow: MutableStateFlow<MyUiState>,
            actionsFlow: MutableSharedFlow<MyUiAction>,
            scope: CoroutineScope
        )
    }

    // ViewModel as thin router
    class MyViewModel(
        private val formHandler: FormEventHandler,
        private val submitHandler: SubmitEventHandler
    ) : ViewModel() {
        init {
            formHandler.bind(_uiState, _actions, viewModelScope)
            submitHandler.bind(_uiState, _actions, viewModelScope)
        }
        fun onEvent(event: MyUiEvent) {
            when (event) {
                is MyUiEvent.NameChanged -> formHandler.handleNameChanged(event.name)
                is MyUiEvent.Submit -> submitHandler.handleSubmit()
            }
        }
    }
    ```

**Handler → Delegate Sub-Pattern (Tier 2):**
* When an Event Handler itself exceeds **~600 lines**, extract cohesive logic sections into **Delegate** classes.
* Delegates use the `*Delegate` suffix (NOT `*EventHandler`) — they don't implement the `AddExpenseEventHandler` interface and don't participate in `bind()`.
* Two valid patterns:
    * **Lambda-based state access** — Delegate receives state access via lambdas (`updateAddOn`, `onRateApplied`). Best for async operations (API calls, debouncing).
    * **Stateless/pure** — Delegate receives all context as parameters. No internal state, no `CoroutineScope`. Best for synchronous calculations.
* Delegates are created inside the `viewModel { }` Koin block alongside handlers, sharing the same domain service instances.
* A **Konsist architecture test** enforces a **600-line hard limit** on all production source files (test files are exempt).
* **Reference:** See `AddOnExchangeRateDelegate` and `IntraSubunitSplitDelegate` in `:features:expenses`.

**Module Visibility:**
* **Features** (`:features:*`) cannot see each other. They communicate strictly via **Navigation Routes** or `:domain` interfaces.
* **Features** cannot see `:data`. They only see `:domain` Repository interfaces.
* **DI Strategy (Koin):**
    * Features declare UI modules (ViewModels).
    * Feature navigation is decoupled using the **`NavigationProvider`** and **`TabGraphContributor`** patterns (Plugin Pattern).
    * *Instruction:* When creating a new feature entry point, choose `NavigationProvider` for bottom tabs or `TabGraphContributor` for non-tab write-flows (see below), and bind it in Koin so the App module can discover it dynamically.

**Navigation Provider vs. TabGraphContributor (CRITICAL for new modules):**
* **`NavigationProvider`** — For features that represent a **bottom navigation tab** (e.g., Groups, Expenses, Balances, Profile). Provides icon, label, order, and a full `buildGraph()`.
* **`TabGraphContributor`** — For features that are **standalone write-flows** extracted into their own module but navigated to from within an existing tab (e.g., `:features:contributions`, `:features:withdrawals`, `:features:subunits`). The host tab's `NavigationProvider` injects all `TabGraphContributor` instances via Koin and calls `contributeGraph(builder)` inside its `buildGraph()`.
    ```kotlin
    // Non-tab module DI registration
    factory { ContributionsTabGraphContributorImpl() } bind TabGraphContributor::class

    // Host tab's NavigationProvider merges contributed routes
    class BalancesNavigationProviderImpl(
        private val graphContributors: List<TabGraphContributor> = emptyList()
    ) : NavigationProvider {
        override fun buildGraph(builder: NavGraphBuilder) {
            builder.balancesGraph()
            graphContributors.forEach { it.contributeGraph(builder) }
        }
    }
    ```

---

## 2. 📱 UI Architecture: "Feature vs. Screen"

We strictly separate **Orchestration** from **Rendering** to enable isolated `@Preview`.

**The Pattern:**
1.  **`Screen` (The Renderer):**
    * Must be a **Stateless** Composable.
    * Takes strictly **pure data** (UiState) and **lambdas** (onEvent).
    * **NEVER** accepts `ViewModel`, `NavController`, `TopPillController`, or `Flow`.
    * *Why?* Enables instant `@Preview` without mocking complex classes.
2.  **`Feature` (The Orchestrator):**
    * The "Route" entry point.
    * Holds the `ViewModel` via `koinViewModel()`.
    * Consumes Global Controllers (`LocalNavController`, `LocalTopPillController`).
    * Collects StateFlow/Actions and passes plain data/lambdas to the `Screen`.

**Previews & Helpers:**
* **Wrappers:** Always wrap previews in **`PreviewThemeWrapper`**.
* **Annotations:** Use `@PreviewLocales` (En/Es), `@PreviewThemes` (Light/Dark), and `@PreviewComplete` (Full Screen).
* **Mapped Previews:** Do not manually instantiate complex `UiModel`s.
    * ✅ **Required:** Use **`MappedPreview`** and create a `*PreviewHelper` composable (e.g., `GroupUiPreviewHelper`) in `src/debug`.
    * *Flow:* Domain Object -> Mapper -> UiModel -> Preview.
    * *Why?* Ensures the Preview accurately reflects how the Mapper transforms data.

---

## 3. 🔄 State Management (MVI & UDF)

**The Contract:**
Every screen must implement the Triad:
1.  **`UiState` (Data Class):**
    * Must be consumed as `.collectAsStateWithLifecycle()`.
    * **Immutable:** Use `ImmutableList` (Kotlinx Immutable Collections) instead of `List` for collections.
    * ❌ **Strict Prohibition:** NEVER put "One-shot" events (e.g., `showToast`) in `UiState`.
2.  **`UiEvent` (Sealed Interface):**
    * The ONLY way the UI talks to the ViewModel.
    * Expose a single `fun onEvent(event: UiEvent)`.
3.  **`UiAction` (Side Effects):**
    * Use `Channel<UiAction>` or `SharedFlow<UiAction>`.
    * Used for: Pill notifications, Navigation, Toasts.
    * **UiText Pattern:** Use a sealed `UiText` interface for strings in ViewModels. Never use `Context` in ViewModels.

**Zero-Flicker Policy (Hot Flows):**
* Avoid triggering data loads via `LaunchedEffect(Unit)` (cold loading).
* **Mandatory:** Use `stateIn` with `SharingStarted.WhileSubscribed(stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME, replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION)` to keep data "alive" during configuration changes or brief tab switches, while resetting stale state on longer absences.
* **Never** hardcode the timeout or expiration values. Always use the constants from `:core:common`.
* **`FLOW_REPLAY_EXPIRATION = 0`** resets the replay cache to `initialValue` immediately after the upstream stops (which happens `FLOW_RETENTION_TIME` ms after the last subscriber leaves). This prevents stale-state flashes (e.g., empty state → shimmer → content) when returning to a tab after the flow has expired.
    ```kotlin
    val uiState = useCase().map { ... }
        .stateIn(
            scope,
            SharingStarted.WhileSubscribed(
                stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
                replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
            ),
            initialValue
        )
    ```

---

## 4. 🧭 Navigation & Global Controllers

This app uses **CompositionLocals** for global orchestration. Do not pass these controllers down as parameters; consume them in the **Feature** layer.

1.  **`LocalRootNavController`**:
    * Scope: Global Activity level.
    * Use for: Full-screen flows (Onboarding, Login, Settings) or "covering" the BottomBar.
2.  **`LocalTabNavController`**:
    * Scope: Inside MainScreen Tabs.
    * Use for: Drill-down navigation within a tab.
3.  **`LocalTopPillController`**:
    * Scope: Global (survives navigation).
    * Use for: Displaying top pill notifications from `UiAction`s.
    * *Pattern:* `pillController.showPill(message)` inside the Feature's `LaunchedEffect`.

**Routes:**
* Must be defined as `const val` in `:core:design-system/Routes.kt`.

---

## 5. 💱 Domain Logic: Multi-Currency & Validation

**Money Math:**
* ❌ **Prohibited:** Performing `BigDecimal` math or currency conversion inside a `ViewModel`.
* ✅ **Required:** Delegate ALL calculation logic to **`ExpenseCalculatorService`** (Domain Service).
* **Snapshot Model:** Expenses store three distinct values:
    1.  `sourceAmount` (What the user paid).
    2.  `groupAmount` (Standardized debt amount).
    3.  `exchangeRate` (The bridge).

**Decimal Precision (CRITICAL — No Double for Math):**
* ❌ **Prohibited:** Using `Double` or `Float` for any decimal math in Domain Services, Repositories, or Use Cases — including percentages, share weights, split ratios, and any value that requires precision.
    * *Reason:* `Double` has IEEE 754 floating-point representation errors (e.g., `1.0 / 3.0 = 0.33333...336`). These accumulate and cause validation failures (shares don't sum to 1.0), display glitches, and non-deterministic rounding.
* ✅ **Required:** Use `BigDecimal` with explicit `RoundingMode` and `scale` for ALL decimal operations.
    * *Reference:* See `SplitPreviewService` which uses `BigDecimal("100")`, `SMALLEST_PERCENT_UNIT = BigDecimal("0.01")`, and proper `divide(..., scale, RoundingMode.DOWN)`.
* **Firestore Serialization:** All domain model fields that represent decimal values (e.g., `Subunit.memberShares`, exchange rates, split percentages) use `BigDecimal`. At the Firestore document boundary, these are serialized as `String` (via `toPlainString()`) and deserialized with safe parsing (`toBigDecimalOrNull()`) to avoid IEEE 754 floating-point precision loss.
    ```kotlin
    // ✅ GOOD — String serialization at Firestore boundary
    // Domain → Document: BigDecimal → String
    memberShares = memberShares.mapValues { it.value.toPlainString() }
    exchangeRate = exchangeRate.toPlainString()

    // Document → Domain: String → BigDecimal (safe parsing)
    memberShares = memberShares.mapValues { it.value.toBigDecimalOrNull() ?: BigDecimal.ZERO }
    exchangeRate = exchangeRate?.toBigDecimalOrNull() ?: BigDecimal.ONE

    // ❌ BAD — Double at boundary (precision loss)
    memberShares = memberShares.mapValues { it.value.toDouble() }
    ```

**Domain Services MUST NOT Contain Formatting/Display Logic (STRICT):**
* ❌ **Prohibited:** Placing `formatShareForInput()`, `formatAmountForDisplay()`, or any human-readable formatting method in a Domain Service.
    * *Reason:* Domain Services encapsulate **business rules** (validation, calculation, distribution). Formatting is a **presentation/mapping concern**.
* ✅ **Required:** Formatting methods belong in **Mappers** (`:features:*/presentation/mapper/`).
    * Even "simple" conversions like `0.5 → "50"` are presentation logic — the format depends on locale, decimal separators, and UI context.
    * *Reference:* See `GroupUiMapperImpl`, `ExpenseUiMapper` which handle all formatting with `LocaleProvider`.

**Validation:**
* Validation logic (e.g., "Title is empty") belongs in a **`Domain Service`** (e.g., `ExpenseValidationService`), NOT the ViewModel, and NOT a UseCase.
* *Distinction:* UseCases = User Actions (Save). Services = Business Rules (Validate).

---

## 6. 💾 Data Layer: Offline-First & Single Source of Truth

**Mapping Strategy:**
* **Mandatory:** Data objects (Firebase/Room) must be mapped to Domain objects immediately.
* **Mandatory:** Domain objects must be mapped to `UiModel`s before reaching the View.
* **Formatting:** Use `LocaleProvider` inside Mappers. **Never** pass Android `Context` to Mappers or ViewModels.
* **Mappers Handle Formatting:** Even when using extension functions from `:core:design-system` (like `formatNumberForDisplay`), the **Mapper** must call them, NOT the ViewModel.
    * ✅ **Correct:** Mapper receives `LocaleProvider`, calls `value.formatNumberForDisplay(locale = localeProvider.getCurrentLocale())`
    * ❌ **Wrong:** ViewModel receives `LocaleProvider` and calls formatting functions directly
    * *Reason:* Formatting is a mapping/transformation concern, not a state management concern.

**Strict SSOT Flow (Single Source of Truth):**
1.  **Read:** UI observes **ONLY** the Local DB (Room) Flow.
    * *Note:* The Repository must never return a Flow directly from Cloud/Firebase.
2.  **Write:** User Action -> Writes to Local DB (Room).
3.  **Sync:** Repository triggers Cloud sync (suspend).
    * Success: Update Room -> UI updates automatically.
    * Failure: Silently catch exception -> UI continues showing Local data.

**DataStore Best Practices:**
* When saving IDs (e.g., `selectedGroupId`), **ALWAYS** save the corresponding human-readable metadata (e.g., `selectedGroupName`) to prevent UI blank states on app restart.

### 6.1 🛑 The "True Offline" Write Protocol

We use a strictly **"Offline-First"** approach. The UI only observes the Local DB. The Cloud is a replication target, not the source of truth for the UI.

When creating new data (Expenses, Groups, etc.), you **MUST** follow this exact order to prevent UI jumping, sorting issues, and duplicates.

1.  **Local ID Generation:**
    * **NEVER** let Firestore generate the ID (e.g., do not use `.add()`).
    * **ALWAYS** generate a `UUID` locally in the Repository or UseCase.
    * *Reason:* We need the ID immediately for the Local DB to prevent duplicates during sync.
2.  **Local Metadata Generation:**
    * **Timestamps:** Generate `createdAt = System.currentTimeMillis()` locally.
        * ❌ **Bad:** Relying on Firestore `@ServerTimestamp` or leaving it `0`.
        * *Consequence:* Items appear at the bottom of the list or disappear until a sync happens.
    * **User Attribution:** Inject `AuthenticationService` into the Repository and set `createdBy = currentUserId` locally.
        * ❌ **Bad:** Waiting for cloud functions to set the user ID.
3.  **Repository Write Order:**
    1.  **Save to Room (Local) FIRST.** -> *UI updates instantly.*
    2.  **Launch Background Job.** -> *Sync to Cloud.*
4.  **Cloud Operation:**
    * Use `.document(localId).set(data)` (Upsert).
    * **NEVER** use `.collection(...).add(data)`.

### 6.2 🔄 The Sync Protocol (Read)
When fetching data from the cloud:
1.  **Upsert Strategy:** Use `OnConflictStrategy.REPLACE` in Room DAOs.
2.  **Race Condition Protection:**
    * **NEVER** `deleteAll()` before inserting synced data. This wipes out unsynced local changes.
    * Only insert/update the specific items returned from the cloud.

### 6.3 📡 Real-Time Multi-Device Sync (Snapshot Listeners)

This is a **multi-user, multi-device** app. While the UI only reads from Room (Offline-First), changes by other users/devices must propagate in near real-time.

**The Pattern:**
1.  The Repository's `get*Flow()` subscribes to a **persistent Firestore `snapshotListener`** via `onStart`.
2.  Each snapshot represents the **complete authoritative state** of the collection.
3.  The Repository reconciles Room using a **merge strategy in a `@Transaction`** (upsert remote + selective delete of stale IDs).
4.  The Room Flow **re-emits automatically**, updating the UI.

> **⚡ Reusable Sync Delegates:** The patterns below are encapsulated in reusable utility functions in `es.pedrazamiguez.splittrip.data.sync`. **New repositories MUST use these delegates** instead of writing the boilerplate manually. See `wiki/offline-first-architecture.md` § "Reusable Sync Delegates" and `wiki/core-services-catalog.md` § G for the full API.

**Available delegates (`:data` module, `internal` visibility):**

| Delegate | Replaces |
|---|---|
| `KeyedSubscriptionTracker` | Manual `ConcurrentHashMap<String, Job>` + cancel + relaunch |
| `subscribeAndReconcile<T>()` | `subscribeToCloudChanges()` + `confirmPendingSyncXxx()` (~25 lines per repo) |
| `syncCreateToCloud()` | `syncScope.launch { cloudWrite → updateSyncStatus(SYNCED/SYNC_FAILED) }` |
| `syncDeletionToCloud()` | `syncScope.launch { cloudDelete + Timber log }` |

**Example using delegates (canonical pattern):**

```kotlin
override fun getGroupSubunitsFlow(groupId: String): Flow<List<Subunit>> =
    localDataSource.getSubunitsByGroupIdFlow(groupId)
        .onStart {
            subscriptionTracker.cancelAndRelaunch(groupId, syncScope) {
                subscribeAndReconcile(
                    cloudFlow = cloudDataSource.getSubunitsByGroupIdFlow(groupId),
                    reconcileLocal = { localDataSource.replaceSubunitsForGroup(groupId, it) },
                    getPendingIds = { localDataSource.getPendingSyncSubunitIds(groupId) },
                    verifyOnServer = { cloudDataSource.verifySubunitOnServer(groupId, it) },
                    markSynced = { localDataSource.updateSyncStatus(it, SyncStatus.SYNCED) },
                    entityLabel = "subunit",
                    logContext = "for group $groupId"
                )
            }
        }
```

**Reference:** `SubunitRepositoryImpl` (cleanest, all delegates), `CashWithdrawalRepositoryImpl` (mixed with batch operations), `GroupRepositoryImpl` (`subscribeAndReconcile` only; unique create/delete patterns).

**🛑 Critical: Single-Subscription Rule (Prevent Duplicate Listeners)**
* `onStart` fires every time the Flow gets a new collector (`WhileSubscribed` resubscription, config changes, `flatMapLatest` restarts).
* Launching into `syncScope` without cancelling the previous Job **leaks** snapshot listeners — each keeps reconciling Room independently.
* ✅ **Mandatory:** Track the subscription as a `Job` and **cancel before re-launching**.
    * Single-key (e.g., groups): `private var cloudSubscriptionJob: Job?`
    * Multi-key (e.g., expenses per group): `private val cloudSubscriptionJobs = ConcurrentHashMap<String, Job>()`
* ❌ **Bad:** `syncScope.launch { subscribeToCloudChanges() }` (leaks on every resubscription)
* ✅ **Good:** `cloudSubscriptionJob?.cancel(); cloudSubscriptionJob = syncScope.launch { ... }`

**🛑 Critical: Subcollection Cleanup on Deletion**
* Firestore does **NOT** auto-delete subcollections when a parent document is deleted.
* If a real-time listener watches a **subcollection** (e.g., `group_members`), you **MUST** delete subcollection documents **BEFORE** the parent document.
* ❌ **Bad:** Only deleting the group document → orphaned member docs remain → listener on other devices never fires → deleted group still appears.
* ✅ **Good:** Delete all `members` subcollection docs first → listener fires on other devices → then delete the group document.

**🛑 Critical: Merge Reconciliation (Preserve Unsynced Local Data)**
* ❌ **Bad:** `deleteAll() + insertAll(remote)` — destroys locally-created items not yet in the cloud snapshot (offline-created groups/expenses).
* ✅ **Good:** `upsertAll(remote) + deleteByIds(staleIds)` — upsert remote data, then only remove local IDs that are NOT in the remote set.
* **Why it's safe:** Firestore's latency compensation includes pending local writes in snapshot emissions, so unsynced items appear in the remote set almost immediately. The merge strategy adds an extra safety net for the narrow race where a snapshot fires before the Firestore SDK caches the pending write.
* **DAO Pattern:**
    ```kotlin
    @Transaction
    suspend fun replaceExpensesForGroup(groupId: String, expenses: List<ExpenseEntity>) {
        val remoteIds = expenses.map { it.id }.toSet()
        val localIds = getExpenseIdsByGroupId(groupId)
        val staleIds = localIds.filter { it !in remoteIds }
        insertExpenses(expenses)          // @Upsert
        if (staleIds.isNotEmpty()) {
            deleteExpensesByIds(staleIds)  // Selective removal
        }
    }
    ```

### 6.4 DataStore Best Practices
* When saving IDs (e.g., `selectedGroupId`), **ALWAYS** save the corresponding human-readable metadata (e.g., `selectedGroupName`) to prevent UI blank states on app restart.

---

## 7. 🎨 UX & Design System

* **ScreenUiProvider (MainScreen Orchestration):**
    * Features hosted in the Bottom Tabs **must** implement `ScreenUiProvider` in their DI module.
    * This allows each screen to define its own **`TopAppBar`** (title, actions) and **`FAB`**, which the `MainScreen` will render.
    * *Do not* implement a `Scaffold` with a TopBar inside the individual feature screen if it is a main tab screen.
* **Scaffold:** Full-screen features (non-tab) use `FeatureScaffold`.
* **Notifications:** Do NOT use `Scaffold(snackbarHost = ...)`. Use `LocalTopPillController` for transient feedback (top pill notifications). Never use bottom snackbars.
* **Loading:** Avoid standard circular loaders for lists. Use **`ShimmerLoading`** components.
* **Empty States:** Use **`EmptyStateView`** from `:core:design-system`.
* **Cards:** Use **`FlatCard`** from `:core:design-system` for all card containers. Never use raw `Surface(…)` with manual border/color/shape for cards.
* **Formatting:** Use `AmountFormatter` and `DateFormatter` from `:core:design-system`.

**Bottom Padding & `LocalBottomPadding` (CRITICAL — Prevents Content Hidden by Bottom Nav):**
* The `MainScreen` uses a **floating bottom navigation bar** that overlays content. If screens don't account for this, the last list items, FABs, and buttons will be hidden behind the nav bar.
* ✅ **Mandatory:** All tab screens (`ScreenUiProvider`-hosted screens) **MUST** read `LocalBottomPadding.current` and apply it as bottom content padding.
    * **For scrollable lists (LazyColumn / LazyGrid):** Use `contentPadding = PaddingValues(bottom = bottomPadding)`.
    * **For FABs rendered inside screens (not via `ScreenUiProvider.fab`):** Add `Modifier.padding(bottom = bottomPadding)` to position above the nav bar.
    * **For bottom-anchored buttons / actions:** Include `bottomPadding` in the `Spacer` or `padding` below the last interactive element.
* ❌ **Bad:** Hardcoding `padding(bottom = 80.dp)` — the value is dynamic and depends on the Scaffold's `innerPadding`.
* ✅ **Good:** `val bottomPadding = LocalBottomPadding.current` → use this value.
* *Reference:* See `ExpensesScreen`, `GroupsScreen`, `BalancesScreen` for correct usage.
* **Non-tab screens** using `FeatureScaffold` do **not** need `LocalBottomPadding` — the Scaffold handles padding via `innerPadding`.

---

## 8. 🧪 Testing Strategy

* **Unit Tests:** JUnit 5 + MockK.
* **View Models:** Test strictly inputs (`onEvent`) vs outputs (`StateFlow` / `SharedFlow`).
* **Domain:** Mappers and Services must be tested with varying Locales/Zones using `LocaleProvider` fakes.

**Assertions (CRITICAL — Android Runtime Pitfall):**
* ❌ **NEVER** use Kotlin's `assert()` in any test (unit or instrumentation).
    * *Reason:* Kotlin's `assert()` compiles to a JVM `assert` bytecode instruction, which is a **no-op** on Android (Dalvik/ART) unless `-ea` is explicitly enabled. Tests silently pass even when the condition is false.
* ✅ **ALWAYS** use JUnit assertions: `Assert.assertTrue(...)`, `Assert.assertEquals(...)`, `Assert.assertNotNull(...)`, etc.
* ✅ Or use MockK's `verify` / `coVerify` for interaction assertions.

```kotlin
// ❌ BAD — silently passes even if wasCompleted is false
assert(wasCompleted) { "Expected callback to fire" }

// ✅ GOOD — fails loudly with a clear message
Assert.assertTrue("Expected callback to fire", wasCompleted)
```

### 8.1 🧵 Coroutine Testing (CRITICAL - Prevents Flaky Tests)

When testing classes that launch background coroutines (e.g., Repositories with `syncScope.launch {}`), you **MUST** inject the `CoroutineDispatcher` to ensure deterministic test behavior.

**The Problem:**
Tests using `advanceUntilIdle()` only control coroutines running on the **test dispatcher**. If your class creates its own `CoroutineScope(Dispatchers.IO)`, those coroutines run on a **real dispatcher** that the test cannot control. This causes:
* ✅ Tests pass locally (faster machine, lucky timing)
* ❌ Tests fail on CI/GitHub Actions (slower, different scheduling)

**❌ BAD - Hardcoded Dispatcher (Flaky Tests):**
```kotlin
class GroupRepositoryImpl(
    private val cloudDataSource: CloudGroupDataSource,
    private val localDataSource: LocalGroupDataSource
) : GroupRepository {
    // ❌ NOT testable - test cannot control this scope
    private val syncScope = CoroutineScope(Dispatchers.IO)
    
    override suspend fun deleteGroup(groupId: String) {
        localDataSource.deleteGroup(groupId)
        syncScope.launch {
            cloudDataSource.deleteGroup(groupId) // Runs on real IO dispatcher
        }
    }
}

// ❌ This test is FLAKY - advanceUntilIdle() has no effect on Dispatchers.IO
@Test
fun `syncs to cloud in background`() = runTest {
    repository.deleteGroup("123")
    advanceUntilIdle() // Does NOT wait for syncScope coroutines!
    coVerify { cloudDataSource.deleteGroup("123") } // May fail randomly
}
```

**✅ GOOD - Injected Dispatcher (Deterministic Tests):**
```kotlin
class GroupRepositoryImpl(
    private val cloudDataSource: CloudGroupDataSource,
    private val localDataSource: LocalGroupDataSource,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO  // ✅ Injectable with default
) : GroupRepository {
    private val syncScope = CoroutineScope(ioDispatcher)  // ✅ Uses injected dispatcher
    
    override suspend fun deleteGroup(groupId: String) {
        localDataSource.deleteGroup(groupId)
        syncScope.launch {
            cloudDataSource.deleteGroup(groupId)
        }
    }
}

// ✅ Test provides StandardTestDispatcher - advanceUntilIdle() now works!
@OptIn(ExperimentalCoroutinesApi::class)
class GroupRepositoryImplTest {
    private val testDispatcher = StandardTestDispatcher()
    
    @BeforeEach
    fun setUp() {
        repository = GroupRepositoryImpl(
            cloudDataSource = mockk(relaxed = true),
            localDataSource = mockk(relaxed = true),
            ioDispatcher = testDispatcher  // ✅ Inject test dispatcher
        )
    }
    
    @Test
    fun `syncs to cloud in background`() = runTest(testDispatcher) {  // ✅ Same dispatcher
        repository.deleteGroup("123")
        advanceUntilIdle()  // ✅ Now correctly waits for all coroutines
        coVerify { cloudDataSource.deleteGroup("123") }  // ✅ Deterministic
    }
}
```

**Key Rules:**
1. **Always inject `CoroutineDispatcher`** into classes that launch background coroutines.
2. **Provide a default** (`= Dispatchers.IO`) so production code doesn't need to specify it.
3. **Use `StandardTestDispatcher()`** in tests and pass it to both the class and `runTest()`.
4. **Call `runTest(testDispatcher)`** - the dispatcher must match what's used in the class.
5. **Call `advanceUntilIdle()`** before assertions to ensure background work completes.

---

## 9. 🔍 Static Analysis & Code Quality

**Detekt** (code quality/complexity), **Ktlint** (formatting), and **CodeQL** (security) are configured for all subprojects. **CPD** (copy-paste detection), **JaCoCo** (code coverage), and **Konsist** (architecture rule enforcement) complement the existing tooling.

**Configuration Locations:**
* Detekt rules: `config/detekt/detekt.yml`
* Ktlint formatting rules: `.editorconfig`
* CPD, JaCoCo: `build.gradle.kts` (root)
* Konsist architecture tests: `konsist-tests/`
* CI workflow (ktlint + detekt + CPD): `.github/workflows/static-analysis.yml`
* CI workflow (JaCoCo + Konsist): `.github/workflows/coverage-and-architecture.yml`

**Code Quality Rules for Generated Code:**
* New code **must not** introduce new detekt findings. Detekt uses `ignoreFailures = true` locally; gating is done by GitHub Code Scanning's "Code scanning results" check (only new alerts block PRs).
* Formatting **must** comply with ktlint rules defined in `.editorconfig`.
* Pre-commit hook runs **ktlint only** (fast). Detekt, CPD, JaCoCo, and Konsist run in CI only.
* Reference `config/detekt/detekt.yml` for threshold values (e.g., `CognitiveComplexMethod: 15`, `LongMethod: 60`, `LongParameterList.functionThreshold: 8`).
* See `wiki/code-quality-and-static-analysis.md` for full documentation.

---

## 10. 🤖 AI Agent Behavior Rules (CRITICAL)

These rules govern how AI assistants (Copilot, agents) interact with this codebase.

**Pre-Implementation Research (MANDATORY):**
* Before writing ANY implementation or action plan, the AI agent **MUST** read and internalize:
    1. This file (`.github/copilot-instructions.md`) — the full Technical Manifesto.
    2. `AGENTS.md` — the condensed project overview and constraints.
    3. All relevant `wiki/*.md` articles — especially those related to the feature being implemented.
    4. `wiki/core-services-catalog.md` — the comprehensive catalog of all reusable components, formatters, and domain services. **Always check here before creating new services or utilities to avoid duplication.**
    5. Existing reference implementations (e.g., if building a new feature, study an existing feature module's structure: ViewModel, Handlers, Mapper, Screen, Feature, DI module, tests).
* ❌ **Bad:** Jumping straight into code generation without reading the architecture docs.
* ✅ **Good:** Reading all relevant instruction files, studying existing patterns, then proposing a plan that aligns with the documented architecture.

**No Pragmatic Patches (STRICT):**
* You will provide clean, production-ready code. Quick hacks, "temporary" patches, or code that compromises modularity and Clean Architecture boundaries are strictly prohibited.

**Establish a Baseline:**
* Before editing any files, run the full local validation check suite (`make check`) to establish the initial status of the codebase. Resolve any pre-existing violations on files you will touch before making other changes.

**File-Size Guards:**
* Always verify a file's line count using `wc -l` before and after editing it. If editing will push the file near or over 600 lines, extract event handlers, delegates, or components first.

**Commenting Policy:**
* Comment the *why*, never the *what*. Completely avoid/delete redundant comments that merely restate what code does.

**Local Verification Gate (MANDATORY):**
* Before declaring any task, issue, or review comment done, completed, addressed, or accomplished, you MUST run `make check` locally and ensure there are 0 failures. Never leave verification for CI/CD or the user to discover.

**No Autonomous Git/GitHub Operations (STRICT):**
* ❌ **NEVER** push code to any remote branch without explicit user permission.
* ❌ **NEVER** create Pull Requests without explicit user permission and confirmation of:
    * Branch naming convention (see `wiki/branching-versioning-release-strategy.md`).
    * PR target branch (e.g., `develop` for features, `main` for releases/hotfixes).
    * PR title and description format.
* ❌ **NEVER** comment on GitHub issues or PRs without the user explicitly requesting it.
* ❌ **NEVER** merge PRs or close issues autonomously.
* ✅ **Good:** Prepare changes locally, present the plan/diff to the user, and wait for their explicit approval before any remote operation.

**Architecture Compliance Checklist (Before Generating Code):**
The AI agent must mentally verify each of these before writing code:
1. **ViewModels:** Only injecting UseCases, Mappers, Domain Services? No `Context`, `LocaleProvider`, Repositories?
2. **Formatting:** All formatting logic in Mappers? Nothing in ViewModels or Domain Services?
3. **Decimal Math:** Using `BigDecimal` (not `Double`) for ALL precision-sensitive calculations?
4. **Handler Delegation:** Does the ViewModel have >5 event categories? If so, extract into Handler classes.
5. **Bottom Padding:** Tab screens using `LocalBottomPadding.current`? FABs/buttons not hidden behind nav bar?
6. **Feature/Screen Split:** Screen is stateless (pure data + lambdas)? Feature is the orchestrator?
7. **MVI Triad:** `UiState` + `UiEvent` + `UiAction` all defined? No one-shot events in UiState?
8. **Hot Flows:** Using `stateIn(WhileSubscribed(AppConstants.FLOW_RETENTION_TIME, AppConstants.FLOW_REPLAY_EXPIRATION))`? No `LaunchedEffect(Unit)` for data loading?
9. **Offline-First:** Room-first reads? Local UUID generation? Cloud sync in background?
10. **ImmutableList:** Collections in UiState using `ImmutableList` from kotlinx-immutable?
11. **Local Verification Gate:** Local verification suite (`make check`) executed and passing with 0 failures?

