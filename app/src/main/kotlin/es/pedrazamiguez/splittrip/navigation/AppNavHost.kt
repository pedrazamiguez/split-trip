package es.pedrazamiguez.splittrip.navigation

import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import es.pedrazamiguez.splittrip.R
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.NavigationProvider
import es.pedrazamiguez.splittrip.core.designsystem.navigation.NavigationUtils
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.BrandedLoadingScreen
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TopPillNotification
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.rememberTopPillController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.core.designsystem.transition.NavTransitionDefaults
import es.pedrazamiguez.splittrip.core.logging.LogTag
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.currency.WarmCurrencyCacheUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.IsOnboardingCompleteUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetOnboardingCompleteUseCase
import es.pedrazamiguez.splittrip.features.authentication.navigation.loginGraph
import es.pedrazamiguez.splittrip.features.main.navigation.DeepLinkHolder
import es.pedrazamiguez.splittrip.features.main.navigation.mainGraph
import es.pedrazamiguez.splittrip.features.onboarding.navigation.onboardingGraph
import es.pedrazamiguez.splittrip.features.profile.navigation.profileGraph
import es.pedrazamiguez.splittrip.features.settings.navigation.settingsGraph
import kotlinx.coroutines.launch
import org.koin.compose.getKoin
import timber.log.Timber

@Suppress("LongMethod", "CognitiveComplexMethod") // Navigation host DSL with auth/onboarding branching
@Composable
fun AppNavHost(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()) {
    val koin = getKoin()
    val telemetryTracker = remember(koin) { koin.get<TelemetryTracker>() }
    val navigationProviders = remember(koin) { koin.getAll<NavigationProvider>() }
    val screenUiProviders = remember(koin) { koin.getAll<ScreenUiProvider>() }
    val isOnboardingCompleteUseCase = remember(koin) { koin.get<IsOnboardingCompleteUseCase>() }
    val setOnboardingCompleteUseCase = remember(koin) { koin.get<SetOnboardingCompleteUseCase>() }
    val authenticationService = remember(koin) { koin.get<AuthenticationService>() }
    val warmCurrencyCacheUseCase = remember(koin) { koin.get<WarmCurrencyCacheUseCase>() }
    val deepLinkHolder = remember(koin) { koin.get<DeepLinkHolder>() }
    val scope = rememberCoroutineScope()

    val routeToUiProvider = remember(screenUiProviders) {
        screenUiProviders.associateBy { it.route }
    }

    val isUserLoggedIn by authenticationService.authState.collectAsStateWithLifecycle(initialValue = null)
    val onboardingCompleted by isOnboardingCompleteUseCase().collectAsStateWithLifecycle(
        initialValue = null
    )

    LaunchedEffect(isUserLoggedIn) {
        if (isUserLoggedIn == true) {
            telemetryTracker.setUserId(authenticationService.currentUserId())
        } else if (isUserLoggedIn == false) {
            telemetryTracker.setUserId(null)
        }
    }

    // Determine the start destination reactively
    val startDestination = NavigationUtils.resolveStartDestination(
        isUserLoggedIn = isUserLoggedIn,
        onboardingCompleted = onboardingCompleted
    )

    // Latch the first resolved startDestination so the NavHost graph is never
    // recreated when auth/onboarding state changes AFTER initial graph creation.
    // All subsequent transitions (sign-in, sign-out) are handled imperatively
    // via navController.navigate() in onLoginSuccess / onOnboardingComplete / sign-out.
    // Using `remember` (not rememberSaveable) ensures a fresh value on Activity
    // recreation after process death, while surviving normal recompositions.
    val stableStartDestination = remember { mutableStateOf<String?>(null) }
    if (stableStartDestination.value == null && startDestination != null) {
        stableStartDestination.value = startDestination
    }

    // Wrap changing values used inside the NavHost builder in rememberUpdatedState
    // so the builder lambda captures stable State references (same instance across
    // recompositions) instead of raw changing values. This prevents the Compose
    // compiler from recreating the lambda, which in turn prevents NavHost's
    // remember(startDestination, builder) from invalidating and recreating the graph.
    val currentOnboardingCompleted = rememberUpdatedState(onboardingCompleted)

    DisposableEffect(navController) {
        val listener = androidx.navigation.NavController.OnDestinationChangedListener { _, destination, arguments ->
            Timber.tag(LogTag.NAVIGATION).i(
                "Navigated to: %s | Arg keys: %s",
                destination.route,
                arguments?.keySet() ?: emptySet<String>()
            )
            destination.route?.let { route ->
                telemetryTracker.trackScreenView(route, null)
            }
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    val pillController = rememberTopPillController()

    CompositionLocalProvider(
        LocalRootNavController provides navController,
        LocalTopPillController provides pillController
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Crossfade(
                targetState = stableStartDestination.value != null,
                animationSpec = tween(
                    durationMillis = UiConstants.SPLASH_CROSSFADE_DURATION_MS
                ),
                label = "splash-crossfade"
            ) { destinationResolved ->
                if (!destinationResolved) {
                    BrandedLoadingScreen(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = stringResource(R.string.app_name)
                    )
                } else {
                    // When the user is already authenticated and onboarding is complete,
                    // startDestination = Routes.MAIN. NavHost natively processes the Activity
                    // intent's deep link on first composition, extracting arguments into the
                    // backStackEntry. The DeepLinkHolder may hold a stale copy saved in
                    // MainActivity.onCreate() — consume it to prevent accidental replay.
                    LaunchedEffect(stableStartDestination.value) {
                        if (stableStartDestination.value == Routes.MAIN) {
                            deepLinkHolder.consumePendingDeepLink()
                            // Cold start with existing auth — warm cache in background.
                            // No-op if cache is already populated from a previous session.
                            warmCurrencyCacheUseCase()
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = stableStartDestination.value!!,
                        modifier = modifier,
                        enterTransition = {
                            getEnterTransition(initialState.destination.route, targetState.destination.route)
                        },
                        exitTransition = {
                            getExitTransition(initialState.destination.route, targetState.destination.route)
                        },
                        popEnterTransition = {
                            getPopEnterTransition(initialState.destination.route, targetState.destination.route)
                        },
                        popExitTransition = {
                            getPopExitTransition(initialState.destination.route, targetState.destination.route)
                        }
                    ) {
                        loginGraph(
                            onLoginSuccess = {
                                val destination =
                                    NavigationUtils.resolvePostLoginDestination(
                                        currentOnboardingCompleted.value
                                    )
                                navController.navigate(destination) {
                                    popUpTo(Routes.LOGIN) { inclusive = true }
                                }
                                // Replay pending deep link after auth gate (cold start scenario)
                                if (destination == Routes.MAIN) {
                                    replayPendingDeepLink(deepLinkHolder, navController)
                                }
                                // Warm currency cache while user navigates through
                                // onboarding or the main screen — fire-and-forget.
                                scope.launch { warmCurrencyCacheUseCase() }
                            }
                        )

                        onboardingGraph(
                            onOnboardingComplete = {
                                scope.launch {
                                    try {
                                        setOnboardingCompleteUseCase()
                                    } catch (t: Throwable) {
                                        Timber.e(
                                            t,
                                            "Error setting onboarding complete"
                                        )
                                    }
                                    navController.navigate(Routes.MAIN) {
                                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                                    }
                                    // Replay pending deep link after onboarding gate
                                    replayPendingDeepLink(deepLinkHolder, navController)
                                }
                            }
                        )

                        mainGraph(
                            navigationProviders = navigationProviders,
                            screenUiProviders = routeToUiProvider.values.toList()
                        )

                        settingsGraph()

                        profileGraph()
                    }
                }
            }
            TopPillNotification(controller = pillController)
        }
    }
}

/**
 * Replays a pending deep link that was saved during cold start before the auth gate.
 *
 * Consumes the [DeepLinkHolder.pendingDeepLink] and dispatches it to the
 * [NavHostController] via [NavHostController.handleDeepLink]. The pending URI
 * is cleared after consumption to prevent replay loops.
 */
private fun replayPendingDeepLink(
    deepLinkHolder: DeepLinkHolder,
    navController: NavHostController
) {
    deepLinkHolder.consumePendingDeepLink()?.let { uri ->
        Timber.d("Replaying pending deep link (scheme: %s)", uri.scheme)
        val deepLinkIntent = Intent(Intent.ACTION_VIEW, uri)
        navController.handleDeepLink(deepLinkIntent)
    }
}

private fun isLoginOrOnboardingRoute(route: String?): Boolean {
    return route == Routes.LOGIN ||
        route == Routes.REGISTER ||
        route == Routes.FORGOT_PASSWORD ||
        route == Routes.ONBOARDING
}

private fun getEnterTransition(initialRoute: String?, targetRoute: String?): EnterTransition {
    return if (isLoginOrOnboardingRoute(initialRoute) || isLoginOrOnboardingRoute(targetRoute)) {
        EnterTransition.None
    } else if (targetRoute == Routes.EDIT_PROFILE) {
        NavTransitionDefaults.modalEnterTransition
    } else {
        NavTransitionDefaults.contentEnterTransition
    }
}

private fun getExitTransition(initialRoute: String?, targetRoute: String?): ExitTransition {
    return if (isLoginOrOnboardingRoute(initialRoute) || isLoginOrOnboardingRoute(targetRoute)) {
        ExitTransition.None
    } else if (targetRoute == Routes.EDIT_PROFILE) {
        NavTransitionDefaults.modalExitTransition
    } else {
        NavTransitionDefaults.contentExitTransition
    }
}

private fun getPopEnterTransition(initialRoute: String?, targetRoute: String?): EnterTransition {
    return if (isLoginOrOnboardingRoute(initialRoute) || isLoginOrOnboardingRoute(targetRoute)) {
        EnterTransition.None
    } else if (initialRoute == Routes.EDIT_PROFILE) {
        NavTransitionDefaults.modalPopEnterTransition
    } else {
        NavTransitionDefaults.contentPopEnterTransition
    }
}

private fun getPopExitTransition(initialRoute: String?, targetRoute: String?): ExitTransition {
    return if (isLoginOrOnboardingRoute(initialRoute) || isLoginOrOnboardingRoute(targetRoute)) {
        ExitTransition.None
    } else if (initialRoute == Routes.EDIT_PROFILE) {
        NavTransitionDefaults.modalPopExitTransition
    } else {
        NavTransitionDefaults.contentPopExitTransition
    }
}
