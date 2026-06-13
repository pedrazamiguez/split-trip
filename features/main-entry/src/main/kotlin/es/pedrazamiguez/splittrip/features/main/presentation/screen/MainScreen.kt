package es.pedrazamiguez.splittrip.features.main.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.NavigationProvider
import es.pedrazamiguez.splittrip.core.designsystem.navigation.NavigationUtils
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.ProvideTopAppBarState
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.rememberTopAppBarState
import es.pedrazamiguez.splittrip.core.designsystem.presentation.viewmodel.SharedViewModel
import es.pedrazamiguez.splittrip.core.designsystem.transition.LocalSharedTransitionScope
import es.pedrazamiguez.splittrip.core.designsystem.transition.NavTransitionDefaults
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.features.main.presentation.component.BottomNavigationBar
import es.pedrazamiguez.splittrip.features.main.presentation.viewmodel.MainViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin

@OptIn(ExperimentalSharedTransitionApi::class)
@Suppress("LongMethod") // Orchestration composable: coordinates nav state, deep links and lifecycle effects
@Composable
fun MainScreen(
    navigationProviders: List<NavigationProvider>,
    screenUiProviders: List<ScreenUiProvider>,
    deepLinkGroupId: String? = null,
    deepLinkTargetTab: String? = null,
    mainViewModel: MainViewModel = koinViewModel<MainViewModel>(),
    sharedViewModel: SharedViewModel = koinViewModel(
        viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner
    )
) {
    val koin = getKoin()
    val telemetryTracker = remember(koin) { koin.get<TelemetryTracker>() }
    val hazeState = remember { HazeState() }

    // Compute visibleProviders internally from SharedViewModel's selectedGroupId.
    // This keeps the reactive state INSIDE MainScreen (a @Composable context),
    // preventing it from destabilizing the NavHost builder closure in AppNavHost.
    val selectedGroupId by sharedViewModel.selectedGroupId.collectAsStateWithLifecycle()
    val visibleProviders = remember(navigationProviders, selectedGroupId) {
        NavigationUtils.filterVisibleProviders(navigationProviders, selectedGroupId)
    }

    // Only clear invisible bundles when the visible providers change
    LaunchedEffect(visibleProviders) {
        val visibleRoutes = visibleProviders.map { it.route }.toSet()
        mainViewModel.clearInvisibleBundles(visibleRoutes)
    }

    // Build a stable map of NavHostControllers for ALL providers (maintains navigation stability)
    val navControllers = remember(navigationProviders) {
        mutableMapOf<NavigationProvider, NavHostController>()
    }

    for (provider in navigationProviders) {
        navControllers[provider] = rememberNavController()
    }

    // Use the properly ordered visible providers directly (preserves tab order)
    // No need to re-filter since visibleProviders is already correctly ordered from AppNavHost

    // Use rememberSaveable to persist across recompositions (e.g., after popping back from settings)
    var selectedRoute by rememberSaveable {
        mutableStateOf(visibleProviders.first().route)
    }

    // ── Deep link handling ─────────────────────────────────────────────
    // When a deep link is received, resolve the group name and currency from Room
    // (offline-first), auto-select the group, and switch to the target tab.
    // Keyed on both groupId AND targetTab so that a new deep link for the same group
    // but a different tab (e.g., via onNewIntent) still triggers the effect.
    LaunchedEffect(deepLinkGroupId, deepLinkTargetTab) {
        if (deepLinkGroupId != null) {
            val groupName = mainViewModel.resolveGroupName(deepLinkGroupId)
            val groupCurrency = mainViewModel.resolveGroupCurrency(deepLinkGroupId)
            sharedViewModel.selectGroup(deepLinkGroupId, groupName, groupCurrency)

            if (deepLinkTargetTab != null) {
                selectedRoute = deepLinkTargetTab
            }
        }
    }

    val selectedProvider = navigationProviders.first { it.route == selectedRoute }
    val selectedNavController = navControllers.getValue(selectedProvider)

    // Extract current route/provider for cleaner topBar/FAB usage
    val currentScreenRoute by selectedNavController.currentBackStackEntryAsState()
    val currentRoute = currentScreenRoute?.destination?.route ?: selectedRoute
    val currentUiProvider = remember(currentRoute) {
        screenUiProviders.firstOrNull { it.route == currentRoute }
    }

    LaunchedEffect(currentRoute) {
        telemetryTracker.trackScreenView(currentRoute, null)
    }

    // ── Tab-switch detection ─────────────────────────────────────────────
    // Distinguish tab switches (instant) from within-tab navigation (animated).
    // SideEffect updates previousSelectedRoute AFTER composition, so during the
    // composition where selectedRoute changes, isTabSwitch is correctly true.
    var previousSelectedRoute by remember { mutableStateOf(selectedRoute) }
    val isTabSwitch = selectedRoute != previousSelectedRoute
    SideEffect { previousSelectedRoute = selectedRoute }

    BackHandler {
        // Intentionally left blank to disable back navigation on main screen
    }

    // Save bundles on dispose (e.g., when navigating away to settings)
    for (provider in navigationProviders) {
        val navController = navControllers.getValue(provider)
        DisposableEffect(navController) {
            onDispose {
                mainViewModel.setBundle(
                    provider.route,
                    navController.saveState()
                )
            }
        }
    }

    // Wrap Scaffold in CompositionLocalProvider to provide LocalTabNavController for topBar/FAB
    // Also provide TopAppBarState for scroll-aware top bars
    val topAppBarState = rememberTopAppBarState()

    CompositionLocalProvider(
        LocalTabNavController provides selectedNavController
    ) {
        ProvideTopAppBarState(state = topAppBarState) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    AnimatedTopBar(
                        currentRoute = currentRoute,
                        screenUiProviders = screenUiProviders,
                        isTabSwitch = isTabSwitch
                    )
                },
                floatingActionButton = { currentUiProvider?.fab?.invoke() },
                bottomBar = {
                    BottomNavigationBar(
                        selectedRoute = selectedRoute,
                        onTabSelected = { route -> selectedRoute = route },
                        items = visibleProviders,
                        hazeState = hazeState
                    )
                },
                // Remove default content window insets since we're handling padding manually
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { innerPadding ->
                // Calculate bottom padding for content (FABs, list content padding)
                val bottomPadding = innerPadding.calculateBottomPadding()

                CompositionLocalProvider(LocalBottomPadding provides bottomPadding) {
                    Box(
                        modifier = Modifier
                            .padding(top = innerPadding.calculateTopPadding())
                            .fillMaxSize()
                            .hazeSource(state = hazeState)
                    ) {
                        MainTabsContent(
                            navigationProviders = navigationProviders,
                            navControllers = navControllers,
                            mainViewModel = mainViewModel,
                            selectedRoute = selectedRoute
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animated top bar wrapper that crossfades between top bar states during
 * within-tab navigation, while snapping instantly on tab switches.
 *
 * When the target screen has no top bar, renders a transparent [Spacer] with
 * [statusBarsPadding] so the Scaffold always has a measured top bar slot.
 * This eliminates the conditional padding jump in the content area.
 */
@Composable
private fun AnimatedTopBar(
    currentRoute: String,
    screenUiProviders: List<ScreenUiProvider>,
    isTabSwitch: Boolean
) {
    AnimatedContent(
        targetState = currentRoute,
        transitionSpec = {
            if (isTabSwitch) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                NavTransitionDefaults.topBarEnterTransition togetherWith
                    NavTransitionDefaults.topBarExitTransition using
                    NavTransitionDefaults.topBarSizeTransform
            }
        },
        label = "TopBarTransition"
    ) { route ->
        val provider = screenUiProviders.firstOrNull { it.route == route }
        if (provider?.topBar != null) {
            provider.topBar!!.invoke()
        } else {
            Spacer(modifier = Modifier.statusBarsPadding())
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun MainTabsContent(
    navigationProviders: List<NavigationProvider>,
    navControllers: Map<NavigationProvider, NavHostController>,
    mainViewModel: MainViewModel,
    selectedRoute: String
) {
    for (provider in navigationProviders) {
        val navController = navControllers.getValue(provider)
        val isSelected = selectedRoute == provider.route

        DisposableEffect(isSelected) {
            if (isSelected) {
                val savedBundle = mainViewModel.getBundle(provider.route)
                if (savedBundle != null) {
                    navController.restoreState(savedBundle)
                }
            }
            onDispose {
                if (isSelected) {
                    mainViewModel.setBundle(provider.route, navController.saveState())
                }
            }
        }

        if (isSelected) {
            CompositionLocalProvider(LocalTabNavController provides navController) {
                SharedTransitionLayout {
                    CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                        NavHost(
                            navController = navController,
                            startDestination = provider.route,
                            modifier = Modifier.fillMaxSize(),
                            enterTransition = {
                                NavTransitionDefaults.contentEnterTransition
                            },
                            exitTransition = {
                                NavTransitionDefaults.contentExitTransition
                            },
                            popEnterTransition = {
                                NavTransitionDefaults.contentPopEnterTransition
                            },
                            popExitTransition = {
                                NavTransitionDefaults.contentPopExitTransition
                            }
                        ) {
                            provider.buildGraph(this)
                        }
                    }
                }
            }
        }
    }
}
