package es.pedrazamiguez.splittrip.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus
import es.pedrazamiguez.splittrip.features.settlement.R
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
 * Smoke tests for [GroupSettlementOverviewScreen].
 */
@RunWith(AndroidJUnit4::class)
class GroupSettlementOverviewScreenTest {

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun rendersConfirmAndReviewButtons_withoutTruncation_andTriggersCallbacks() {
        val confirmText = context.getString(R.string.settlement_overview_confirm)
        val reviewText = context.getString(R.string.settlement_overview_dispute)
        val events = mutableListOf<GroupSettlementOverviewUiEvent>()

        composeRule.setContent {
            SplitTripTheme {
                GroupSettlementOverviewScreen(
                    uiState = GroupSettlementOverviewUiState(
                        isLoading = false,
                        isUserCreator = false,
                        pendingSettlements = persistentListOf(
                            createRow(
                                id = "s-1",
                                status = SettlementStatus.SUGGESTED,
                                canConfirm = true,
                                canDispute = true
                            )
                        )
                    ),
                    onEvent = { events.add(it) }
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(confirmText).assertIsDisplayed()
        composeRule.onNodeWithText(reviewText).assertIsDisplayed().performClick()

        composeRule.waitForIdle()

        assertEquals(listOf(GroupSettlementOverviewUiEvent.DisputeSettlement("s-1")), events)
    }

    @Test
    fun rendersFullMultiLineDisputeReason_onDisputedSettlement() {
        val longReason =
            "Wrong amount entered — I paid 25.00 EUR in cash at the airport and split the taxi fare equally with Andrés."

        composeRule.setContent {
            SplitTripTheme {
                GroupSettlementOverviewScreen(
                    uiState = GroupSettlementOverviewUiState(
                        isLoading = false,
                        isUserCreator = false,
                        disputedSettlements = persistentListOf(
                            createRow(
                                id = "s-2",
                                status = SettlementStatus.DISPUTED,
                                canConfirm = false,
                                canDispute = false,
                                disputeReason = longReason
                            )
                        )
                    ),
                    onEvent = {}
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(longReason).assertIsDisplayed()
    }

    private fun createRow(
        id: String,
        status: SettlementStatus,
        canConfirm: Boolean,
        canDispute: Boolean,
        disputeReason: String? = null
    ) = SettlementRowUiModel(
        settlementId = id,
        debtorId = "user-1",
        creditorId = "user-2",
        debtorName = "You",
        creditorName = "Andrés",
        directionTitle = "You owe Andrés",
        formattedAmount = "25.00 €",
        isCurrentUserDebtor = true,
        isCurrentUserCreditor = false,
        pocketTypeLabel = "Pocket",
        currencyCode = "EUR",
        statusLabel = "Pending",
        statusChipStyle = PassportTagStyle.NEUTRAL,
        canCurrentUserConfirm = canConfirm,
        canCurrentUserDispute = canDispute,
        disputedByCurrentUser = false,
        disputeReason = disputeReason,
        status = status
    )
}
