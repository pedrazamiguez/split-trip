This project implements specific User Experience (UX) principles to ensure the app feels polished, responsive, and native.

## 1. Progressive Disclosure & Delayed Navigation

**Context:** Selection screens (e.g., *Default Currency*).

When a user selects an item in a list that acts as a definitive action (single selection), we shouldn't force them to press "Back". However, navigating back immediately feels abrupt and leaves the user wondering if the tap "registered".

**Implementation:**

1. User taps item.
2. **Immediate Feedback:** The UI updates (checkbox appears) instantly.
3. **Delay:** We wait for `200ms` (defined as `UiConstants.NAV_FEEDBACK_DELAY`).
4. **Action:** Navigation pops back automatically.

## 2. Preventing FOUC & Skeleton Loading

**Context:** Loading async data (e.g., Fetching Expenses or User Preferences).

**The Problem:**
If we initialize a ViewModel state with a default value (e.g., "EUR") while loading the real value (e.g., "USD"), the user sees a "flicker": *EUR -> USD* in a split second. Similarly, showing a blank white screen while data loads makes the app feel slow.

**The Solution:**

* **ViewModel:** Initialize state as `null` or `Loading`.
* **Small UI (Text):** Use an invisible placeholder (e.g., `Modifier.alpha(0f)`) to reserve space and prevent layout shifts.
* **Complex UI (Lists):** Use **Shimmer (Skeleton) Loading**.

We use a standard `ShimmerLoadingList` component that mimics the layout of the actual content (Cards, Rows) with a pulsating gradient. This reduces perceived wait time.

**Anti-Flicker: Deferred Loading Container**

When responses are fast (< 150ms), showing a shimmer skeleton and immediately replacing it with content creates an ugly flicker — the "flash of loading state". We solve this with `DeferredLoadingContainer`:

1.  **Show delay** (`LOADING_SHOW_DELAY_MS = 150ms`): The shimmer is NOT shown immediately. If data arrives within this window, the shimmer is skipped entirely and content appears instantly.
2.  **Minimum display time** (`LOADING_MIN_DISPLAY_TIME_MS = 500ms`): If the shimmer *does* appear (because loading took longer than the delay), it stays visible for at least 500ms so it doesn't flash and disappear.
3.  **Visual continuity on reload**: When content was previously displayed and a reload starts (`isLoading` transitions from `false` to `true`) *while the composable stays in composition*, the previous content remains visible during the show-delay window instead of rendering a blank frame. This smooths over brief reloads (e.g., pull-to-refresh or a `stateIn` resubscribe on the same screen). On first-ever load, or when the composable has been removed from composition (e.g., switching away from a tab whose content is disposed and then returning), the behavior is unchanged — a blank frame is shown during the delay. Cross-tab visual continuity is instead handled at the flow layer by `FLOW_REPLAY_EXPIRATION`, which resets the `stateIn` replay cache to `initialValue`.

```kotlin
// UI Implementation — wrap with DeferredLoadingContainer
DeferredLoadingContainer(
    isLoading = uiState.isLoading,
    loadingContent = { ShimmerLoadingList() }
) {
    when {
        uiState.items.isEmpty() -> { EmptyStateView(...) }
        else -> { LazyColumn { ... } }
    }
}
// Transient errors (network failures, validation) are shown via LocalTopPillController,
// not inline error views. See TopPillNotification for the standard pattern.
```

Both timing constants live in `UiConstants` and can be overridden per call-site if a specific screen needs different thresholds.

## 3. Micro-Interactions & Delight

**Context:** The Primary Floating Action Button (FAB).

Material Design 3 encourages "Expressive" motion. We avoid static, boring interactions.

**Implementation:**
The `ExpressiveFab` uses a custom `Morph` shape:

* **Idle State:** An organic 7-point "blob" or "star" shape.
* **Pressed State:** Smoothly morphs into a rounded "flower" shape.
* **Touch Feedback:** Scales down slightly (`0.9x`) on press.
* **Idle Breathing Animation (opt-in):** When `enableIdleAnimation = true`, the FAB gently oscillates vertically (~4px, 3s cycle) using `rememberInfiniteTransition`. This draws attention to the primary action without being distracting. Off by default for backward compatibility.

These subtle animations make the app feel tactile and alive without blocking the user's task.

### 3.1 Scroll-Aware FAB Auto-Hide

Single-FAB screens that still use `ExpressiveFab` **hide the FAB when scrolling down** and show it when scrolling up or idle at the top. This reduces the permanent overlay problem and gives the list more breathing room.

**Utility:** `rememberScrollAwareFabVisibility(listState: LazyListState): Boolean` — a composable in `:core:design-system` that returns `true` (show) or `false` (hide) by tracking `firstVisibleItemIndex` + `firstVisibleItemScrollOffset` deltas via `snapshotFlow` inside a `LaunchedEffect`.

**Usage:** Use `ScrollAwareFabContainer` — it keeps the FAB always composed (preserving shared element transitions) while smoothly animating alpha and vertical translation. **Do NOT use `AnimatedVisibility`** — it removes the FAB from composition, breaking shared element return animations.

```kotlin
ScrollAwareFabContainer(
    listState = listState,
    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).padding(bottom = bottomPadding)
) {
    ExpressiveFab(onClick = ..., icon = ..., contentDescription = ...)
}
```

### 3.2 Dynamic Main Action Button (Primary Creation Pattern)

For the main list screens (Groups, Expenses, Subunits), the primary creation action uses a **Dynamic Main Action Button** integrated directly into the floating bottom navigation bar.

* **Space-efficient** — sits side-by-side with the bottom navigation pill when active.
* **Expressive animations** — utilizes lateral spring-physics transitions to show/hide the button, causing the bottom navigation pill to dynamically shrink/expand in width.
* **Rounded pill shape** — measures `80.dp` x `64.dp` and is fully rounded (`CircleShape` clipped).
* **Primary gradient** — uses a linear gradient from `primary` to `primaryContainer` with a standard `0.35f` lerp blend fraction.
* **Shared element transitions** — supports `sharedTransitionKey` via `fabSharedTransitionModifier`, enabling container-transform animations to destination screens.

### 3.3 Contextual Inline Actions (Balances)

For screens with **multiple creation actions** (e.g., Balances: "Add Money" + "Withdraw Cash"), we use **inline action buttons inside the relevant card** instead of floating FABs or sticky bars. This follows the "contextual actions near the data they affect" UX principle and eliminates the dual-action overlay problem.

The `GroupPocketBalanceCard` contains two `Button`s (filled, primary/tertiary) at the bottom, giving both actions strong visual prominence without floating over list content. Both buttons participate in shared element transitions with their respective destination screens.

### 3.4 Top-Bar Actions Rule

**Strict:** Top-bar `IconButton` actions must **always** have functional `onClick` handlers. A non-functional icon is worse than no icon — it erodes user trust and makes the app feel unfinished. If a feature (Search, Filter, Info) is not yet implemented, **do not render the icon**. Add it back when the functionality is ready.

### 3.5 Top Pill Notifications

Transient feedback (success messages, errors, network failures) uses **top pill notifications** instead of bottom snackbars. Pills drop from the top of the screen with a slide-in + fade animation and auto-dismiss after 3 seconds.

**Why pills over snackbars?**
* **No overlap with navigation:** Bottom snackbars compete with floating bottom bars and action buttons. Top pills avoid this entirely.
* **More visible:** Content at the top of the screen catches the eye during form submissions and navigation transitions.
* **Simpler API:** `pillController.showPill(message)` — no duration, no action labels, no dismiss callbacks.

**Usage in Features:**
```kotlin
val pillController = LocalTopPillController.current

LaunchedEffect(Unit) {
    viewModel.actions.collectLatest { action ->
        when (action) {
            is UiAction.ShowMessage -> {
                pillController.showPill(message = action.message.asString(context))
            }
        }
    }
}
```

### 3.6 Flat Surface Cards

All card-like containers use the **`FlatCard`** composable from `:core:design-system` — zero elevation, `surfaceContainerLow` background (inset tier — slightly tinted relative to the off-white `surface` page background, giving a grounded feel rather than a floating one). This replaces Material 3's default elevated `Card`:

```kotlin
// Standard usage — defaults to shapes.large, surfaceContainerLow, no border
FlatCard(modifier = Modifier.fillMaxWidth()) {
    // card content
}

// Override for variations
FlatCard(
    shape = MaterialTheme.shapes.medium,                         // nested cards
    color = MaterialTheme.colorScheme.primaryContainer           // selected state
) { ... }

// Hero / featured card with ambient shadow (Horizon Narrative §4.4)
// Light mode: shadow rendered. Dark mode: suppressed automatically.
FlatCard(
    modifier = Modifier.fillMaxWidth(),
    elevation = 8.dp
) { ... }
```

**Never** use raw `Surface(…)` with manual `BorderStroke`/`color`/`shape` for card containers — always use `FlatCard` to guarantee consistency.

**Rationale:** Flat surfaces feel more modern and reduce visual noise from shadows. The subtle border provides enough differentiation without competing with content. Centralizing the pattern in `FlatCard` avoids DRY violations and ensures all cards update together if the design tokens change.

Hero cards that visually "float" above the list (e.g., the selected group hero card) are a documented exception — they use `FlatCard(elevation = …)` to add an ultra-diffused ambient shadow in light mode only (§4.4). For cards participating in shared element transitions, animate the elevation externally and pass the state value to `FlatCard`; see Horizon Narrative §4.4 for the full pattern.

### 3.7 Inline Typographic Headers

The three main tab screens (Groups, Expenses, Balances) use **inline scrollable headers** instead of a traditional `TopAppBar`. The header is a 32sp bold `Text` rendered as the first `LazyColumn` item, scrolling naturally with the content.

```kotlin
item(key = "header") {
    Column {
        Text(
            text = stringResource(R.string.groups_title),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.groups_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

**Rationale:** Removes the collapsible `LargeTopAppBar` complexity and gives the content a bold, editorial feel. Non-tab screens (wizards, sub-screens) continue using `DynamicTopAppBar` with back navigation.

## 4. Edge-to-Edge & Glassmorphism

**Context:** The Main Screen and Bottom Navigation.

We strictly follow modern Android **Edge-to-Edge** guidelines.

* **Content:** Lists scroll *behind* the Bottom Navigation Bar and Status Bar. We manually handle `WindowInsets` to ensure the last item is not obscured.
* **Glass Effect:** The Bottom Bar uses a library called **Haze** to create a real-time blur effect (frosted glass) over the scrolling content behind it, providing depth and context.

## 5. Explicit Empty States

**Context:** Lists with no data.

An empty white screen looks like a bug. We use a standardized `EmptyStateView` component when a list is valid but empty.

**Guidelines:**

* **Icon:** Large, outlined icon relevant to the feature.
* **Title:** Clear statement ("No expenses yet").
* **Description:** Helpful guidance ("Tap the + button to add one").
* **Alignment:** Centered vertically and horizontally.

## 6. Stepped Wizard Pattern

**Context:** All multi-field forms in the app are migrated to a stepped wizard pattern for consistency (epic #714).

**Pattern:**
Every form that collects more than a couple of fields uses a shared `WizardStepIndicator` + `AnimatedContent` step transitions + `WizardNavigationBar`. Each step is a focused composable (≤ 60 lines).

**Implementation highlights:**

1. **Step Enum with Factory:** Each form defines an enum (e.g., `AddExpenseStep`, `CashWithdrawalStep`) with a `companion object` containing an `applicableSteps(...)` factory that dynamically includes/excludes conditional steps based on form state.
2. **Conditional Steps & Clamping:** When a state change removes a conditional step (e.g., switching currency back to the group currency removes the EXCHANGE_RATE step), the `withStepClamped()` method on the UiState ensures `currentStep` falls back to the nearest valid step.
3. **Wizard Navigation in ViewModel:** `NextStep` and `PreviousStep` events are handled inline in the ViewModel. On the first step, `PreviousStep` emits a `NavigateBack` UiAction that the Feature routes to `navController.popBackStack()`.
4. **BackHandler:** The Feature composable intercepts the system back button via `BackHandler` and delegates to `PreviousStep`, so the wizard navigates backward before exiting.
5. **Optional Steps & Skip-to-Review:** Steps can be marked as optional via `isOptional = true` in the step enum constructor. Optional steps render with a dashed-border circle in `WizardStepIndicator` and show a "Skip to Review →" link below the indicator. Tapping the link fires a `JumpToReview` event that jumps directly to the REVIEW step and records the departure step in `jumpedFromStep`. Pressing Back on REVIEW returns to the departure step instead of the previous sequential step. Steps that are NOT optional never show the skip link. See `AddExpenseStep` (CATEGORY, VENDOR_NOTES, RECEIPT, ADD_ONS are optional) and `CashWithdrawalStep` (DETAILS is optional).
6. **WizardSkipStrategy Enum:** A `WizardSkipStrategy` enum in `:core:design-system` classifies steps as `REQUIRED` or `OPTIONAL` for documentation purposes. The feature-level step enums use a simpler `isOptional: Boolean` property.

**Reference implementations:**
- `AddCashWithdrawalScreen` / `CashWithdrawalStep` — 7 steps with 1 optional (DETAILS).
- `AddExpenseScreen` / `AddExpenseStep` — 13 steps: TITLE → PAYMENT_METHOD → FUNDING_SOURCE → CONTRIBUTION_SCOPE (conditional) → AMOUNT → EXCHANGE_RATE (conditional) → SPLIT (conditional) → CATEGORY (optional) → VENDOR_NOTES (optional) → PAYMENT_STATUS → RECEIPT (optional) → ADD_ONS (optional) → REVIEW.
- `AddContributionScreen` / `AddContributionStep` — 3 steps, all required (no optional steps).
