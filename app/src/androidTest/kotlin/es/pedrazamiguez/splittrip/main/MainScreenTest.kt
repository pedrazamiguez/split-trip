package es.pedrazamiguez.splittrip.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.viewmodel.SharedViewModel
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveSelectedGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupNameUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetSelectedGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ObserveCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.features.main.presentation.screen.MainScreen
import es.pedrazamiguez.splittrip.features.main.presentation.viewmodel.MainViewModel
import es.pedrazamiguez.splittrip.helpers.FakeNavigationProvider
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
        return MainViewModel(
            registerDeviceTokenUseCase = mockk(relaxed = true),
            getGroupByIdUseCase = mockk(relaxed = true),
            warmCurrencyCacheUseCase = mockk(relaxed = true),
            observeCurrentUserProfileUseCase = observeCurrentUserProfile
        )
    }

    private fun createSharedViewModel(selectedGroupId: String? = null): SharedViewModel {
        val getGroupId = mockk<GetSelectedGroupIdUseCase>().apply {
            every { this@apply.invoke() } returns flowOf(selectedGroupId)
        }
        val getGroupName = mockk<GetSelectedGroupNameUseCase>().apply {
            every { this@apply.invoke() } returns flowOf(
                if (selectedGroupId != null) "Test Group" else null
            )
        }
        val getGroupCurrency = mockk<GetSelectedGroupCurrencyUseCase>().apply {
            every { this@apply.invoke() } returns flowOf(
                if (selectedGroupId != null) "EUR" else null
            )
        }
        val observeSelectedGroup = mockk<ObserveSelectedGroupUseCase>().apply {
            every { this@apply.invoke() } returns flowOf(null)
        }
        val observeGroup = mockk<ObserveGroupUseCase>().apply {
            every { this@apply.invoke(any()) } returns flowOf(null)
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

    // ═════════════════════════════════════════════════════════════════════
    //  Tab visibility: No group selected
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun showsOnlyNonGroupDependentTabs_whenNoGroupIsSelected() {
        composeRule.setContent {
            SplitTripTheme {
                MainScreen(
                    navigationProviders = allProviders,
                    screenUiProviders = emptyList(),
                    mainViewModel = createMainViewModel(),
                    sharedViewModel = createSharedViewModel(selectedGroupId = null)
                )
            }
        }

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
        composeRule.setContent {
            SplitTripTheme {
                MainScreen(
                    navigationProviders = allProviders,
                    screenUiProviders = emptyList(),
                    mainViewModel = createMainViewModel(),
                    sharedViewModel = createSharedViewModel(selectedGroupId = "group-123")
                )
            }
        }

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
        composeRule.setContent {
            SplitTripTheme {
                MainScreen(
                    navigationProviders = allProviders,
                    screenUiProviders = emptyList(),
                    mainViewModel = createMainViewModel(),
                    sharedViewModel = createSharedViewModel(selectedGroupId = "group-123")
                )
            }
        }

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

        composeRule.setContent {
            SplitTripTheme {
                MainScreen(
                    navigationProviders = providers,
                    screenUiProviders = emptyList(),
                    deepLinkGroupId = "group-123",
                    deepLinkTargetTab = "balances",
                    deepLinkInTabDestination = Routes.YOUR_POSITION,
                    mainViewModel = createMainViewModel(),
                    sharedViewModel = createSharedViewModel(selectedGroupId = "group-123")
                )
            }
        }

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

        composeRule.setContent {
            SplitTripTheme {
                MainScreen(
                    navigationProviders = providers,
                    screenUiProviders = emptyList(),
                    deepLinkGroupId = "group-123",
                    deepLinkTargetTab = "expenses",
                    deepLinkInTabDestination = expenseDetailRoute,
                    mainViewModel = createMainViewModel(),
                    sharedViewModel = createSharedViewModel(selectedGroupId = "group-123")
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Content: Expenses - $expenseDetailRoute").assertIsDisplayed()
    }
}
