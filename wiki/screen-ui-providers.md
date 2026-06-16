The **ScreenUiProvider** pattern is a mechanism used to decouple the "Chrome" of a screen (TopAppBar, FloatingActionButton) from the screen's content and the main navigation host.

## The Problem

In a modular app, the `MainScreen` (which holds the `Scaffold`) doesn't know about specific screens inside feature modules. We wanted to avoid a monolithic `when(route)` statement in `MainScreen` to determine which Title or FAB to show.

## The Solution

We define an interface `ScreenUiProvider` in **`:core:design-system`**. Each Feature module implements this interface for its screens to expose their UI requirements.

### 1. The Interface

```kotlin
interface ScreenUiProvider {
    val route: String
    val topBar: (@Composable () -> Unit)? get() = null
    val mainAction: MainAction? @Composable get() = null
}

```

### 2. Implementation (Feature Module)

In the feature module, we create an implementation binding it to a specific route.

**Tab screens** (Groups, Expenses, Balances) use inline typographic headers inside the `LazyColumn` instead of a `DynamicTopAppBar`, so their `ScreenUiProvider` only declares the `route`:

```kotlin
class ExpensesScreenUiProviderImpl(
    override val route: String = Routes.EXPENSES
) : ScreenUiProvider
```

**Non-tab screens** that need a top bar (e.g., write-flow wizards, sub-screens with back navigation) use `DynamicTopAppBar`:

```kotlin
class SubunitManagementScreenUiProviderImpl(
    override val route: String = Routes.SUBUNIT_MANAGEMENT
) : ScreenUiProvider {

    override val topBar: @Composable () -> Unit = {
        DynamicTopAppBar(
            title = stringResource(R.string.subunit_management_title),
            subtitle = stringResource(R.string.subunit_management_subtitle)
        )
    }
}

```

> **⚠️ No placeholder actions.** The `actions` block in `DynamicTopAppBar` must ONLY contain `IconButton`s with **functional** `onClick` handlers. If a feature (Search, Filter, Info) is not yet implemented, **omit the `actions` parameter entirely** rather than rendering an icon with an empty handler. Dead icons erode user trust. See `wiki/ux-ui-concepts.md` § 3.4.

### 3. Dependency Injection

We register it in Koin as a `ScreenUiProvider`:

```kotlin
single { ExpensesScreenUiProviderImpl() } bind ScreenUiProvider::class

```

### 4. Consumption (The "Magic")

In `MainScreen`, we inject **all** providers and find the matching one for the current route:

```kotlin
val providers: List<ScreenUiProvider> = getKoin().getAll()
val currentProvider = providers.find { it.route == currentRoute }

Scaffold(
    topBar = { currentProvider?.topBar?.invoke() },
    bottomBar = {
        BottomNavigationBar(
            selectedRoute = selectedRoute,
            onTabSelected = { selectedRoute = it },
            items = visibleProviders,
            mainAction = currentProvider?.mainAction
        )
    }
) { content() }

```

---

## 🏗️ Design System Components

We have created specialized components to ensure consistency across providers:

### `DynamicTopAppBar`

A wrapper around Material 3's `LargeTopAppBar` that automatically handles:

* **Scroll Behavior:** Collapses smoothly when the list scrolls (hooked into `MainScreen`'s scroll state).
* **Title/Subtitle:** Supports a subtitle that fades out as the bar collapses.
* **Back Navigation:** Automatically shows the back arrow if an `onBack` callback is provided.

### `ExpressiveFab`

A custom FAB with organic shapes:

* **Idle:** A 7-point "star" (blob shape).
* **Pressed:** Morphs into a "flower" shape for tactile feedback.
* **Shared Element:** Supports `sharedTransitionKey` to expand into a full screen (e.g., creating a new Expense).
* **Idle Breathing (opt-in):** `enableIdleAnimation = true` adds a subtle vertical floating animation (~4px, 3s cycle).
* **Scroll-Aware Auto-Hide:** Screens use `ScrollAwareFabContainer(listState)` to smoothly hide the FAB on scroll-down and show on scroll-up. This keeps the FAB always composed (preserving shared element transitions) — **do NOT use `AnimatedVisibility`**, which disposes the FAB and breaks return animations.

### `MainAction`

A data class modeling the primary screen action to be rendered directly in the floating bottom navigation bar:

* **Sleek integration** — rendered side-by-side with the bottom navigation pill.
* **Organic lateral transitions** — slides, expands, and fades in/out with spring physics.
* **Pill-shaped CTA** — uses a `80.dp` x `64.dp` rounded pill with a primary gradient.
* **Container transform** — supports `sharedTransitionKey` for container-transform transitions.

---

## Shared Element Transitions for Main Actions

To support container-transform transitions from the main action button (e.g., into the Create Group or Add Expense screens), the `MainAction` data class exposes a `sharedTransitionKey` field. When provided, the design system automatically applies `fabSharedTransitionModifier(key)` to the button in the bottom navigation bar, matching the container transition on the target screen.

## 🏷️ Screens Without FABs: Inline Card Actions

Not every screen needs a floating action button or sticky bar. When a screen has **multiple contextual actions** tied to a specific data card (e.g., the Balances screen's "Add Money" and "Withdraw Cash"), the preferred pattern is to embed action buttons **directly inside the card** as filled `Button`s with primary/tertiary colors. This reduces visual clutter, eliminates dual-action overlays, and places actions near the data they affect. Both buttons participate in shared element transitions with their destination screens.

*Example: The Balances screen embeds "Add Money" and "Withdraw Cash" buttons inside `GroupPocketBalanceCard` instead of using FABs or sticky bars.*

