package es.pedrazamiguez.splittrip.journey

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.expense.R as ExpenseR
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseDetailUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitDetailUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.AddExpenseScreen
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.ExpenseDetailScreen
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseStep
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.ExpenseDetailUiState
import es.pedrazamiguez.splittrip.features.group.R as GroupR
import es.pedrazamiguez.splittrip.features.group.presentation.screen.CreateEditGroupScreen
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupStep
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupUiState
import es.pedrazamiguez.splittrip.features.settlement.R as SettlementR
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.SettlementRowStatusStyle
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.SettlementRowUiModel
import es.pedrazamiguez.splittrip.features.settlement.presentation.screen.GroupSettlementOverviewScreen
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.event.GroupSettlementOverviewUiEvent
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.state.GroupSettlementOverviewUiState
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end macro UI journey tests verifying core multi-user collaboration flows.
 */
@RunWith(AndroidJUnit4::class)
class CollaborationJourneyTest {

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun givenGroupCreationFlow_whenSettingUpBasicInfoAndMembers_thenWizardStepsAndMembersAreDisplayed() {
        val membersStepLabel = context.getString(GroupR.string.group_wizard_step_members)

        var uiState by mutableStateOf(
            CreateEditGroupUiState(
                currentStep = CreateEditGroupStep.INFO,
                groupName = "Tokyo Trip",
                groupDescription = "Spring vacation with friends"
            )
        )

        composeRule.setContent {
            SplitTripTheme {
                CreateEditGroupScreen(
                    uiState = uiState,
                    onScannerClick = {},
                    onEvent = {}
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Tokyo Trip").assertIsDisplayed()
        composeRule.onNodeWithText("Spring vacation with friends").assertIsDisplayed()

        val alice = User(userId = "user-1", displayName = "Alice", email = "alice@example.com", isPending = false)
        val bob = User(userId = "user-2", email = "bob@example.com", isPending = true)

        uiState = uiState.copy(
            currentStep = CreateEditGroupStep.MEMBERS,
            selectedMembers = persistentListOf(alice, bob)
        )

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Alice").assertIsDisplayed()
        composeRule.onNodeWithText("bob@example.com").assertIsDisplayed()
        composeRule.onNodeWithText(membersStepLabel, substring = true).assertIsDisplayed()
    }

    @Test
    fun givenExpenseFlow_whenAddingAndViewingExpense_thenWizardAndBreakdownAreRendered() {
        val splitBreakdownLabel = context.getString(ExpenseR.string.expense_detail_section_split)

        var isDetailView by mutableStateOf(false)
        val addExpenseState = AddExpenseUiState(
            isConfigLoaded = true,
            currentStep = AddExpenseStep.TITLE,
            expenseTitle = "Team Dinner"
        )
        val detailState = ExpenseDetailUiState(
            isLoading = false,
            expense = ExpenseDetailUiModel(
                id = "exp-1",
                title = "Team Dinner",
                formattedGroupAmount = "€100.00",
                paidByText = "Paid by Alice",
                category = ExpenseCategory.FOOD,
                categoryText = "Food & Drink",
                splits = persistentListOf(
                    SplitDetailUiModel(displayName = "Alice", formattedAmount = "€50.00"),
                    SplitDetailUiModel(displayName = "Bob", formattedAmount = "€50.00")
                )
            )
        )

        composeRule.setContent {
            SplitTripTheme {
                if (!isDetailView) {
                    AddExpenseScreen(
                        groupId = "group-1",
                        uiState = addExpenseState,
                        onEvent = {}
                    )
                } else {
                    ExpenseDetailScreen(
                        uiState = detailState
                    )
                }
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Team Dinner").assertIsDisplayed()

        isDetailView = true
        composeRule.waitForIdle()

        composeRule.onNodeWithText("€100.00").assertIsDisplayed()
        composeRule.onNodeWithText("Paid by Alice").assertIsDisplayed()
        composeRule.onNodeWithText("Food & Drink").assertIsDisplayed()
        composeRule.onNodeWithText(splitBreakdownLabel, substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Alice").assertIsDisplayed()
        composeRule.onNodeWithText("Bob").assertIsDisplayed()
    }

    @Test
    fun givenSettlementFlow_whenViewingSuggestions_thenSettlementCardsAndConfirmActionAreFunctional() {
        val confirmLabel = context.getString(SettlementR.string.settlement_overview_confirm)
        var confirmedSettlementId: String? = null

        val settlement = SettlementRowUiModel(
            settlementId = "settlement-1",
            debtorId = "user-bob",
            creditorId = "user-alice",
            debtorName = "Bob",
            creditorName = "Alice",
            directionTitle = "Bob owes Alice",
            formattedAmount = "€50.00",
            isCurrentUserDebtor = false,
            isCurrentUserCreditor = true,
            pocketTypeLabel = "Virtual",
            currencyCode = "EUR",
            statusLabel = "Pending",
            statusChipStyle = SettlementRowStatusStyle.WARNING,
            canCurrentUserConfirm = true,
            canCurrentUserDispute = false,
            disputedByCurrentUser = false,
            status = SettlementStatus.SUGGESTED
        )

        val uiState = GroupSettlementOverviewUiState(
            isLoading = false,
            isUserCreator = false,
            pendingSettlements = persistentListOf(settlement)
        )

        composeRule.setContent {
            SplitTripTheme {
                GroupSettlementOverviewScreen(
                    uiState = uiState,
                    onEvent = { event ->
                        if (event is GroupSettlementOverviewUiEvent.ConfirmSettlement) {
                            confirmedSettlementId = event.settlementId
                        }
                    }
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Bob owes Alice").assertIsDisplayed()
        composeRule.onNodeWithText("€50.00").assertIsDisplayed()

        composeRule.onNodeWithText(confirmLabel).assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        assertEquals("settlement-1", confirmedSettlementId)
    }

    @Test
    fun givenMultiUserCollaborationScenario_whenCompletingGroupExpenseAndSettlementJourney_thenEntireLoopSucceeds() {
        val confirmLabel = context.getString(SettlementR.string.settlement_overview_confirm)
        val splitBreakdownLabel = context.getString(ExpenseR.string.expense_detail_section_split)
        var confirmedSettlementId: String? = null

        val alice = User(userId = "user-1", displayName = "Alice", email = "alice@example.com")
        val bob = User(userId = "user-2", displayName = "Bob", email = "bob@example.com")

        var journeyStep by mutableStateOf(1)

        val groupState = CreateEditGroupUiState(
            currentStep = CreateEditGroupStep.MEMBERS,
            groupName = "Summer Roadtrip",
            selectedMembers = persistentListOf(alice, bob)
        )

        val expenseDetailState = ExpenseDetailUiState(
            isLoading = false,
            expense = ExpenseDetailUiModel(
                id = "exp-1",
                title = "Dinner",
                formattedGroupAmount = "€100.00",
                paidByText = "Paid by Alice",
                category = ExpenseCategory.FOOD,
                categoryText = "Food & Drink",
                splits = persistentListOf(
                    SplitDetailUiModel(displayName = "Alice", formattedAmount = "€50.00"),
                    SplitDetailUiModel(displayName = "Bob", formattedAmount = "€50.00")
                )
            )
        )

        val pendingSettlement = SettlementRowUiModel(
            settlementId = "settlement-trip-1",
            debtorId = "user-2",
            creditorId = "user-1",
            debtorName = "Bob",
            creditorName = "Alice",
            directionTitle = "Bob owes Alice",
            formattedAmount = "€50.00",
            isCurrentUserDebtor = false,
            isCurrentUserCreditor = true,
            pocketTypeLabel = "Virtual",
            currencyCode = "EUR",
            statusLabel = "Pending",
            statusChipStyle = SettlementRowStatusStyle.WARNING,
            canCurrentUserConfirm = true,
            canCurrentUserDispute = false,
            disputedByCurrentUser = false,
            status = SettlementStatus.SUGGESTED
        )

        val settlementState = GroupSettlementOverviewUiState(
            isLoading = false,
            isUserCreator = false,
            pendingSettlements = persistentListOf(pendingSettlement)
        )

        composeRule.setContent {
            SplitTripTheme {
                when (journeyStep) {
                    1 -> CreateEditGroupScreen(
                        uiState = groupState,
                        onScannerClick = {},
                        onEvent = {}
                    )
                    2 -> ExpenseDetailScreen(
                        uiState = expenseDetailState
                    )
                    3 -> GroupSettlementOverviewScreen(
                        uiState = settlementState,
                        onEvent = { event ->
                            if (event is GroupSettlementOverviewUiEvent.ConfirmSettlement) {
                                confirmedSettlementId = event.settlementId
                            }
                        }
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Alice").assertIsDisplayed()
        composeRule.onNodeWithText("Bob").assertIsDisplayed()

        journeyStep = 2
        composeRule.waitForIdle()
        composeRule.onNodeWithText("€100.00").assertIsDisplayed()
        composeRule.onNodeWithText("Paid by Alice").assertIsDisplayed()
        composeRule.onNodeWithText("Food & Drink").assertIsDisplayed()
        composeRule.onNodeWithText(splitBreakdownLabel, substring = true).assertIsDisplayed()

        journeyStep = 3
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Bob owes Alice").assertIsDisplayed()
        composeRule.onNodeWithText("€50.00").assertIsDisplayed()

        composeRule.onNodeWithText(confirmLabel).assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals("settlement-trip-1", confirmedSettlementId)
    }
}
