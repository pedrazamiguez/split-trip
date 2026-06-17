# Core Services & Components Catalog

> **Purpose:** A single reference for developers and AI agents to discover all reusable components, formatters, and domain services — before writing new code. If you need to format a number, split an expense, validate input, or display a loading state, check here first.

---

## Table of Contents

- [A. Design-System UI Components](#a-design-system-ui-components)
  - [A.1 Scaffold & Navigation](#a1-scaffold--navigation)
  - [A.2 Layout & Feedback](#a2-layout--feedback)
  - [A.3 Input](#a3-input)
  - [A.4 Currency](#a4-currency)
  - [A.5 Wizard](#a5-wizard)
  - [A.6 Form](#a6-form)
  - [A.7 Dialog & Sheet](#a7-dialog--sheet)
  - [A.8 Shared Transitions](#a8-shared-transitions)
  - [A.9 Chip](#a9-chip)
  - [A.10 Foundation Utilities](#a10-foundation-utilities)
- [B. Design-System Infrastructure](#b-design-system-infrastructure)
  - [B.1 CompositionLocals](#b1-compositionlocals)
  - [B.2 Contracts & Interfaces](#b2-contracts--interfaces)
  - [B.3 Top Bar](#b3-top-bar)
  - [B.4 Notification](#b4-notification)
  - [B.5 Extensions](#b5-extensions)
  - [B.6 Models & Constants](#b6-models--constants)
- [C. Design-System Formatters](#c-design-system-formatters)
  - [C.1 NumberFormatter](#c1-numberformatter)
  - [C.2 AmountFormatter](#c2-amountformatter)
  - [C.3 DateFormatter](#c3-dateformatter)
  - [C.4 FormattingHelper](#c4-formattinghelper)
- [D. Design-System Preview Utilities](#d-design-system-preview-utilities)
- [E. Domain Services](#e-domain-services)
  - [E.1 Calculation Services](#e1-calculation-services)
  - [E.2 Split Services](#e2-split-services)
  - [E.3 Add-On Services](#e3-add-on-services)
  - [E.4 Validation Services](#e4-validation-services)
  - [E.5 Membership & Auth Services](#e5-membership--auth-services)
  - [E.6 Infrastructure Services](#e6-infrastructure-services)
- [F. Domain Converter](#f-domain-converter)
- [G. Data Layer Sync Delegates](#g-data-layer-sync-delegates)
  - [G.1 KeyedSubscriptionTracker](#g1-keyedsubscriptiontracker)
  - [G.2 CloudSyncDelegates](#g2-cloudsyncdelegates)

---

## A. Design-System UI Components

**Module:** `:core:design-system`
**Package:** `...core.designsystem.presentation.component.*`

All components are `@Composable` functions following Material 3 design. They accept standard `Modifier` parameters and are stateless (state is lifted to the caller).

### A.1 Scaffold & Navigation

| Component | File | Purpose |
|---|---|---|
| `FeatureScaffold` | `scaffold/FeatureScaffold.kt` | Standard scaffold for full-screen (non-tab) features. Provides a `Scaffold` with consistent styling, TopAppBar integration, and inner padding handling. **Use for:** Any screen navigated via `LocalRootNavController` or `LocalTabNavController` that is NOT a bottom-tab screen. |
| `ExpressiveFab` | `scaffold/ExpressiveFab.kt` | Material 3 Expressive FAB with icon and label. Supports expand/collapse animation, optional idle breathing animation (`enableIdleAnimation`). Also provides `LargeExpressiveFab` variant. **Use for:** Primary actions inside screen composables where traditional FAB layout is preferred over the bottom bar integration. |
| `rememberScrollAwareFabVisibility` | `scaffold/ScrollAwareFabVisibility.kt` | Composable utility that returns `Boolean` based on `LazyListState` scroll direction via `snapshotFlow`. Returns `true` (show FAB) when scrolling up/idle, `false` (hide FAB) when scrolling down. Prefer using `ScrollAwareFabContainer` which encapsulates the animation. |
| `ScrollAwareFabContainer` | `scaffold/ScrollAwareFabVisibility.kt` | Composable container that keeps its FAB content always composed (preserving shared element transitions) while animating alpha + translationY via `graphicsLayer`. Accepts an optional `visible` parameter for additional conditions. **Use instead of `AnimatedVisibility`** to avoid breaking shared element return animations. |
| `NavigationBarIcon` | `scaffold/NavigationBarIcon.kt` | A single bottom navigation bar item with icon, label, and selected state. **Use for:** Bottom navigation tabs in `MainScreen`. |

### A.2 Layout & Feedback

| Component | File | Purpose |
|---|---|---|
| `ShimmerLoadingList` | `layout/ShimmerLoading.kt` | Animated placeholder list for loading states. Also exposes `shimmerBrush()`, `ShimmerBox`, and `ShimmerItemCard` for custom shimmer layouts. **Use instead of:** Circular progress indicators for list loading. |
| `EmptyStateView` | `layout/EmptyStateView.kt` | Displays icon, title, and optional description when a list/screen has no content. **Use for:** Empty groups, no expenses, no balances, etc. |
| `FlatCard` | `layout/FlatCard.kt` | Standard flat card container: zero elevation, `surfaceContainerLow` background (Layering Principle inset tier — slightly tinted relative to the off-white page background), `shapes.large` corners, no border by default. Optional `elevation` parameter adds an ambient drop-shadow in light mode only (dark mode suppressed automatically per §4.4). Opt-in `ghostBorder = true` for dark-mode edge cases. Override `shape` or `color` only for documented variations (e.g., `shapes.medium` for nested cards, `primaryContainer` for selected state). **Use instead of:** Raw `Surface(…)` with manual border/color for card-style containers. |
| `SectionCard` | `layout/SectionCard.kt` | A card container with a title section header. Built on `FlatCard`. **Use for:** Grouping related content in forms or detail screens. |
| `AnimatedAmount` | `layout/AnimatedAmount.kt` | Animates numeric text changes with a smooth counter effect. **Use for:** Balance totals, expense amounts that change dynamically. |
| `DeferredLoadingContainer` | `layout/DeferredLoadingContainer.kt` | Delays showing loading indicator until a threshold has passed, preventing brief loading flashes. **Use for:** Wrapping content that may load quickly — avoids shimmer flicker on fast connections. |

### A.3 Input

| Component | File | Purpose |
|---|---|---|
| `StyledOutlinedTextField` | `input/StyledOutlinedTextField.kt` | App-standard "Soft Field" text field (Horizon §5): `surfaceContainerLow` background, no border at rest, ghost indicator on focus. Also provides `softFieldColors()`. **Use for:** All text inputs across the app. |
| `SearchableChipSelector<T>` | `input/SearchableChipSelector.kt` | A searchable dropdown that displays selected items as chips. Generic type `T` for any selectable item. **Use for:** Selecting members, categories, or other enumerable items from a local list. |
| `AsyncSearchableChipSelector<T>` | `input/AsyncSearchableChipSelector.kt` | Like `SearchableChipSelector` but with async search support (debounced query → results callback). **Use for:** Searching users by email (remote lookup). |

### A.4 Currency

| Component | File | Purpose |
|---|---|---|
| `CurrencyDropdown` | `currency/CurrencyDropdown.kt` | Dropdown selector for currencies with search/filter. **Use for:** Selecting source or group currency in expense/contribution forms. |
| `AmountCurrencyCard` | `currency/AmountCurrencyCard.kt` | Combined amount input + currency selector card. Uses `AmountCurrencyCardState` for state management. **Use for:** Entering monetary amounts with currency selection (e.g., expense source amount). |
| `CurrencyConversionCard` | `currency/CurrencyConversionCard.kt` | Displays source → group currency conversion with exchange rate input. Uses `CurrencyConversionCardState`. **Use for:** Multi-currency expense forms when source ≠ group currency. |

**State classes:**
- `AmountCurrencyCardState` — holds amount text, currency, error state, enabled/read-only flags.
- `CurrencyConversionCardState` — holds source amount, group amount, exchange rate, display rate, and currency metadata.

### A.5 Wizard

| Component | File | Purpose |
|---|---|---|
| `WizardStepLayout` | `wizard/WizardStepLayout.kt` | Container for a single wizard step: title, content slot, and consistent padding. **Use for:** Multi-step form screens (e.g., Add Expense wizard). |
| `WizardStepIndicator` | `wizard/WizardStepIndicator.kt` | Animated step dots/labels showing progress through wizard steps. Supports optional step indices (dashed-border visual), and a "Skip to Review" link. **Use for:** Top of wizard screens to show current step. |
| `WizardNavigationBar` | `wizard/WizardNavigationBar.kt` | Bottom bar with Back/Next/Submit buttons. Configured via `WizardNavigationBarConfig`. **Use for:** Wizard step navigation controls. |
| `WizardSkipStrategy` | `wizard/WizardSkipStrategy.kt` | Enum classifying wizard steps as `REQUIRED` or `OPTIONAL`. Used as documentation/reference; feature step enums use `isOptional: Boolean`. |

**Configuration class:**
- `WizardNavigationBarConfig` — data class defining button labels, visibility, and enabled state for each wizard navigation button.

### A.6 Form

| Component | File | Purpose |
|---|---|---|
| `GradientButton` | `form/GradientButton.kt` | Gradient CTA button with Box-based shadow and loading state. Parameterised via `GradientButtonColors` — use `GradientButtonDefaults.primaryColors()` (default), `.secondaryColors()`, or `.tertiaryColors()` to switch tiers. Supports `leadingIcon` and `trailingIcon`. |
| `SecondaryButton` | `form/SecondaryButton.kt` | Flat neutral button with `surfaceContainerHigh` fill, Box-based shadow, same height as `GradientButton`. Supports `isLoading` (replaces label with spinner), `leadingIcon`, and `trailingIcon`. **Use for:** Non-gradient secondary actions (Back, Cancel, Google Sign-In). |
| `DestructiveButton` | `form/DestructiveButton.kt` | Destructive-action button with `errorContainer` fill, Box-based shadow, and loading state. **Use for:** Dangerous actions (Logout, Delete). |
| `FormErrorBanner` | `form/FormErrorBanner.kt` | Animated error banner displayed at the top of a form. **Use for:** Showing validation errors that apply to the form as a whole (not a specific field). |
| `FormSubmitButton` | `form/FormSubmitButton.kt` | Full-width submit button with loading state. Delegates to `GradientButton`. **Use for:** Form submission buttons (Create Group, Save Expense, etc.). |

### A.7 Dialog & Sheet

| Component | File | Purpose |
|---|---|---|
| `DestructiveConfirmationDialog` | `dialog/DestructiveConfirmationDialog.kt` | Confirmation dialog with a destructive (red) action button. **Use for:** Delete confirmations (Delete Group, Delete Expense). |
| `ActionBottomSheet` | `sheet/ActionBottomSheet.kt` | Bottom sheet displaying a list of `SheetAction` items with icons. **Use for:** Contextual action menus (long-press on expense, group options). |
| `CopyableTextSheet` | `sheet/CopyableTextSheet.kt` | Bottom sheet displaying text that can be copied to clipboard. **Use for:** Sharing group invite codes, debug info. |

**Model class:**
- `SheetAction` — represents a single action in `ActionBottomSheet` (icon, label, onClick, optional destructive flag).

### A.8 Shared Transitions

| Component | File | Purpose |
|---|---|---|
| `SharedTransitionSurface` | `transition/SharedTransitionSurface.kt` | Full-screen `Surface` that participates in shared-element container-transform animations. **Use for:** Destination screens of FAB → screen transitions. |
| `LocalSharedTransitionScope` | `transition/SharedElements.kt` | CompositionLocal providing the `SharedTransitionScope` for animation coordination. |
| `LocalAnimatedVisibilityScope` | `transition/SharedElements.kt` | CompositionLocal providing the `AnimatedVisibilityScope` for enter/exit animations. |

### A.9 Chip

| Component | File | Purpose |
|---|---|---|
| `PassportChip` | `chip/PassportChip.kt` | Horizon Narrative signature travel chip — "collectible stamp" feel. Uses `secondaryFixedDim` (selected) / `surfaceContainerHigh` (unselected), `CircleShape`, no border (No-Line rule). Supports `leadingIcon` (checkmark) and `trailingIcon` (close/overflow). **Use instead of:** Stock `FilterChip` or `InputChip` for all selectable tags, categories, payment methods, and removable item chips. |

### A.10 Foundation Utilities

| Utility | File | Purpose |
|---|---|---|
| `GlassmorphismDefaults` | `foundation/GlassmorphismDefaults.kt` | Constants and `Modifier.horizonGlassEffect()` extension for the Horizon Narrative glass-blur recipe. Light mode: surface at 70% opacity + 20dp blur. Dark mode: surface at 60% opacity + 24dp blur. Accepts optional `HazeEffectScope` block for per-site customisation (e.g., gradient mask). **Use for:** Any floating UI element that needs frosted-glass depth (nav bars, top bars, modal sheets). Powered by `dev.chrisbanes.haze`. |

---

## B. Design-System Infrastructure

### B.1 CompositionLocals

| Local | File | Purpose |
|---|---|---|
| `LocalRootNavController` | `navigation/LocalRootNavController.kt` | Global (Activity-level) navigation controller. **Use for:** Full-screen flows (Login, Onboarding, Settings). |
| `LocalTabNavController` | `navigation/LocalTabNavController.kt` | Tab-level navigation controller inside `MainScreen`. **Use for:** Drill-down navigation within a bottom tab. |
| `LocalBottomPadding` | `navigation/LocalBottomPadding.kt` | Dynamic bottom padding value to account for floating bottom nav bar. **Must be applied** by all tab screens to prevent content from being hidden. |
| `LocalTopPillController` | `notification/TopPillNotification.kt` | Global top-pill notification controller that survives navigation. Consumed in Feature layer. |
| `LocalSharedTransitionScope` | `transition/SharedElements.kt` | Shared-element transition scope for container-transform animations. |
| `LocalAnimatedVisibilityScope` | `transition/SharedElements.kt` | Animated visibility scope for shared-element transitions. |
| `LocalTopAppBarState` | `topbar/TopAppBarScrollBehaviorProvider.kt` | Provides scroll-connected `TopAppBarState` for collapsible top bars. |

### B.2 Contracts & Interfaces

| Interface | File | Purpose |
|---|---|---|
| `NavigationProvider` | `navigation/NavigationProvider.kt` | Plugin interface for features to register as bottom tabs. Provides route, label, icon, and composable content. **Implement in:** Each tab feature module's DI setup (e.g., `GroupsNavigationProviderImpl`). |
| `TabGraphContributor` | `navigation/TabGraphContributor.kt` | Plugin interface for non-tab feature modules to contribute routes into an existing tab's NavHost. The host tab's `NavigationProvider` injects all `TabGraphContributor` instances via Koin and calls `contributeGraph(builder)`. **Implement in:** Standalone write-flow modules (e.g., `ContributionsTabGraphContributorImpl`, `WithdrawalsTabGraphContributorImpl`, `SubunitsTabGraphContributorImpl`). |
| `ScreenUiProvider` | `screen/ScreenUiProvider.kt` | Allows tab screens to declare their own TopAppBar title/actions and FAB without owning a Scaffold. **Implement in:** Each tab feature's DI setup. |
| `IntentProvider` | `provider/IntentProvider.kt` | Abstraction for creating Android intents (e.g., share, email). Keeps features free of direct `Intent` references. |
| `SharedViewModel` | `viewmodel/SharedViewModel.kt` | Shared ViewModel for cross-feature data passing within the Activity scope. Injected in Feature composables using `viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner`. |

### B.3 Top Bar

| Component | File | Purpose |
|---|---|---|
| `DynamicTopAppBar` | `topbar/DynamicTopAppBar.kt` | Animated `LargeTopAppBar` with collapsible title, subtitle fade-out, and scroll-synchronized color transitions. Falls back to standard `TopAppBar` when no scroll behavior is provided. **Use for:** Non-tab screens that need a top bar with scroll-aware behavior (wizards, sub-screens with back navigation). Tab screens use inline typographic headers instead. |
| `TopAppBarScrollBehaviorProvider` | `topbar/TopAppBarScrollBehaviorProvider.kt` | Provides `TopAppBarScrollBehavior` via `LocalTopAppBarState`. **Use for:** Connecting `LazyColumn` scroll state to the top bar collapse animation. |

### B.4 Notification

| Component | File | Purpose |
|---|---|---|
| `TopPillController` | `notification/TopPillNotification.kt` | Controller class with `showPill(message)` that uses MainScreen's `CoroutineScope` — survives feature navigation. Exposed via `LocalTopPillController`. **Use for:** Showing transient feedback from `UiAction` side effects in the Feature layer. Auto-dismisses after 3 seconds. |
| `TopPillNotification` | `notification/TopPillNotification.kt` | Animated pill composable that drops from the top of the screen with `slideInVertically` + `fadeIn`. Placed as an overlay in `MainScreen`. |
| `rememberTopPillController` | `notification/TopPillNotification.kt` | Creates and remembers a `TopPillController`. Called at the MainScreen level. |

### B.5 Extensions

| Extension | File | Purpose |
|---|---|---|
| `UiText.asString(context)` | `extension/UiTextExtensions.kt` | Resolves `UiText.StringResource` to a `String`. Called in Feature composables. |
| `Currency.getNameRes()` | `extension/CurrencyExtensions.kt` | Maps domain `Currency` enum to a string resource ID for the currency's display name. |
| `NavGraphBuilder.sharedComposable()` | `extension/NavGraphExtensions.kt` | Extension for declaring composable destinations with shared-element transition support. |
| `Modifier.sharedElementAnimation()` | `extension/SharedTransitionExtensions.kt` | Modifier extension for applying shared-element animation to a composable. |
| `NotificationPermissionEffect` | `permission/NotificationPermissionEffect.kt` | Composable side-effect that requests notification permission on Android 13+. |

### B.6 Models & Constants

| Item | File | Purpose |
|---|---|---|
| `CurrencyUiModel` | `model/CurrencyUiModel.kt` | UI model for currencies (code, symbol, display name). Used in dropdowns and selectors. |
| `Routes` | `navigation/Routes.kt` | All navigation route constants (`const val`). Single source of truth for route strings. |
| `UiConstants` | `constant/UiConstants.kt` | UI-layer constants (animation durations, sizes, etc.). |
| `NavigationUtils` | `navigation/NavigationUtils.kt` | Helper functions for common navigation patterns (popBackStack, navigate with arguments). |
| `DoubleTapBackToExitHandler` | `navigation/DoubleTapBackToExitHandler.kt` | Composable that implements "press back twice to exit" behavior. |
| `DomainConstants.DEFAULT_DECIMAL_PLACES` | `domain/.../constant/DomainConstants.kt` | `2` — canonical default decimal places for domain money math and parsing defaults. |
| `DomainConstants.RATE_PRECISION` | `domain/.../constant/DomainConstants.kt` | `6` — canonical intermediate precision for exchange-rate division operations. |
| `AppConstants.FLOW_RETENTION_TIME` | `core/common/.../AppConstants.kt` | `5000L` ms — `WhileSubscribed` stop timeout for hot flows. |
| `AppConstants.FLOW_REPLAY_EXPIRATION` | `core/common/.../AppConstants.kt` | `0L` ms — replay cache reset delay (prevents stale-state flash on tab re-entry). |
| `AppConstants.BALANCE_COMPUTATION_DEBOUNCE_MS` | `core/common/.../AppConstants.kt` | `300L` ms — debounce applied to the balance computation flow to absorb rapid Firestore reconciliation bursts. |

`AppConstants` remains the source of app-lifecycle/UI-layer timing constants; domain precision constants live in `DomainConstants` to keep module boundaries clean.

---

## C. Design-System Formatters

**Module:** `:core:design-system`
**Package:** `...core.designsystem.presentation.formatter`

> **Rule:** All formatting is done in **Mappers** (`:features:*/presentation/mapper/`), never in ViewModels or Domain Services. Mappers call these formatters, optionally via `FormattingHelper`.

### C.1 NumberFormatter

**File:** `formatter/NumberFormatter.kt`
**Type:** Top-level extension functions

| Function | Signature | Purpose |
|---|---|---|
| `formatNumberForDisplay` | `String.formatNumberForDisplay(locale, maxDecimalPlaces, minDecimalPlaces): String` | Formats an internal number string (dot decimal) to locale-aware display. E.g., `"37.22"` → `"37,22"` (ES). |
| `formatRateForDisplay` | `String.formatRateForDisplay(locale): String` | Shorthand for `formatNumberForDisplay` with 6 max decimals. For exchange rates. |
| `formatForDisplay` | `BigDecimal.formatForDisplay(locale, maxDecimalPlaces, minDecimalPlaces): String` | Formats a `BigDecimal` to locale-aware display string. |

**When to use:** Any time you need to display a number with locale-specific separators.

### C.2 AmountFormatter

**File:** `formatter/AmountFormatter.kt`
**Type:** Top-level extension functions + free functions

| Function | Signature | Purpose |
|---|---|---|
| `Expense.formatAmount()` | `Expense.formatAmount(locale): String` | Formats expense's group amount with group currency symbol. |
| `Expense.formatSourceAmount()` | `Expense.formatSourceAmount(locale): String` | Formats expense's source amount with source currency symbol. |
| `formatCurrencyAmount()` | `formatCurrencyAmount(amount: Long, currencyCode: String, locale: Locale): String` | Core function: converts cents to locale-aware currency string with proper symbol (e.g., `2550` + `"EUR"` → `"25,50 €"`). Handles native symbol resolution for non-local currencies. |
| `Currency.formatDisplay()` | `Currency.formatDisplay(): String` | Formats a domain `Currency` as `"EUR (€)"` with native symbol. |
| `parseAmountToSmallestUnit()` | `parseAmountToSmallestUnit(amountString: String, currencyCode: String): Long` | Parses user input to currency's smallest unit (cents/yen/millimes). Handles any currency's decimal places. |
| `formatAmountWithCurrency()` | `formatAmountWithCurrency(amountString: String, currencyCode: String, locale: Locale): String` | Combines `parseAmountToSmallestUnit` + `formatCurrencyAmount` in one call. |

**When to use:** Displaying monetary amounts with currency symbols. `formatCurrencyAmount()` is the workhorse function.

### C.3 DateFormatter

**File:** `formatter/DateFormatter.kt`
**Type:** Top-level extension functions

| Function | Signature | Purpose |
|---|---|---|
| `formatShortDate` | `LocalDateTime.formatShortDate(locale): String` | Formats as `"12 Jan"` (short month). For list items. |
| `formatMediumDate` | `LocalDateTime.formatMediumDate(locale): String` | Formats as `"January 2025"`. For section headers. |

**When to use:** Displaying dates in expense lists, group headers, etc.

### C.4 FormattingHelper

**File:** `formatter/FormattingHelper.kt`
**Type:** Injectable class (via Koin)

A convenience wrapper around all the above formatters, injected with `LocaleProvider` so mappers don't need to pass locale manually each time.

| Method | Purpose |
|---|---|
| `formatForDisplay(internalValue, maxDecimalPlaces, minDecimalPlaces)` | Locale-aware number formatting. |
| `formatRateForDisplay(internalValue)` | Locale-aware exchange rate formatting. |
| `formatCentsWithCurrency(cents, currencyCode)` | Cents → formatted currency string (e.g., `"16,67 €"`). |
| `formatCentsValue(cents, decimalDigits)` | Cents → plain locale number (e.g., `"16,67"`). For input fields. |
| `formatPercentageForDisplay(percentage)` | BigDecimal percentage → locale string (e.g., `"33,33"`). |
| `formatShortDate(date)` | `LocalDateTime` → `"12 Jan"` short date string. Used for cash tranche withdrawal date labels. |

**When to use:** Inject into feature mappers as the primary formatting API. Eliminates locale-passing boilerplate.

---

## D. Design-System Preview Utilities

**Module:** `:core:design-system`
**Source set:** `src/debug`
**Package:** `...core.designsystem.preview`

| Utility | File | Purpose |
|---|---|---|
| `@PreviewLocales` | `PreviewAnnotations.kt` | Shows preview in English and Spanish. |
| `@PreviewThemes` | `PreviewAnnotations.kt` | Shows preview in Light and Dark modes. |
| `@PreviewComplete` | `PreviewAnnotations.kt` | Shows all 4 combinations (EN/ES × Light/Dark). |
| `PreviewThemeWrapper` | `PreviewHelpers.kt` | Wraps content in `SplitTripTheme`. Required by all preview annotations. |
| `MappedPreview` | `MappedPreview.kt` | Generic composable for domain→mapper→UiModel previews. Accepts domain object, mapper factory, transform function, and content lambda. |
| `PreviewLocaleProvider` | `PreviewLocaleProvider.kt` | Fake `LocaleProvider` for preview context. |
| `PreviewResourceProvider` | `PreviewResourceProvider.kt` | Fake `ResourceProvider` for preview context. |
| `PreviewNavigationProviders` | `PreviewNavigationProviders.kt` | Sample `NavigationProvider` instances for previewing navigation-dependent components. |
| `PreviewExamples` | `PreviewExamples.kt` | Usage examples demonstrating the preview pipeline. |

**Existing component previews** (also in `src/debug`):

| Preview File | Components Previewed |
|---|---|
| `dialog/DestructiveConfirmationDialogPreviews.kt` | `DestructiveConfirmationDialog` |
| `input/SearchableChipSelectorPreviews.kt` | `SearchableChipSelector` |
| `input/StyledOutlinedTextFieldPreviews.kt` | `StyledOutlinedTextField` |
| `layout/AnimatedAmountPreviews.kt` | `AnimatedAmount` |
| `layout/EmptyStateViewPreviews.kt` | `EmptyStateView` |
| `layout/ShimmerLoadingPreviews.kt` | `ShimmerLoadingList`, `ShimmerItemCard` |
| `scaffold/ExpressiveFabPreviews.kt` | `ExpressiveFab`, `LargeExpressiveFab` |
| `scaffold/NavigationBarIconPreviews.kt` | `NavigationBarIcon` |
| `sheet/ActionBottomSheetPreviews.kt` | `ActionBottomSheet` |
| `sheet/CopyableTextSheetPreviews.kt` | `CopyableTextSheet` |

---

## E. Domain Services

**Module:** `:domain`
**Package:** `...domain.service.*`

> **Rule:** All decimal math in domain services uses `BigDecimal` with explicit `RoundingMode` and `scale` — never `Double` or `Float`. Domain services contain business rules only; formatting belongs in presentation-layer mappers.

### E.1 Calculation Services

#### `ExpenseCalculatorService`

**File:** `service/ExpenseCalculatorService.kt`
**Type:** Plain class (no interface)

General expense-related calculations: cents conversion, fair distribution, proportional amounts, and FIFO cash operations.

| Method | Purpose | When to Use |
|---|---|---|
| `centsToBigDecimal(cents, decimalPlaces)` | Converts cents (Long) to `BigDecimal`. | Converting stored amounts for calculation. |
| `centsToBigDecimalString(cents, decimalPlaces)` | Cents → plain decimal string. | Bridge between storage (Long) and display formatting. |
| `computeProportionalAmount(amount, targetAmount, totalAmount)` | Cross-multiplication for proportional scaling. | Deriving source-currency base cost from group-currency base cost. |
| `distributeAmount(totalAmount, numberOfUsers, decimalPlaces)` | Equal distribution with remainder handling. Sum always equals total exactly. | Splitting expense amounts among N users (conservation of currency guaranteed). |
| `hasInsufficientCash(amountToCover, availableWithdrawals)` | Checks if cash withdrawals are insufficient. | Pre-validation before FIFO cash calculation. |
| `calculateFifoCashAmount(amountToCover, availableWithdrawals, targetDecimalPlaces)` | FIFO algorithm: consumes cash from oldest withdrawals first, blending exchange rates. | Cash payment method with multiple ATM withdrawal rates. |

#### `AddOnCalculationService`

**File:** `service/AddOnCalculationService.kt`
**Type:** Plain class (depends on `AddOnResolverFactory`)

Add-on-specific calculations: resolution, totalling, effective amounts, and included-base-cost decomposition.

| Method | Purpose | When to Use |
|---|---|---|
| `resolveAddOnAmountCents(normalizedInput, valueType, decimalDigits, sourceAmountCents)` | Resolves user input to absolute add-on amount in cents. Handles both EXACT and PERCENTAGE value types. | When user enters an add-on amount/percentage. |
| `calculateTotalOnTopAddOns(addOns)` | Sums ON_TOP non-discount add-ons (group currency). | Calculating the extra cost beyond the base expense. |
| `calculateTotalAddOnExtras(addOns)` | Sums ALL non-discount add-ons (ON_TOP + INCLUDED). | "Extras" display in balance screens. |
| `calculateEffectiveGroupAmount(groupAmountCents, addOns)` | Computes total debt: base + ON_TOP (non-discount) + INCLUDED (non-discount) − ON_TOP DISCOUNT. INCLUDED discounts are informational and excluded. | Final debt amount for balance computation. |
| `calculateBaseCost(sourceAmountCents, includedAddOns)` | Extracts base cost from composite total by subtracting included add-ons. | Decomposing a total that already includes tip/tax. |

#### `ExchangeRateCalculationService`

**File:** `service/ExchangeRateCalculationService.kt`
**Type:** Plain class

Exchange-rate calculations: forward/inverse conversions, string convenience methods, and blended-rate computations.

| Method | Purpose | When to Use |
|---|---|---|
| `calculateGroupAmount(sourceAmount, rate, targetDecimalPlaces)` | `source × rate = group` (BigDecimal). | Core conversion from source to group currency. |
| `calculateImpliedRate(sourceAmount, groupAmount)` | `group / source = rate`. | Deriving rate from manual group amount entry. |
| `calculateGroupAmountFromStrings(sourceAmountString, exchangeRateString, …)` | String-based convenience for UI layer. | Real-time preview as user types amounts. |
| `calculateGroupAmountFromDisplayRate(sourceAmountString, displayRateString, …)` | Converts using user-friendly display rate (inverse format). | When rate is shown as "1 EUR = 37 THB". |
| `calculateBlendedRate(tranches, cashWithdrawals, sourceDecimalPlaces, targetDecimalPlaces)` | Weighted average rate across FIFO tranches. | Display rate for cash-paid expenses with multiple withdrawal rates. |

#### `PreviewCashExchangeRateUseCase`

**File:** `usecase/expense/PreviewCashExchangeRateUseCase.kt`
**Type:** Use case (not a domain service)

Simulates the FIFO algorithm against the current pool and returns a live preview for the Add Expense wizard. The result is **indicative** — actual tranches are determined at save time.

```kotlin
suspend operator fun invoke(
    groupId: String,
    sourceCurrency: String,
    sourceAmountCents: Long,
    payerType: PayerType = PayerType.GROUP,
    payerId: String? = null,                 // userId for USER scope, subunitId for SUBUNIT scope
    preferredWithdrawalScope: PayerType? = null,  // Explicit pool override (#1010)
    preferredWithdrawalOwnerId: String? = null
): CashRatePreviewResult
```

Returns `CashRatePreviewResult.Available(preview: CashRatePreview)` (with `tranches` populated when `sourceAmountCents > 0`), `InsufficientCash`, or `NoWithdrawals`.

#### `GetAvailableWithdrawalPoolsUseCase`

**File:** `usecase/expense/GetAvailableWithdrawalPoolsUseCase.kt`
**Type:** Use case

Returns all withdrawal pools that have available funds for a given currency and expense scope, using **single-scope queries** (no GROUP fallback). Used by `WithdrawalPoolSelectionDelegate` to determine whether the pool selector UI should be shown.

```kotlin
suspend operator fun invoke(
    groupId: String,
    currency: String,
    payerType: PayerType,
    payerId: String? = null
): List<WithdrawalPoolOption>
```

**When to use:** Only to populate the pool selector UI. For the actual FIFO query (with GROUP fallback), use `CashWithdrawalRepository.getAvailableWithdrawals()` directly.

#### `CashTranchePreview` Domain Model

**File:** `model/CashTranchePreview.kt`

Read model holding display-relevant fields from one withdrawal's simulated FIFO consumption. Populated by `PreviewCashExchangeRateUseCase` and surfaced via `CashRatePreview.tranches`.

```kotlin
data class CashTranchePreview(
    val withdrawalId: String,
    val withdrawalTitle: String?,       // null/blank → falls back to "ATM — <date>" in mapper
    val withdrawalDate: LocalDateTime?,
    val amountConsumedCents: Long,
    val remainingAfterCents: Long,
    val withdrawalRate: BigDecimal      // Always BigDecimal, serialized as String in Firestore
)
```

#### `WithdrawalPoolOption` Domain Model

**File:** `model/WithdrawalPoolOption.kt`

Lightweight descriptor representing a single available cash pool for a given expense scope.

```kotlin
data class WithdrawalPoolOption(
    val scope: PayerType,        // GROUP, USER, or SUBUNIT
    val ownerId: String? = null  // userId for USER scope, subunitId for SUBUNIT scope
)
```

#### `RemainderDistributionService`

**File:** `service/RemainderDistributionService.kt`
**Type:** Plain class

General-purpose proportional distribution with floor rounding and one-unit remainder redistribution. Guarantees `sum(result) == total` exactly.

| Method | Purpose | When to Use |
|---|---|---|
| `distributeByWeights(total, weights)` | Distributes total proportionally across BigDecimal weights. | Included add-on residual distribution, split rescaling. |
| `rescaleAmounts(originalTotal, newTotal, amounts, isExcluded)` | Rescales amounts from one total to another, respecting exclusions. | Base cost extraction rescaling when add-ons are included. |
| `rescalePercentages(percentages, targetPercentage)` | Rescales BigDecimal percentages to a new target, preserving proportions. | Percentage-based split rescaling after base cost extraction. |

### E.2 Split Services

#### `ExpenseSplitCalculatorFactory`

**File:** `service/split/ExpenseSplitCalculatorFactory.kt`
**Type:** Factory class (Strategy pattern)

Creates the correct `ExpenseSplitCalculator` strategy based on `SplitType`.

| Strategy | Calculator | Behavior |
|---|---|---|
| `SplitType.EQUAL` | `EqualSplitCalculator` | Equal shares with remainder distribution. |
| `SplitType.EXACT` | `ExactSplitCalculator` | User-specified exact amounts per participant. |
| `SplitType.PERCENT` | `PercentSplitCalculator` | Percentage-based split. |

**When to use:** When validating or computing expense splits at save time (authoritative calculation).

#### `ExpenseSplitCalculator` (Abstract Base)

**File:** `service/split/ExpenseSplitCalculator.kt`
**Type:** Abstract class (Template Method pattern)

Enforces validate → execute contract. Subclasses implement `validate()` and `executeCalculation()`.

```kotlin
fun calculateShares(totalAmountCents, participantIds, existingSplits): List<ExpenseSplit>
```

#### `SplitPreviewService`

**File:** `service/split/SplitPreviewService.kt`
**Type:** Plain class

Ephemeral UI-feedback calculations for **live preview** as the user types. Not authoritative — only for display.

| Method | Purpose | When to Use |
|---|---|---|
| `distributePercentagesEvenly(sourceAmountCents, participantIds)` | Even % distribution with remainder handling. | Initial percentage split preview. |
| `redistributeRemainingPercentage(editedPercentage, sourceAmountCents, otherParticipantIds, lockedPercentages)` | Recalculates others' percentages when one user edits theirs. | Real-time split preview during percent editing. |
| `parseAmountToCents(amountString)` | Parses locale-aware amount input to cents. | Parsing user-entered split amounts. |

#### `SubunitAwareSplitService`

**File:** `service/split/SubunitAwareSplitService.kt`
**Type:** Plain class (depends on `ExpenseSplitCalculatorFactory`)

Two-level expense splitting: first among entities (solo + subunits), then within each subunit. Output is always a flat `List<ExpenseSplit>`.

| Method | Purpose | When to Use |
|---|---|---|
| `calculateShares(totalAmountCents, individualParticipantIds, subunits, entitySplitType, entitySplits, subunitSplitOverrides)` | Full two-level split computation. | When the group has subunits (couples, families). |

#### `SubunitShareDistributionService`

**File:** `service/SubunitShareDistributionService.kt`
**Type:** Plain class

Subunit share percentage math — even distribution, manual redistribution, and input parsing.

| Method | Purpose | When to Use |
|---|---|---|
| `distributeEvenly(memberIds)` | Equal shares among members (as 0–1 BigDecimal). | Creating a new subunit. |
| `redistributeRemaining(editedShare, otherMemberIds, lockedShares)` | Redistributes remaining share when one member edits theirs. | Editing subunit member shares. |
| `parseShareTexts(selectedMemberIds, memberShareTexts)` | Parses user-entered percentage texts to domain shares. | Saving subunit with custom percentages. |

### E.3 Add-On Services

#### `AddOnResolverFactory`

**File:** `service/addon/AddOnResolverFactory.kt`
**Type:** Factory class (Strategy pattern)

Creates `AddOnAmountResolver` based on `AddOnValueType` (EXACT or PERCENTAGE).

#### `AddOnAmountResolver` Implementations

| Resolver | File | Behavior |
|---|---|---|
| `ExactAddOnResolver` | `service/addon/ExactAddOnResolver.kt` | Converts normalized input directly to cents. |
| `PercentageAddOnResolver` | `service/addon/PercentageAddOnResolver.kt` | Computes percentage of source amount. |

### E.4 Validation Services

> **Rule:** Validation logic belongs in Domain Services, NOT in UseCases or ViewModels.

#### `ExpenseValidationService`

**File:** `service/ExpenseValidationService.kt`

| Method | Purpose |
|---|---|
| `validateTitle(title)` | Title must not be blank. |
| `validateAmount(amountString)` | Must parse to valid positive cents. |
| `validateUserCount(count)` | Count must be > 0. |
| `validateSplits(splitType, splits, totalAmountCents, participantIds)` | Delegates to strategy calculator's validation. |
| `validateAddOn(addOn, sourceAmountCents)` | Add-on amount > 0, currency not blank, included < total. |
| `validateAddOns(addOns, sourceAmountCents)` | Validates all add-ons, returns first error. |

#### `SubunitValidationService`

**File:** `service/SubunitValidationService.kt`

| Method | Purpose |
|---|---|
| `validate(subunit, groupMemberIds, existingSubunits, excludeSubunitId)` | Validates name, member count, group membership, overlap, share integrity. Auto-normalizes empty shares. |

#### `ContributionValidationService`

**File:** `service/ContributionValidationService.kt`

| Method | Purpose |
|---|---|
| `validateAmount(amount)` | Amount must be positive. |
| `validate(contribution)` | Validates a full `Contribution` object. |
| `validateContributionScope(contributionScope, subunitId, userId, groupSubunits)` | Validates SUBUNIT scope (subunit exists, user is member). |

#### `CashWithdrawalValidationService`

**File:** `service/CashWithdrawalValidationService.kt`

| Method | Purpose |
|---|---|
| `validateAmountWithdrawn(amount)` | Amount must be positive. |
| `validateTitle(title)` | Optional; max 100 chars. |
| `validateNotes(notes)` | Optional; max 500 chars. |
| `validateDeductedBaseAmount(amount)` | Must be positive. |
| `validateCurrency(currency)` | Must not be blank. |
| `validateExchangeRate(rate)` | Must be > 0. |

#### `EmailValidationService`

**File:** `service/EmailValidationService.kt`

| Method | Purpose |
|---|---|
| `isValidEmail(email)` | Pure Kotlin regex email validation. No Android dependencies. |

### E.5 Membership & Auth Services

#### `GroupMembershipService`

**File:** `service/GroupMembershipService.kt`
**Type:** Plain class (depends on `GroupRepository`, `AuthenticationService`)

| Method | Purpose | When to Use |
|---|---|---|
| `requireMembership(groupId)` | Throws `NotGroupMemberException` if current user is not a group member. | Pre-validation before write operations on group data. |

#### `AuthenticationService` (Interface)

**File:** `service/AuthenticationService.kt`
**Type:** Interface (implemented in `:data:firebase`)

| Method | Purpose |
|---|---|
| `currentUserId()` | Returns current user ID or null. |
| `requireUserId()` | Returns current user ID or throws. |
| `authState` | `Flow<Boolean>` — emits authentication state changes. |
| `signIn(email, password)` | Email/password sign-in. |
| `signUp(email, password)` | Email/password sign-up. |
| `signOut()` | Signs out the current user. |
| `signInWithGoogle(idToken)` | Google OAuth sign-in. |

### E.6 Infrastructure Services

#### `CloudMetadataService` (Interface)

**File:** `service/CloudMetadataService.kt`
**Type:** Interface (implemented in `:data:firebase`)

| Method | Purpose |
|---|---|
| `getAppInstallationId()` | Returns Firebase Installation ID. |

#### `LocalDatabaseCleanerService` (Interface)

**File:** `service/LocalDatabaseCleanerService.kt`
**Type:** Interface (implemented in `:data:local`)

| Method | Purpose |
|---|---|
| `clearAll()` | Clears all local Room database tables. Used on sign-out. |

---

## F. Domain Converter

#### `CurrencyConverter` (Object)

**File:** `converter/CurrencyConverter.kt`
**Type:** Kotlin `object` (stateless utility)

| Method | Purpose | When to Use |
|---|---|---|
| `convert(amount, source, target, rates)` | Full currency conversion via base currency with `BigDecimal` precision. | Converting amounts between currencies using exchange rates. |
| `parseToCents(amountString)` | Parses raw amount string to cents (`Result<Long>`). Handles locale separators. | Validating and converting user-entered amounts. |
| `normalizeAmountString(input)` | Normalizes locale-specific decimal/thousand separators to US format (`"."` = decimal). | Pre-processing user input before `BigDecimal` parsing. |

---

## G. Data Layer Sync Delegates

**Module:** `:data`
**Package:** `...data.sync`
**Visibility:** `internal` — scoped to `:data` module only. Not injectable via Koin.

> **Rule:** New repositories MUST use these delegates instead of duplicating offline-first boilerplate. See [`wiki/offline-first-architecture.md`](offline-first-architecture.md) for the full architectural context and usage patterns.

### G.1 KeyedSubscriptionTracker

**File:** `sync/KeyedSubscriptionTracker.kt`
**Type:** Internal class

Manages a set of keyed cloud subscription `Job`s via a `ConcurrentHashMap`, ensuring only one active Firestore snapshot listener exists per key at any time.

| Method | Purpose | When to Use |
|---|---|---|
| `cancelAndRelaunch(key, scope, block)` | Cancels any existing subscription for the key and launches a new one. | In `onStart` of group-keyed repository Flows (Expense, Subunit, Contribution, CashWithdrawal). |

**When NOT to use:** `GroupRepositoryImpl` uses a single `Job?` for its non-keyed subscription — the 3-line pattern doesn't benefit from keyed tracking.

### G.2 CloudSyncDelegates

**File:** `sync/CloudSyncDelegates.kt`
**Type:** Top-level internal functions

| Function | Purpose | When to Use |
|---|---|---|
| `subscribeAndReconcile<T>()` | Collects cloud Flow, reconciles local via merge strategy, then confirms PENDING_SYNC items. | In the cloud subscription body for any repository's `get*Flow()` method. Replaces manual `subscribeToCloudChanges()` + `confirmPendingSyncXxx()`. |
| `confirmPendingSync()` | Iterates PENDING_SYNC entity IDs, verifies each on server (`Source.SERVER`), marks verified as SYNCED. | Called automatically by `subscribeAndReconcile`. Also available standalone for custom reconciliation hooks. |
| `syncCreateToCloud()` | Background `scope.launch`: cloud write → `updateSyncStatus(SYNCED)` or `updateSyncStatus(SYNC_FAILED)`. | After local save in `create` and `update` repository methods. |
| `syncDeletionToCloud()` | Background `scope.launch`: cloud delete with error logging. Always queues, even for PENDING_SYNC. | After local delete in `delete` repository methods. |

**Sync status transitions:**

```
syncCreateToCloud:   PENDING_SYNC ──success──→ SYNCED
                     PENDING_SYNC ──failure──→ SYNC_FAILED

syncDeletionToCloud: (no status tracking — delete is fire-and-forget with retry via snapshot listener)

confirmPendingSync:  PENDING_SYNC ──server verified──→ SYNCED
                     PENDING_SYNC ──server unreachable──→ stays PENDING_SYNC
```

**Reference implementations:**
- `SubunitRepositoryImpl` — cleanest example, uses all 4 delegates + `KeyedSubscriptionTracker`
- `CashWithdrawalRepositoryImpl` — demonstrates mixing delegates with entity-specific batch operations
- `GroupRepositoryImpl` — uses `subscribeAndReconcile` only; create/delete have unique patterns

---

## Quick Decision Guide

| I need to… | Use this |
|---|---|
| Display a loading list | `ShimmerLoadingList` |
| Show an empty state | `EmptyStateView` |
| Wrap content in a flat card | `FlatCard` (override `shape`/`color`/`borderColor` only for variations) |
| Create a currency input field | `AmountCurrencyCard` + `AmountCurrencyCardState` |
| Format cents as "25,50 €" | `FormattingHelper.formatCentsWithCurrency()` or `formatCurrencyAmount()` |
| Format a number for display | `FormattingHelper.formatForDisplay()` or `String.formatNumberForDisplay()` |
| Format a date | `LocalDateTime.formatShortDate()` / `.formatMediumDate()` or `FormattingHelper.formatShortDate()` |
| Format a date for a cash tranche label | `FormattingHelper.formatShortDate(date)` (injected into `AddExpenseOptionsUiMapper`) |
| Parse user input to cents | `parseAmountToSmallestUnit()` or `CurrencyConverter.parseToCents()` |
| Normalize "1.245,56" → "1245.56" | `CurrencyConverter.normalizeAmountString()` |
| Split an expense equally | `ExpenseSplitCalculatorFactory.create(SplitType.EQUAL)` |
| Preview split percentages live | `SplitPreviewService.distributePercentagesEvenly()` |
| Distribute a total by weights | `RemainderDistributionService.distributeByWeights()` |
| Calculate group amount from rate | `ExchangeRateCalculationService.calculateGroupAmount()` |
| Preview FIFO cash rate + tranches | `PreviewCashExchangeRateUseCase` (returns `CashRatePreview` with `tranches`) |
| Check available withdrawal pools (for pool selector) | `GetAvailableWithdrawalPoolsUseCase` (single-scope, no GROUP fallback) |
| Get FIFO-ordered withdrawal pool (for expense save) | `CashWithdrawalRepository.getAvailableWithdrawals(groupId, currency, payerType, payerId)` |
| Manage pool selection state in Add Expense | `WithdrawalPoolSelectionDelegate` (plain class, not a handler — see `:features:expenses`) |
| Resolve add-on amount | `AddOnCalculationService.resolveAddOnAmountCents()` |
| Validate expense title/amount | `ExpenseValidationService` |
| Validate email | `EmailValidationService.isValidEmail()` |
| Check group membership | `GroupMembershipService.requireMembership()` |
| Convert cents to BigDecimal | `ExpenseCalculatorService.centsToBigDecimal()` |
| Create a confirmation dialog | `DestructiveConfirmationDialog` |
| Show contextual actions | `ActionBottomSheet` + `SheetAction` |
| Build a multi-step form | `WizardStepLayout` + `WizardStepIndicator` + `WizardNavigationBar` |
| Manage keyed cloud subscriptions | `KeyedSubscriptionTracker` (`:data` internal) |
| Subscribe + reconcile + confirm sync | `subscribeAndReconcile()` (`:data` internal) |
| Sync a create/update to cloud | `syncCreateToCloud()` (`:data` internal) |
| Sync a deletion to cloud | `syncDeletionToCloud()` (`:data` internal) |

