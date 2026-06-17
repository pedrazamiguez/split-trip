The `MainScreen` is the most complex UI component in the app. Unlike standard features, it acts as a container and orchestrator for the top-level navigation.

## Responsibilities

1. **Bottom Navigation Management:** It holds the `BottomNavigationBar` and manages switching between tabs (e.g., Groups, Expenses, Balances) using a persistent `BottomNavigationController`.
2. **Multi-Stack State Preservation:** It maintains a separate `NavHostController` for each tab. It uses `MainViewModel` to save and restore the `Bundle` state of each controller when switching tabs, ensuring the back stack is preserved (e.g., if you drill down into an expense details screen, switch tabs, and come back, the details screen is still there).
3. **Dynamic UI Construction:** It observes the *current* inner route of the active tab to dynamically decide which `ScreenUiProvider` (TopBar/MainAction) to hoist to the parent Scaffold.
4. **Visual Effects & Transitions:** It initializes the `SharedTransitionLayout` to enable shared element transitions between screens and manages the `HazeState` for glassmorphism effects on the bottom bar.

## Why not use `FeatureScaffold`?

We use `FeatureScaffold` for "leaf" screens (simple screens with one job). `MainScreen` cannot use it because:

* **Persistent Lifecycle:** It is not destroyed when changing tabs; it acts as the host.
* **Complex Logic:** It needs to calculate logic based on "Which tab is active?" combined with "Which screen is active inside the tab?" to render the correct TopBar.
* **Multiple NavHosts:** It manages a map of multiple `NavHostController` instances (one per tab) to keep stacks independent, whereas `FeatureScaffold` assumes a single linear flow.

## Architecture

`MainScreen` acts as the "Host" that swaps content dynamically while keeping the Scaffold (TopBar/BottomBar/MainAction) persistent, providing a smooth user experience.

* **Window Insets:** It consumes `WindowInsets` manually to allow content (like lists) to draw behind the transparent Bottom Navigation Bar, creating an edge-to-edge experience.
* **Composition Locals:** It provides `LocalTabNavController` and `LocalSharedTransitionScope` to its children, enabling them to trigger global navigation actions or shared animations.
