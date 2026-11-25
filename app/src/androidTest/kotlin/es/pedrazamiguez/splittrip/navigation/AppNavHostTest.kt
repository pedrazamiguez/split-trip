package es.pedrazamiguez.splittrip.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.navigation.NavigationProvider
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.BRANDED_LOADING_SCREEN_TEST_TAG
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.security.APP_LOCK_GATE_TEST_TAG
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.update.APP_UPDATE_DOWNLOADING_BANNER_TEST_TAG
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.update.APP_UPDATE_DOWNLOADING_DETERMINATE_PROGRESS_TEST_TAG
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.update.APP_UPDATE_DOWNLOADING_INDETERMINATE_PROGRESS_TEST_TAG
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.update.APP_UPDATE_DOWNLOADING_PERCENT_TEST_TAG
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.update.APP_UPDATE_GATE_BUTTON_TEST_TAG
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.update.APP_UPDATE_GATE_TEST_TAG
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.update.APP_UPDATE_READY_ACTION_TEST_TAG
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.update.APP_UPDATE_READY_BANNER_TEST_TAG
import es.pedrazamiguez.splittrip.core.designsystem.presentation.viewmodel.SharedViewModel
import es.pedrazamiguez.splittrip.di.FakeRegisterDeviceTokenUseCase
import es.pedrazamiguez.splittrip.di.createAppNavHostTestModule
import es.pedrazamiguez.splittrip.domain.enums.BiometricCapability
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.auth.HandleSessionExpiredUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveSelectedGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupNameUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.CheckPendingReconciliationUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ObserveCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ObserveProAllowlistAccessUseCase
import es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel.AuthenticationViewModel
import es.pedrazamiguez.splittrip.features.main.presentation.viewmodel.MainViewModel
import es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.OnboardingViewModel
import es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.ReconciliationViewModel
import es.pedrazamiguez.splittrip.helpers.FakeNavigationProvider
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import es.pedrazamiguez.splittrip.navigation.AppNavHost
import es.pedrazamiguez.splittrip.update.AppUpdateCoordinator
import es.pedrazamiguez.splittrip.update.AppUpdateCoordinatorAction
import es.pedrazamiguez.splittrip.update.AppUpdateUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.compose.KoinApplication
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Instrumentation tests for [AppNavHost] navigation flow.
 *
 * Verifies that the start destination and screen transitions correctly reflect
 * the combination of authentication state, onboarding state, and deep links.
 * Each test runs in isolation with its own [ComposeNavigator] and minimal
 * mock dependencies injected via [KoinApplication].
 */
@RunWith(AndroidJUnit4::class)
class AppNavHostTest {

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule()

    @Before
    fun setUp() {
        stopKoin()
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /**
     * Creates a Koin module that provides mock ViewModels and NavigationProviders
     * needed by the sub-graphs rendered inside the NavHost.
     */
    private fun createFeatureTestModule() = module {
        // ── ViewModels required by feature screens ────────────────────
        viewModel {
            AuthenticationViewModel(
                signInWithEmailUseCase = mockk(relaxed = true),
                signInWithGoogleUseCase = mockk(relaxed = true),
                signInAnonymouslyUseCase = mockk(relaxed = true),
                authenticationCollisionEventHandler = mockk(relaxed = true)
            )
        }

        viewModel { OnboardingViewModel() }

        viewModel {
            ReconciliationViewModel(
                reconcileUnregisteredUserUseCase = mockk(relaxed = true),
                authenticationService = get()
            )
        }

        // Main screen needs MainViewModel
        viewModel {
            val observeCurrentUserProfile = mockk<ObserveCurrentUserProfileUseCase>().apply {
                every { this@apply.invoke() } returns flowOf(null)
            }
            val observeProAllowlistAccess = mockk<ObserveProAllowlistAccessUseCase>().apply {
                every { this@apply.invoke() } returns flowOf(false)
            }
            MainViewModel(
                registerDeviceTokenUseCase = FakeRegisterDeviceTokenUseCase(),
                getGroupByIdUseCase = mockk(relaxed = true),
                warmCurrencyCacheUseCase = mockk(relaxed = true),
                observeCurrentUserProfileUseCase = observeCurrentUserProfile,
                observeProAllowlistAccessUseCase = observeProAllowlistAccess,
                shouldShowAdsUseCase = mockk(relaxed = true),
                getBannerAdUnitIdUseCase = mockk(relaxed = true),
                isUserAnonymousUseCase = mockk(relaxed = true)
            )
        }

        // Main screen needs SharedViewModel (activity-scoped)
        viewModel {
            val getSelectedGroupIdUseCase = mockk<GetSelectedGroupIdUseCase>().apply {
                every { this@apply.invoke() } returns flowOf(null)
            }
            val getSelectedGroupNameUseCase = mockk<GetSelectedGroupNameUseCase>().apply {
                every { this@apply.invoke() } returns flowOf(null)
            }
            val getSelectedGroupCurrencyUseCase = mockk<GetSelectedGroupCurrencyUseCase>().apply {
                every { this@apply.invoke() } returns flowOf(null)
            }
            val observeSelectedGroup = mockk<ObserveSelectedGroupUseCase>().apply {
                every { this@apply.invoke() } returns flowOf(null)
            }
            val observeGroup = mockk<ObserveGroupUseCase>().apply {
                every { this@apply.invoke(any()) } returns flowOf(null)
            }
            SharedViewModel(
                getSelectedGroupIdUseCase = getSelectedGroupIdUseCase,
                getSelectedGroupNameUseCase = getSelectedGroupNameUseCase,
                getSelectedGroupCurrencyUseCase = getSelectedGroupCurrencyUseCase,
                setSelectedGroupUseCase = mockk(relaxed = true),
                observeSelectedGroupUseCase = observeSelectedGroup,
                observeGroupUseCase = observeGroup
            )
        }

        // ── Navigation/Screen providers ───────────────────────────────
        // MainScreen requires at least one NavigationProvider to avoid
        // NoSuchElementException on visibleProviders.first().
        factory {
            FakeNavigationProvider(
                route = Routes.GROUPS,
                order = 10,
                requiresSelectedGroup = false,
                label = "Groups"
            )
        } bind NavigationProvider::class

        factory {
            FakeNavigationProvider(
                route = Routes.PROFILE,
                order = 90,
                requiresSelectedGroup = false,
                label = "Profile"
            )
        } bind NavigationProvider::class
    }

    /**
     * Builds a [TestNavHostController] with a [ComposeNavigator].
     */
    private fun buildTestNavController(): TestNavHostController {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return TestNavHostController(context).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Loading state
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun showsLoadingIndicator_whenAuthAndOnboardingStatesAreUnknown() {
        // Both authState and onboarding never emit → startDestination stays null
        val authState = MutableSharedFlow<Boolean>() // never emits
        val onboarding = MutableSharedFlow<Boolean>() // never emits

        composeRule.setContent {
            KoinApplication(application = {
                modules(
                    createAppNavHostTestModule(
                        authStateFlow = authState,
                        onboardingFlow = onboarding
                    ),
                    createFeatureTestModule()
                )
            }) {
                SplitTripTheme {
                    AppNavHost()
                }
            }
        }

        // Branded loading screen is shown while undetermined
        composeRule.onNode(
            hasTestTag(BRANDED_LOADING_SCREEN_TEST_TAG)
        ).assertIsDisplayed()
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Not logged in → LOGIN
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun navigatesToLogin_whenUserIsNotLoggedIn() {
        val navController = buildTestNavController()

        composeRule.setContent {
            KoinApplication(application = {
                modules(
                    createAppNavHostTestModule(
                        authStateFlow = flowOf(false),
                        onboardingFlow = flowOf(true)
                    ),
                    createFeatureTestModule()
                )
            }) {
                SplitTripTheme {
                    AppNavHost(navController = navController)
                }
            }
        }

        composeRule.waitForIdle()

        assertEquals(
            Routes.LOGIN,
            navController.currentDestination?.route
        )
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Logged in, onboarding NOT complete → ONBOARDING
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun navigatesToOnboarding_whenLoggedInButOnboardingNotComplete() {
        val navController = buildTestNavController()

        composeRule.setContent {
            KoinApplication(application = {
                modules(
                    createAppNavHostTestModule(
                        authStateFlow = flowOf(true),
                        onboardingFlow = flowOf(false)
                    ),
                    createFeatureTestModule()
                )
            }) {
                SplitTripTheme {
                    AppNavHost(navController = navController)
                }
            }
        }

        composeRule.waitForIdle()

        assertEquals(
            Routes.ONBOARDING,
            navController.currentDestination?.route
        )
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Logged in + onboarding done → MAIN
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun navigatesToMain_whenLoggedInAndOnboardingComplete() {
        val navController = buildTestNavController()

        composeRule.setContent {
            KoinApplication(application = {
                modules(
                    createAppNavHostTestModule(
                        authStateFlow = flowOf(true),
                        onboardingFlow = flowOf(true)
                    ),
                    createFeatureTestModule()
                )
            }) {
                SplitTripTheme {
                    AppNavHost(navController = navController)
                }
            }
        }

        composeRule.waitForIdle()

        assertEquals(
            Routes.MAIN,
            navController.currentDestination?.route
        )
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Start destination latching — graph NOT recreated on state change
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun startDestinationIsLatched_doesNotChangeAfterInitialResolution() {
        val navController = buildTestNavController()

        // Start as not logged in → LOGIN
        val authState = MutableStateFlow(false)

        composeRule.setContent {
            KoinApplication(application = {
                modules(
                    createAppNavHostTestModule(
                        authStateFlow = authState,
                        onboardingFlow = flowOf(true)
                    ),
                    createFeatureTestModule()
                )
            }) {
                SplitTripTheme {
                    AppNavHost(navController = navController)
                }
            }
        }

        composeRule.waitForIdle()
        assertEquals(Routes.LOGIN, navController.currentDestination?.route)

        // Now simulate login success by changing auth state.
        // The start destination should NOT change (latched).
        // Navigation should only happen via imperative navigate() calls.
        authState.value = true
        composeRule.waitForIdle()

        // The graph's startDestination is still LOGIN (latched).
        // navController may or may not have navigated (depends on onLoginSuccess callback),
        // but the graph was NOT recreated. We verify the start destination of the graph itself.
        val graphStartRoute = navController.graph.startDestinationRoute
        assertEquals(Routes.LOGIN, graphStartRoute)
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Biometric App Lock Gate
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun showsAppLockGate_whenUserIsLoggedIn_onboardingComplete_andBiometricLockEnabled() {
        val navController = buildTestNavController()

        composeRule.setContent {
            KoinApplication(application = {
                modules(
                    createAppNavHostTestModule(
                        authStateFlow = flowOf(true),
                        onboardingFlow = flowOf(true),
                        biometricLockEnabledFlow = flowOf(true),
                        biometricCapability = BiometricCapability.AVAILABLE
                    ),
                    createFeatureTestModule()
                )
            }) {
                SplitTripTheme {
                    AppNavHost(navController = navController)
                }
            }
        }

        composeRule.waitForIdle()

        composeRule.onNode(
            hasTestTag(APP_LOCK_GATE_TEST_TAG)
        ).assertIsDisplayed()
    }

    @Test
    fun doesNotShowAppLockGate_whenBiometricLockIsDisabled() {
        val navController = buildTestNavController()

        composeRule.setContent {
            KoinApplication(application = {
                modules(
                    createAppNavHostTestModule(
                        authStateFlow = flowOf(true),
                        onboardingFlow = flowOf(true),
                        biometricLockEnabledFlow = flowOf(false),
                        biometricCapability = BiometricCapability.AVAILABLE
                    ),
                    createFeatureTestModule()
                )
            }) {
                SplitTripTheme {
                    AppNavHost(navController = navController)
                }
            }
        }

        composeRule.waitForIdle()

        composeRule.onNode(
            hasTestTag(APP_LOCK_GATE_TEST_TAG)
        ).assertDoesNotExist()

        assertEquals(
            Routes.MAIN,
            navController.currentDestination?.route
        )
    }

    @Test
    fun doesNotShowAppLockGate_whenBiometricCapabilityIsUnavailable() {
        val navController = buildTestNavController()

        composeRule.setContent {
            KoinApplication(application = {
                modules(
                    createAppNavHostTestModule(
                        authStateFlow = flowOf(true),
                        onboardingFlow = flowOf(true),
                        biometricLockEnabledFlow = flowOf(true),
                        biometricCapability = BiometricCapability.NO_HARDWARE
                    ),
                    createFeatureTestModule()
                )
            }) {
                SplitTripTheme {
                    AppNavHost(navController = navController)
                }
            }
        }

        composeRule.waitForIdle()

        composeRule.onNode(
            hasTestTag(APP_LOCK_GATE_TEST_TAG)
        ).assertDoesNotExist()

        assertEquals(
            Routes.MAIN,
            navController.currentDestination?.route
        )
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Session Expiration Guard
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun redirectsToLoginAndClearsBackStack_whenAuthStateTransitionsTrueToFalse() {
        val navController = buildTestNavController()
        val authState = MutableStateFlow(true)
        val handleSessionExpiredUseCase = mockk<HandleSessionExpiredUseCase>(relaxed = true).apply {
            coEvery { this@apply.invoke() } returns Result.success(Unit)
        }
        val expectedMessage = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(DesignSystemR.string.session_expired_message)

        composeRule.setContent {
            KoinApplication(application = {
                modules(
                    createAppNavHostTestModule(
                        authStateFlow = authState,
                        onboardingFlow = flowOf(true),
                        handleSessionExpiredUseCase = handleSessionExpiredUseCase
                    ),
                    createFeatureTestModule()
                )
            }) {
                SplitTripTheme {
                    AppNavHost(navController = navController)
                }
            }
        }

        composeRule.waitForIdle()
        assertEquals(Routes.MAIN, navController.currentDestination?.route)

        authState.value = false
        composeRule.waitForIdle()

        assertEquals(Routes.LOGIN, navController.currentDestination?.route)
        assertNull(navController.previousBackStackEntry)
        coVerify(exactly = 1) { handleSessionExpiredUseCase() }
        composeRule.onNodeWithText(expectedMessage).assertIsDisplayed()
    }

    @Test
    fun doesNotTriggerSessionExpired_whenExplicitSignOut() {
        val navController = buildTestNavController()
        val authState = MutableStateFlow(true)
        val handleSessionExpiredUseCase = mockk<HandleSessionExpiredUseCase>(relaxed = true).apply {
            coEvery { this@apply.invoke() } returns Result.success(Unit)
        }
        val expectedMessage = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(DesignSystemR.string.session_expired_message)

        composeRule.setContent {
            KoinApplication(application = {
                modules(
                    createAppNavHostTestModule(
                        authStateFlow = authState,
                        onboardingFlow = flowOf(true),
                        isExplicitSignOut = true,
                        handleSessionExpiredUseCase = handleSessionExpiredUseCase
                    ),
                    createFeatureTestModule()
                )
            }) {
                SplitTripTheme {
                    AppNavHost(navController = navController)
                }
            }
        }

        composeRule.waitForIdle()
        assertEquals(Routes.MAIN, navController.currentDestination?.route)

        authState.value = false
        composeRule.waitForIdle()

        coVerify(exactly = 0) { handleSessionExpiredUseCase() }
        composeRule.onNodeWithText(expectedMessage).assertDoesNotExist()
    }

    @Test
    fun redirectsToLogin_whenSessionExpiresBehindBiometricAppLock() {
        val navController = buildTestNavController()
        val authState = MutableStateFlow(true)
        val handleSessionExpiredUseCase = mockk<HandleSessionExpiredUseCase>(relaxed = true).apply {
            coEvery { this@apply.invoke() } returns Result.success(Unit)
        }

        composeRule.setContent {
            KoinApplication(application = {
                modules(
                    createAppNavHostTestModule(
                        authStateFlow = authState,
                        onboardingFlow = flowOf(true),
                        biometricLockEnabledFlow = flowOf(true),
                        biometricCapability = BiometricCapability.AVAILABLE,
                        handleSessionExpiredUseCase = handleSessionExpiredUseCase
                    ),
                    createFeatureTestModule()
                )
            }) {
                SplitTripTheme {
                    AppNavHost(navController = navController)
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNode(hasTestTag(APP_LOCK_GATE_TEST_TAG)).assertIsDisplayed()

        authState.value = false
        composeRule.waitForIdle()

        composeRule.onNode(hasTestTag(APP_LOCK_GATE_TEST_TAG)).assertDoesNotExist()
        coVerify(exactly = 1) { handleSessionExpiredUseCase() }
        assertEquals(Routes.LOGIN, navController.currentDestination?.route)
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Startup Reconciliation Gate
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun navigatesToMain_whenAnonymousUserWithNullEmailAndNotReconciled() {
        val navController = buildTestNavController()
        val userPreferenceRepository = mockk<UserPreferenceRepository>(relaxed = true)

        composeRule.setContent {
            KoinApplication(application = {
                modules(
                    createAppNavHostTestModule(
                        authStateFlow = flowOf(true),
                        onboardingFlow = flowOf(true),
                        isReconciledFlow = flowOf(false),
                        currentUserEmail = null,
                        userPreferenceRepository = userPreferenceRepository
                    ),
                    createFeatureTestModule()
                )
            }) {
                SplitTripTheme {
                    AppNavHost(navController = navController)
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 2_000L) {
            navController.currentDestination?.route == Routes.MAIN
        }

        assertEquals(Routes.MAIN, navController.currentDestination?.route)
        coVerify(exactly = 1) { userPreferenceRepository.setIsReconciled(true) }
    }

    @Test
    fun navigatesToOnboarding_whenAnonymousUserWithNullEmailAndOnboardingIncomplete() {
        val navController = buildTestNavController()
        val userPreferenceRepository = mockk<UserPreferenceRepository>(relaxed = true)

        composeRule.setContent {
            KoinApplication(application = {
                modules(
                    createAppNavHostTestModule(
                        authStateFlow = flowOf(true),
                        onboardingFlow = flowOf(false),
                        isReconciledFlow = flowOf(false),
                        currentUserEmail = null,
                        userPreferenceRepository = userPreferenceRepository
                    ),
                    createFeatureTestModule()
                )
            }) {
                SplitTripTheme {
                    AppNavHost(navController = navController)
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 2_000L) {
            navController.currentDestination?.route == Routes.ONBOARDING
        }

        assertEquals(Routes.ONBOARDING, navController.currentDestination?.route)
        coVerify(exactly = 1) { userPreferenceRepository.setIsReconciled(true) }
    }

    @Test
    fun failsOpenToMain_whenPendingReconciliationCheckFails() {
        val navController = buildTestNavController()
        val userPreferenceRepository = mockk<UserPreferenceRepository>(relaxed = true)
        val checkPendingReconciliationUseCase = mockk<CheckPendingReconciliationUseCase>().apply {
            coEvery { this@apply.invoke("user@example.com") } returns Result.failure(RuntimeException("Network error"))
        }

        composeRule.setContent {
            KoinApplication(application = {
                modules(
                    createAppNavHostTestModule(
                        authStateFlow = flowOf(true),
                        onboardingFlow = flowOf(true),
                        isReconciledFlow = flowOf(false),
                        currentUserEmail = "user@example.com",
                        checkPendingReconciliationUseCase = checkPendingReconciliationUseCase,
                        userPreferenceRepository = userPreferenceRepository
                    ),
                    createFeatureTestModule()
                )
            }) {
                SplitTripTheme {
                    AppNavHost(navController = navController)
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 2_000L) {
            navController.currentDestination?.route == Routes.MAIN
        }

        assertEquals(Routes.MAIN, navController.currentDestination?.route)
        coVerify(exactly = 0) { userPreferenceRepository.setIsReconciled(true) }
    }

    @Test
    fun failsOpenToMain_whenPendingReconciliationCheckTimesOut() {
        val navController = buildTestNavController()
        val userPreferenceRepository = mockk<UserPreferenceRepository>(relaxed = true)
        val checkPendingReconciliationUseCase = mockk<CheckPendingReconciliationUseCase>().apply {
            coEvery { this@apply.invoke("user@example.com") } coAnswers { awaitCancellation() }
        }

        composeRule.setContent {
            KoinApplication(application = {
                modules(
                    createAppNavHostTestModule(
                        authStateFlow = flowOf(true),
                        onboardingFlow = flowOf(true),
                        isReconciledFlow = flowOf(false),
                        currentUserEmail = "user@example.com",
                        checkPendingReconciliationUseCase = checkPendingReconciliationUseCase,
                        userPreferenceRepository = userPreferenceRepository
                    ),
                    createFeatureTestModule()
                )
            }) {
                SplitTripTheme {
                    AppNavHost(
                        navController = navController,
                        reconciliationTimeoutMs = 100L
                    )
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 2_000L) {
            navController.currentDestination?.route == Routes.MAIN
        }

        assertEquals(Routes.MAIN, navController.currentDestination?.route)
        coVerify(exactly = 0) { userPreferenceRepository.setIsReconciled(true) }
    }

    @Test
    fun navigatesToReconciliation_whenPendingReconciliationExists() {
        val navController = buildTestNavController()
        val checkPendingReconciliationUseCase = mockk<CheckPendingReconciliationUseCase>().apply {
            coEvery { this@apply.invoke("user@example.com") } returns Result.success(true)
        }

        composeRule.setContent {
            KoinApplication(application = {
                modules(
                    createAppNavHostTestModule(
                        authStateFlow = flowOf(true),
                        onboardingFlow = flowOf(false),
                        isReconciledFlow = flowOf(false),
                        currentUserEmail = "user@example.com",
                        checkPendingReconciliationUseCase = checkPendingReconciliationUseCase
                    ),
                    createFeatureTestModule()
                )
            }) {
                SplitTripTheme {
                    AppNavHost(navController = navController)
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 2_000L) {
            navController.currentDestination?.route == Routes.RECONCILIATION
        }

        assertEquals(Routes.RECONCILIATION, navController.currentDestination?.route)
    }

    // ═════════════════════════════════════════════════════════════════════
    //  In-App Updates (Gate & Ready Banner)
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun showsAppUpdateGate_whenCoordinatorEmitsForcedUpdateBlocked() {
        val navController = buildTestNavController()
        val coordinator = mockk<AppUpdateCoordinator>(relaxed = true).apply {
            every { uiState } returns MutableStateFlow(
                AppUpdateUiState.ForcedUpdateBlocked(availableVersionCode = 1_001_000L)
            )
            every { actions } returns MutableSharedFlow<AppUpdateCoordinatorAction>()
            every { registerInstallListener() } returns {}
        }

        composeRule.setContent {
            KoinApplication(application = {
                modules(
                    createAppNavHostTestModule(
                        authStateFlow = flowOf(true),
                        onboardingFlow = flowOf(true),
                        appUpdateCoordinator = coordinator
                    ),
                    createFeatureTestModule()
                )
            }) {
                SplitTripTheme {
                    AppNavHost(navController = navController)
                }
            }
        }

        composeRule.waitForIdle()

        composeRule.onNode(hasTestTag(APP_UPDATE_GATE_TEST_TAG)).assertIsDisplayed()
        composeRule.onNode(hasTestTag(APP_UPDATE_GATE_BUTTON_TEST_TAG)).performClick()
        composeRule.waitForIdle()

        coVerify(exactly = 1) { coordinator.retryForcedUpdate(any()) }
    }

    @Test
    fun showsAppUpdateReadyBanner_whenCoordinatorEmitsFlexibleUpdateReadyToInstall() {
        val navController = buildTestNavController()
        val coordinator = mockk<AppUpdateCoordinator>(relaxed = true).apply {
            every { uiState } returns MutableStateFlow(AppUpdateUiState.FlexibleUpdateReadyToInstall)
            every { actions } returns MutableSharedFlow<AppUpdateCoordinatorAction>()
            every { registerInstallListener() } returns {}
        }

        composeRule.setContent {
            KoinApplication(application = {
                modules(
                    createAppNavHostTestModule(
                        authStateFlow = flowOf(true),
                        onboardingFlow = flowOf(true),
                        appUpdateCoordinator = coordinator
                    ),
                    createFeatureTestModule()
                )
            }) {
                SplitTripTheme {
                    AppNavHost(navController = navController)
                }
            }
        }

        composeRule.waitForIdle()

        composeRule.onNode(hasTestTag(APP_UPDATE_READY_BANNER_TEST_TAG)).assertIsDisplayed()
        composeRule.onNode(hasTestTag(APP_UPDATE_READY_ACTION_TEST_TAG)).performClick()
        composeRule.waitForIdle()

        coVerify(exactly = 1) { coordinator.completeFlexibleUpdate() }
    }

    @Test
    fun showsAppUpdateDownloadingBanner_whenCoordinatorEmitsFlexibleUpdateDownloading() {
        val navController = buildTestNavController()
        val uiStateFlow = MutableStateFlow<AppUpdateUiState>(
            AppUpdateUiState.FlexibleUpdateDownloading(
                bytesDownloaded = 0L,
                totalBytesToDownload = 0L
            )
        )
        val coordinator = mockk<AppUpdateCoordinator>(relaxed = true).apply {
            every { uiState } returns uiStateFlow
            every { actions } returns MutableSharedFlow<AppUpdateCoordinatorAction>()
            every { registerInstallListener() } returns {}
        }

        composeRule.setContent {
            KoinApplication(application = {
                modules(
                    createAppNavHostTestModule(
                        authStateFlow = flowOf(true),
                        onboardingFlow = flowOf(true),
                        appUpdateCoordinator = coordinator
                    ),
                    createFeatureTestModule()
                )
            }) {
                SplitTripTheme {
                    AppNavHost(navController = navController)
                }
            }
        }

        composeRule.waitForIdle()

        composeRule.onNode(hasTestTag(APP_UPDATE_DOWNLOADING_BANNER_TEST_TAG)).assertIsDisplayed()
        composeRule.onNode(hasTestTag(APP_UPDATE_DOWNLOADING_INDETERMINATE_PROGRESS_TEST_TAG))
            .assertIsDisplayed()
        composeRule.onNode(hasTestTag(APP_UPDATE_DOWNLOADING_PERCENT_TEST_TAG)).assertDoesNotExist()

        uiStateFlow.value = AppUpdateUiState.FlexibleUpdateDownloading(
            bytesDownloaded = 45L,
            totalBytesToDownload = 100L
        )
        composeRule.waitForIdle()

        composeRule.onNode(hasTestTag(APP_UPDATE_DOWNLOADING_DETERMINATE_PROGRESS_TEST_TAG))
            .assertIsDisplayed()
        composeRule.onNode(hasTestTag(APP_UPDATE_DOWNLOADING_PERCENT_TEST_TAG))
            .assertIsDisplayed()

        uiStateFlow.value = AppUpdateUiState.FlexibleUpdateReadyToInstall
        composeRule.waitForIdle()

        composeRule.onNode(hasTestTag(APP_UPDATE_READY_BANNER_TEST_TAG)).assertIsDisplayed()
    }
}
