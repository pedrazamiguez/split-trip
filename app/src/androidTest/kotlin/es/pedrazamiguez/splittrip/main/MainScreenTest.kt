package es.pedrazamiguez.splittrip.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.navigation.NavigationProvider
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.viewmodel.SharedViewModel
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveSelectedGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.notification.RegisterDeviceTokenUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupNameUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetSelectedGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ObserveCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.features.main.presentation.screen.MainScreen
import es.pedrazamiguez.splittrip.features.main.presentation.viewmodel.MainViewModel
import es.pedrazamiguez.splittrip.helpers.FakeNavigationProvider
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.After
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
            observeCurrentUserProfileUseCase = observeCurrentUserProfile
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
}
