To maintain clean code and enable Jetpack Compose `@Preview`, we separate the "Logic" from the "UI".

## The Component: `Screen`

* **Responsibility:** Pure UI rendering.
* **Dependencies:** None. It relies on simple data classes, primitives, or State objects.
* **State:** Stateless (receives state as parameters).
* **Events:** Exposes lambdas (e.g., `onCurrencySelected: (String) -> Unit`).
* **Benefit:** Can be previewed easily in Android Studio with dummy data.

```kotlin
@Composable
fun DefaultCurrencyScreen(
    availableCurrencies: List<Currency>, // Pure data
    selectedCurrencyCode: String?,       // Pure data
    onCurrencySelected: (String) -> Unit // Event
) {
    // UI Implementation...
}

```

## The Wrapper: `Feature`

* **Responsibility:** Wiring the logic.
* **Dependencies:** `ViewModel`, `NavController`, `ScreenUiProvider` (via `FeatureScaffold`).
* **State:** Collects `StateFlow` from the ViewModel.
* **Events:** Handles navigation, side effects, or calls ViewModel functions.
* **Benefit:** Isolates dependencies from the rendering logic.

```kotlin
@Composable
fun DefaultCurrencyFeature(
    viewModel: DefaultCurrencyViewModel = koinViewModel()
) {
    // 1. Collect State
    val selectedCurrency by viewModel.selectedCurrencyCode.collectAsState()
    
    // 2. Setup Dependencies
    val navController = LocalRootNavController.current
    val scope = rememberCoroutineScope()

    // 3. Wrap with Scaffold (injects TopBar based on Route)
    FeatureScaffold(currentRoute = Routes.SETTINGS_DEFAULT_CURRENCY) {
        
        // 4. Render the Screen
        DefaultCurrencyScreen(
            availableCurrencies = viewModel.availableCurrencies,
            selectedCurrencyCode = selectedCurrency,
            onCurrencySelected = { newCurrencyCode ->
                viewModel.onCurrencySelected(newCurrencyCode)
                // Handle navigation side-effect
                scope.launch {
                    delay(UiConstants.NAV_FEEDBACK_DELAY)
                    navController.popBackStack()
                }
            }
        )
    }
}

```

## Rule of Thumb

* Never pass a `ViewModel` into a `Screen`.
* Never pass a `NavController` into a `Screen`.
* Pass them into the `Feature`, which orchestrates the `Screen`.
