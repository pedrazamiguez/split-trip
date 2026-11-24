package es.pedrazamiguez.splittrip.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowInsets as WindowInsetsOverride
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.graphics.Insets as AndroidXInsets
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.navigation.NavigationProvider
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.FeatureScaffold
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.viewmodel.SharedViewModel
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.usecase.auth.IsUserAnonymousUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveSelectedGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.notification.RegisterDeviceTokenUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupNameUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetSelectedGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ObserveCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ObserveProAllowlistAccessUseCase
import es.pedrazamiguez.splittrip.features.main.presentation.screen.MainScreen
import es.pedrazamiguez.splittrip.features.main.presentation.viewmodel.MainViewModel
import es.pedrazamiguez.splittrip.helpers.FakeNavigationProvider
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.compose.KoinApplication
import org.koin.core.context.stopKoin
import org.koin.dsl.module

private class FakeRegisterDeviceTokenUseCase : RegisterDeviceTokenUseCase {
    override suspend fun invoke(): Result<Unit> = Result.success(Unit)
}

/**
 * Instrumentation tests for [MainScreen] tab visibility and interaction.
 *
 * These tests use fake [NavigationProvider] implementations that render
 * trivial content, avoiding real feature ViewModels and Koin dependencies.
 */
@RunWith(AndroidJUnit4::class)
class MainScreenTest {

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

    private val testModule = module {
        single<TelemetryTracker> { mockk(relaxed = true) }
    }

    // ── Provider instances ────────────────────────────────────────────

    private val groupsProvider = FakeNavigationProvider(
        route = "groups",
        order = 10,
        requiresSelectedGroup = false,
        label = "Groups"
    )

    private val balancesProvider = FakeNavigationProvider(
        route = "balances",
        order = 20,
        requiresSelectedGroup = true,
        label = "Balances"
    )

    private val expensesProvider = FakeNavigationProvider(
        route = "expenses",
        order = 30,
        requiresSelectedGroup = true,
        label = "Expenses"
    )

    private val profileProvider = FakeNavigationProvider(
        route = "profile",
        order = 90,
        requiresSelectedGroup = false,
        label = "Profile"
    )

    private val allProviders = listOf(
        groupsProvider,
        balancesProvider,
        expensesProvider,
        profileProvider
    )

    // ── ViewModel helpers ────────────────────────────────────────────

    private fun createMainViewModel(): MainViewModel {
        val observeCurrentUserProfile = mockk<ObserveCurrentUserProfileUseCase>().apply {
            every { this@apply.invoke() } returns flowOf(null)
        }
        val observeProAllowlistAccess = mockk<ObserveProAllowlistAccessUseCase>().apply {
            every { this@apply.invoke() } returns flowOf(false)
        }
        val isUserAnonymous = mockk<IsUserAnonymousUseCase>().apply {
            every { this@apply.invoke() } returns flowOf(false)
        }
        val getGroupById = mockk<GetGroupByIdUseCase>().apply {
            coEvery { this@apply.invoke(any()) } returns Group(
                id = "group-123",
                name = "Test Group",
                currency = "EUR"
            )
        }
        return MainViewModel(
            registerDeviceTokenUseCase = FakeRegisterDeviceTokenUseCase(),
            getGroupByIdUseCase = getGroupById,
            warmCurrencyCacheUseCase = mockk(relaxed = true),
            observeCurrentUserProfileUseCase = observeCurrentUserProfile,
            observeProAllowlistAccessUseCase = observeProAllowlistAccess,
            shouldShowAdsUseCase = mockk(relaxed = true),
            getBannerAdUnitIdUseCase = mockk(relaxed = true),
            isUserAnonymousUseCase = isUserAnonymous
        )
    }

    private fun createSharedViewModel(selectedGroupId: String? = null): SharedViewModel {
        val group = if (selectedGroupId != null) {
            Group(id = selectedGroupId, name = "Test Group", currency = "EUR")
        } else {
            null
        }
        val getGroupId = mockk<GetSelectedGroupIdUseCase>().apply {
            every { this@apply.invoke() } returns flowOf(selectedGroupId)
        }
        val getGroupName = mockk<GetSelectedGroupNameUseCase>().apply {
            every { this@apply.invoke() } returns flowOf(group?.name)
        }
        val getGroupCurrency = mockk<GetSelectedGroupCurrencyUseCase>().apply {
            every { this@apply.invoke() } returns flowOf(group?.currency)
        }
        val observeSelectedGroup = mockk<ObserveSelectedGroupUseCase>().apply {
            every { this@apply.invoke() } returns flowOf(group)
        }
        val observeGroup = mockk<ObserveGroupUseCase>().apply {
            every { this@apply.invoke(any()) } returns flowOf(group)
        }
        val setGroup = mockk<SetSelectedGroupUseCase>(relaxed = true)

        return SharedViewModel(
            getSelectedGroupIdUseCase = getGroupId,
            getSelectedGroupNameUseCase = getGroupName,
            getSelectedGroupCurrencyUseCase = getGroupCurrency,
            setSelectedGroupUseCase = setGroup,
            observeSelectedGroupUseCase = observeSelectedGroup,
            observeGroupUseCase = observeGroup
        )
    }

    private fun setContent(
        navigationProviders: List<NavigationProvider> = allProviders,
        screenUiProviders: List<ScreenUiProvider> = emptyList(),
        deepLinkGroupId: String? = null,
        deepLinkTargetTab: String? = null,
        deepLinkInTabDestination: String? = null,
        mainViewModel: MainViewModel = createMainViewModel(),
        sharedViewModel: SharedViewModel = createSharedViewModel()
    ) {
        composeRule.setContent {
            KoinApplication(application = { modules(testModule) }) {
                SplitTripTheme {
                    MainScreen(
                        navigationProviders = navigationProviders,
                        screenUiProviders = screenUiProviders,
                        deepLinkGroupId = deepLinkGroupId,
                        deepLinkTargetTab = deepLinkTargetTab,
                        deepLinkInTabDestination = deepLinkInTabDestination,
                        mainViewModel = mainViewModel,
                        sharedViewModel = sharedViewModel
                    )
                }
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Tab visibility: No group selected
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun showsOnlyNonGroupDependentTabs_whenNoGroupIsSelected() {
        setContent(
            sharedViewModel = createSharedViewModel(selectedGroupId = null)
        )

        composeRule.waitForIdle()

        // Non-group tabs should be visible
        composeRule.onNodeWithText("Groups").assertIsDisplayed()
        composeRule.onNodeWithText("Profile").assertIsDisplayed()

        // Group-dependent tabs should NOT be displayed
        composeRule.onNodeWithText("Balances").assertDoesNotExist()
        composeRule.onNodeWithText("Expenses").assertDoesNotExist()
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Tab visibility: Group selected
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun showsAllTabs_whenGroupIsSelected() {
        setContent(
            sharedViewModel = createSharedViewModel(selectedGroupId = "group-123")
        )

        composeRule.waitForIdle()

        // All tabs should be visible
        composeRule.onNodeWithText("Groups").assertIsDisplayed()
        composeRule.onNodeWithText("Balances").assertIsDisplayed()
        composeRule.onNodeWithText("Expenses").assertIsDisplayed()
        composeRule.onNodeWithText("Profile").assertIsDisplayed()
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Tab selection
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun tappingTab_changesSelectedContent() {
        setContent(
            sharedViewModel = createSharedViewModel(selectedGroupId = "group-123")
        )

        composeRule.waitForIdle()

        // Initially the first visible tab is selected (Groups)
        composeRule.onNodeWithText("Content: Groups").assertIsDisplayed()

        // Tap Profile tab
        composeRule.onNodeWithText("Profile").performClick()
        composeRule.waitForIdle()

        // Profile content should now be visible
        composeRule.onNodeWithText("Content: Profile").assertIsDisplayed()
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Deep Link & In-Tab Navigation
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun deepLinkWithInTabDestination_navigatesToInTabDestination() {
        val balancesProviderWithSub = FakeNavigationProvider(
            route = "balances",
            order = 20,
            requiresSelectedGroup = true,
            label = "Balances",
            subRoutes = listOf(Routes.YOUR_POSITION)
        )
        val providers = listOf(
            groupsProvider,
            balancesProviderWithSub,
            expensesProvider,
            profileProvider
        )

        setContent(
            navigationProviders = providers,
            deepLinkGroupId = "group-123",
            deepLinkTargetTab = "balances",
            deepLinkInTabDestination = Routes.YOUR_POSITION,
            sharedViewModel = createSharedViewModel(selectedGroupId = "group-123")
        )

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Content: Balances - ${Routes.YOUR_POSITION}").assertIsDisplayed()
    }

    @Test
    fun deepLinkWithExpenseDetail_navigatesToExpenseDetail() {
        val expenseDetailRoute = Routes.expenseDetailRoute("expense-999")
        val expensesProviderWithSub = FakeNavigationProvider(
            route = "expenses",
            order = 30,
            requiresSelectedGroup = true,
            label = "Expenses",
            subRoutes = listOf(expenseDetailRoute)
        )
        val providers = listOf(
            groupsProvider,
            balancesProvider,
            expensesProviderWithSub,
            profileProvider
        )

        setContent(
            navigationProviders = providers,
            deepLinkGroupId = "group-123",
            deepLinkTargetTab = "expenses",
            deepLinkInTabDestination = expenseDetailRoute,
            sharedViewModel = createSharedViewModel(selectedGroupId = "group-123")
        )

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Content: Expenses - $expenseDetailRoute").assertIsDisplayed()
    }

    @Test
    fun deepLinkWithContributionDetail_navigatesToContributionDetail() {
        val contributionDetailRoute = Routes.contributionDetailRoute("group-123", "contrib-456")
        val balancesProviderWithSub = FakeNavigationProvider(
            route = "balances",
            order = 20,
            requiresSelectedGroup = true,
            label = "Balances",
            subRoutes = listOf(contributionDetailRoute)
        )
        val providers = listOf(
            groupsProvider,
            balancesProviderWithSub,
            expensesProvider,
            profileProvider
        )

        setContent(
            navigationProviders = providers,
            deepLinkGroupId = "group-123",
            deepLinkTargetTab = "balances",
            deepLinkInTabDestination = contributionDetailRoute,
            sharedViewModel = createSharedViewModel(selectedGroupId = "group-123")
        )

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Content: Balances - $contributionDetailRoute").assertIsDisplayed()
    }

    @Test
    fun hidesBottomNavigationBar_whenCurrentRouteProviderHasShowBottomBarFalse() {
        val expensesProviderWithSub = FakeNavigationProvider(
            route = "expenses",
            order = 30,
            requiresSelectedGroup = true,
            label = "Expenses",
            subRoutes = listOf(Routes.ADD_EXPENSE)
        )
        val wizardScreenUiProvider = object : ScreenUiProvider {
            override val route: String = Routes.ADD_EXPENSE
            override val showBottomBar: Boolean = false
        }
        val providers = listOf(
            groupsProvider,
            balancesProvider,
            expensesProviderWithSub,
            profileProvider
        )

        setContent(
            navigationProviders = providers,
            screenUiProviders = listOf(wizardScreenUiProvider),
            deepLinkGroupId = "group-123",
            deepLinkTargetTab = "expenses",
            deepLinkInTabDestination = Routes.ADD_EXPENSE,
            sharedViewModel = createSharedViewModel(selectedGroupId = "group-123")
        )

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Content: Expenses - ${Routes.ADD_EXPENSE}").assertIsDisplayed()
        composeRule.onNodeWithText("Groups").assertDoesNotExist()
        composeRule.onNodeWithText("Profile").assertDoesNotExist()
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Edge-to-Edge & Landscape Cutout / NavBar Insets
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun mainScreen_landscapeCutoutAndNavBarInsets_offsetsTabContentWithoutDoublePadding() {
        val directTag = "direct_tab_content"
        val scaffoldTag = "scaffold_tab_content"
        val directProvider = object : NavigationProvider {
            override val route: String = "groups"
            override val order: Int = 10
            override val requiresSelectedGroup: Boolean = false

            @Composable
            override fun Icon(isSelected: Boolean, tint: Color) = Unit

            @Composable
            override fun getLabel(): String = "Groups"

            override fun buildGraph(builder: NavGraphBuilder) {
                builder.composable(route) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(directTag)
                    )
                }
            }
        }
        val scaffoldProvider = object : NavigationProvider {
            override val route: String = "profile"
            override val order: Int = 90
            override val requiresSelectedGroup: Boolean = false

            @Composable
            override fun Icon(isSelected: Boolean, tint: Color) = Unit

            @Composable
            override fun getLabel(): String = "Profile"

            override fun buildGraph(builder: NavGraphBuilder) {
                builder.composable(route) {
                    FeatureScaffold(currentRoute = route) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag(scaffoldTag)
                        )
                    }
                }
            }
        }

        val leftCutoutPx = 60
        val rightNavBarPx = 90
        val simulatedInsets = WindowInsetsCompat.Builder()
            .setInsets(
                WindowInsetsCompat.Type.displayCutout(),
                AndroidXInsets.of(leftCutoutPx, 0, 0, 0)
            )
            .setInsets(
                WindowInsetsCompat.Type.navigationBars(),
                AndroidXInsets.of(0, 0, rightNavBarPx, 0)
            )
            .build()

        var densityPx = 1f
        val rootTag = "main_screen_root"

        composeRule.setContent {
            densityPx = LocalDensity.current.density
            KoinApplication(application = { modules(testModule) }) {
                SplitTripTheme {
                    DeviceConfigurationOverride(
                        DeviceConfigurationOverride.WindowInsetsOverride(simulatedInsets)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().testTag(rootTag)) {
                            MainScreen(
                                navigationProviders = listOf(directProvider, scaffoldProvider),
                                screenUiProviders = emptyList(),
                                mainViewModel = createMainViewModel(),
                                sharedViewModel = createSharedViewModel()
                            )
                        }
                    }
                }
            }
        }

        composeRule.waitForIdle()

        val rootBounds = composeRule.onNodeWithTag(rootTag).getUnclippedBoundsInRoot()
        val directBounds = composeRule.onNodeWithTag(directTag).getUnclippedBoundsInRoot()
        val expectedLeftDp = leftCutoutPx / densityPx
        val expectedRightDp = rightNavBarPx / densityPx

        assertTrue(
            "Direct tab left (${directBounds.left.value}) should be >= $expectedLeftDp",
            directBounds.left.value >= expectedLeftDp - 0.5f
        )
        assertTrue(
            "Direct tab right (${directBounds.right.value}) should be <= ${rootBounds.right.value - expectedRightDp}",
            directBounds.right.value <= rootBounds.right.value - expectedRightDp + 0.5f
        )

        composeRule.onNodeWithText("Profile").performClick()
        composeRule.waitForIdle()

        val scaffoldBounds = composeRule.onNodeWithTag(scaffoldTag).getUnclippedBoundsInRoot()
        assertTrue(
            "FeatureScaffold left (${scaffoldBounds.left.value}) should not double-pad ($expectedLeftDp)",
            scaffoldBounds.left.value >= expectedLeftDp - 0.5f &&
                scaffoldBounds.left.value < expectedLeftDp * 2f - 1f
        )
        assertTrue(
            "FeatureScaffold right (${scaffoldBounds.right.value}) should not double-pad ($expectedRightDp)",
            scaffoldBounds.right.value <= rootBounds.right.value - expectedRightDp + 0.5f &&
                scaffoldBounds.right.value > rootBounds.right.value - expectedRightDp * 2f + 1f
        )
    }
}
