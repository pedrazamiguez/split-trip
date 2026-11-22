package es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.SubunitOptionUiModel
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.mapper.AddCashWithdrawalUiMapper
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.action.AddCashWithdrawalUiAction
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.event.AddCashWithdrawalUiEvent
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.handler.WithdrawalConfigHandler
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.handler.WithdrawalCurrencyHandler
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.handler.WithdrawalFeeHandler
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.handler.WithdrawalSubmitHandler
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.state.AddCashWithdrawalUiState
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.state.CashWithdrawalStep
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddCashWithdrawalViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var configHandler: WithdrawalConfigHandler
    private lateinit var currencyHandler: WithdrawalCurrencyHandler
    private lateinit var feeHandler: WithdrawalFeeHandler
    private lateinit var submitHandler: WithdrawalSubmitHandler
    private lateinit var addCashWithdrawalUiMapper: AddCashWithdrawalUiMapper
    private lateinit var viewModel: AddCashWithdrawalViewModel

    private lateinit var _uiState: MutableStateFlow<AddCashWithdrawalUiState>

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        configHandler = mockk(relaxed = true)
        currencyHandler = mockk(relaxed = true)
        feeHandler = mockk(relaxed = true)
        submitHandler = mockk(relaxed = true)
        addCashWithdrawalUiMapper = mockk(relaxed = true)
        val uiStateSlot = io.mockk.slot<MutableStateFlow<AddCashWithdrawalUiState>>()
        every { configHandler.bind(capture(uiStateSlot), any(), any()) } answers {
            _uiState = uiStateSlot.captured
        }

        viewModel = AddCashWithdrawalViewModel(
            configHandler = configHandler,
            currencyHandler = currencyHandler,
            feeHandler = feeHandler,
            submitHandler = submitHandler,
            addCashWithdrawalUiMapper = addCashWithdrawalUiMapper
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Handler Delegation ───────────────────────────────────────────────

    @Nested
    inner class HandlerDelegation {

        @Test
        fun `LoadGroupConfig delegates to configHandler`() = runTest(testDispatcher) {
            viewModel.onEvent(AddCashWithdrawalUiEvent.LoadGroupConfig("group-1"))
            advanceUntilIdle()
            verify { configHandler.loadGroupConfig("group-1") }
        }

        @Test
        fun `RetryLoadConfig resets configLoadFailed and delegates with forceRefresh`() =
            runTest(testDispatcher) {
                viewModel.onEvent(AddCashWithdrawalUiEvent.RetryLoadConfig("group-1"))
                advanceUntilIdle()
                // ViewModel resets flag inline before delegating
                assertEquals(false, viewModel.uiState.value.configLoadFailed)
                assertNull(viewModel.uiState.value.error)
                verify { configHandler.loadGroupConfig("group-1", forceRefresh = true) }
            }

        @Test
        fun `CurrencySelected delegates to currencyHandler`() = runTest(testDispatcher) {
            viewModel.onEvent(AddCashWithdrawalUiEvent.CurrencySelected("USD"))
            advanceUntilIdle()
            verify { currencyHandler.handleCurrencySelected("USD") }
        }

        @Test
        fun `WithdrawalAmountChanged delegates to currencyHandler`() = runTest(testDispatcher) {
            viewModel.onEvent(AddCashWithdrawalUiEvent.WithdrawalAmountChanged("100.00"))
            advanceUntilIdle()
            verify { currencyHandler.handleWithdrawalAmountChanged("100.00") }
        }

        @Test
        fun `DeductedAmountChanged delegates to currencyHandler`() = runTest(testDispatcher) {
            viewModel.onEvent(AddCashWithdrawalUiEvent.DeductedAmountChanged("27.00"))
            advanceUntilIdle()
            verify { currencyHandler.handleDeductedAmountChanged("27.00") }
        }

        @Test
        fun `ExchangeRateChanged delegates to currencyHandler`() = runTest(testDispatcher) {
            viewModel.onEvent(AddCashWithdrawalUiEvent.ExchangeRateChanged("37.0"))
            advanceUntilIdle()
            verify { currencyHandler.handleExchangeRateChanged("37.0") }
        }

        @Test
        fun `FeeToggled delegates to feeHandler`() = runTest(testDispatcher) {
            viewModel.onEvent(AddCashWithdrawalUiEvent.FeeToggled(true))
            advanceUntilIdle()
            verify { feeHandler.handleFeeToggled(true) }
        }

        @Test
        fun `FeeAmountChanged delegates to feeHandler`() = runTest(testDispatcher) {
            viewModel.onEvent(AddCashWithdrawalUiEvent.FeeAmountChanged("5.00"))
            advanceUntilIdle()
            verify { feeHandler.handleFeeAmountChanged("5.00") }
        }

        @Test
        fun `FeeCurrencySelected delegates to feeHandler`() = runTest(testDispatcher) {
            viewModel.onEvent(AddCashWithdrawalUiEvent.FeeCurrencySelected("THB"))
            advanceUntilIdle()
            verify { feeHandler.handleFeeCurrencySelected("THB") }
        }

        @Test
        fun `FeeExchangeRateChanged delegates to feeHandler`() = runTest(testDispatcher) {
            viewModel.onEvent(AddCashWithdrawalUiEvent.FeeExchangeRateChanged("37.0"))
            advanceUntilIdle()
            verify { feeHandler.handleFeeExchangeRateChanged("37.0") }
        }

        @Test
        fun `FeeConvertedAmountChanged delegates to feeHandler`() = runTest(testDispatcher) {
            viewModel.onEvent(AddCashWithdrawalUiEvent.FeeConvertedAmountChanged("2.70"))
            advanceUntilIdle()
            verify { feeHandler.handleFeeConvertedAmountChanged("2.70") }
        }

        @Test
        fun `SubmitWithdrawal delegates to submitHandler`() = runTest(testDispatcher) {
            viewModel.onEvent(AddCashWithdrawalUiEvent.SubmitWithdrawal("group-1"))
            advanceUntilIdle()
            verify { submitHandler.submitWithdrawal(eq("group-1"), any()) }
        }
    }

    // ── Inline State Updates ─────────────────────────────────────────────

    @Nested
    inner class InlineStateUpdates {

        @Test
        fun `WithdrawalScopeSelected sets scope to GROUP and clears subunitId`() =
            runTest(testDispatcher) {
                viewModel.onEvent(AddCashWithdrawalUiEvent.WithdrawalScopeSelected(PayerType.GROUP))
                advanceUntilIdle()
                assertEquals(PayerType.GROUP, viewModel.uiState.value.withdrawalScope)
                assertNull(viewModel.uiState.value.selectedSubunitId)
            }

        @Test
        fun `WithdrawalScopeSelected with SUBUNIT sets subunitId`() = runTest(testDispatcher) {
            viewModel.onEvent(
                AddCashWithdrawalUiEvent.WithdrawalScopeSelected(PayerType.SUBUNIT, subunitId = "sub-1")
            )
            advanceUntilIdle()
            assertEquals(PayerType.SUBUNIT, viewModel.uiState.value.withdrawalScope)
            assertEquals("sub-1", viewModel.uiState.value.selectedSubunitId)
        }

        @Test
        fun `TitleChanged updates title in state`() = runTest(testDispatcher) {
            viewModel.onEvent(AddCashWithdrawalUiEvent.TitleChanged("Airport ATM"))
            advanceUntilIdle()
            assertEquals("Airport ATM", viewModel.uiState.value.title)
        }

        @Test
        fun `NotesChanged updates notes in state`() = runTest(testDispatcher) {
            viewModel.onEvent(AddCashWithdrawalUiEvent.NotesChanged("Charged commission"))
            advanceUntilIdle()
            assertEquals("Charged commission", viewModel.uiState.value.notes)
        }
    }

    // ── Wizard Navigation ────────────────────────────────────────────────

    @Nested
    inner class WizardNavigation {

        @Test
        fun `initial state starts at AMOUNT step`() = runTest(testDispatcher) {
            assertEquals(CashWithdrawalStep.AMOUNT, viewModel.uiState.value.currentStep)
        }

        @Test
        fun `NextStep advances to the next applicable step`() = runTest(testDispatcher) {
            // Default state: [AMOUNT, SCOPE, DETAILS, REVIEW] — no exchange-rate or fee steps
            viewModel.onEvent(AddCashWithdrawalUiEvent.NextStep)
            advanceUntilIdle()
            assertEquals(CashWithdrawalStep.SCOPE, viewModel.uiState.value.currentStep)
        }

        @Test
        fun `NextStep does not advance past the last applicable step`() = runTest(testDispatcher) {
            val steps = viewModel.uiState.value.applicableSteps
            repeat(steps.lastIndex) {
                viewModel.onEvent(AddCashWithdrawalUiEvent.NextStep)
                advanceUntilIdle()
            }
            assertEquals(CashWithdrawalStep.REVIEW, viewModel.uiState.value.currentStep)

            // One extra NextStep on REVIEW — should stay at REVIEW
            viewModel.onEvent(AddCashWithdrawalUiEvent.NextStep)
            advanceUntilIdle()
            assertEquals(CashWithdrawalStep.REVIEW, viewModel.uiState.value.currentStep)
        }

        @Test
        fun `PreviousStep goes back to the previous applicable step`() = runTest(testDispatcher) {
            viewModel.onEvent(AddCashWithdrawalUiEvent.NextStep)
            advanceUntilIdle()
            assertEquals(CashWithdrawalStep.SCOPE, viewModel.uiState.value.currentStep)

            viewModel.onEvent(AddCashWithdrawalUiEvent.PreviousStep)
            advanceUntilIdle()
            assertEquals(CashWithdrawalStep.AMOUNT, viewModel.uiState.value.currentStep)
        }

        @Test
        fun `PreviousStep on first step with clean state emits NavigateBack`() = runTest(testDispatcher) {
            assertEquals(CashWithdrawalStep.AMOUNT, viewModel.uiState.value.currentStep)
            assertFalse(viewModel.uiState.value.isDirty)

            val emittedActions = mutableListOf<AddCashWithdrawalUiAction>()
            val job = launch { viewModel.actions.collect { emittedActions.add(it) } }

            viewModel.onEvent(AddCashWithdrawalUiEvent.PreviousStep)
            advanceUntilIdle()

            assertTrue(emittedActions.any { it is AddCashWithdrawalUiAction.NavigateBack })
            job.cancel()
        }

        @Test
        fun `PreviousStep on first step with dirty state emits RequestExitConfirmation`() = runTest(testDispatcher) {
            assertEquals(CashWithdrawalStep.AMOUNT, viewModel.uiState.value.currentStep)

            // Setup initial snapshot so isDirty can evaluate correctly
            _uiState.value = _uiState.value.copy(initialFormSnapshot = _uiState.value.toFormSnapshot())

            // Make it dirty by updating the title (directly handled by ViewModel, not mocked currencyHandler)
            viewModel.onEvent(AddCashWithdrawalUiEvent.TitleChanged("ATM fee"))
            assertTrue(viewModel.uiState.value.isDirty)

            val emittedActions = mutableListOf<AddCashWithdrawalUiAction>()
            val job = launch { viewModel.actions.collect { emittedActions.add(it) } }

            viewModel.onEvent(AddCashWithdrawalUiEvent.PreviousStep)
            advanceUntilIdle()

            assertTrue(emittedActions.any { it is AddCashWithdrawalUiAction.RequestExitConfirmation })
            job.cancel()
        }

        @Test
        fun `CloseWizard with clean state emits NavigateBack`() = runTest(testDispatcher) {
            assertFalse(viewModel.uiState.value.isDirty)

            val emittedActions = mutableListOf<AddCashWithdrawalUiAction>()
            val job = launch { viewModel.actions.collect { emittedActions.add(it) } }

            viewModel.onEvent(AddCashWithdrawalUiEvent.CloseWizard)
            advanceUntilIdle()

            assertTrue(emittedActions.any { it is AddCashWithdrawalUiAction.NavigateBack })
            job.cancel()
        }

        @Test
        fun `CloseWizard with dirty state emits RequestExitConfirmation`() = runTest(testDispatcher) {
            // Setup initial snapshot so isDirty can evaluate correctly
            _uiState.value = _uiState.value.copy(initialFormSnapshot = _uiState.value.toFormSnapshot())

            // Make it dirty
            viewModel.onEvent(AddCashWithdrawalUiEvent.TitleChanged("ATM fee"))
            assertTrue(viewModel.uiState.value.isDirty)

            val emittedActions = mutableListOf<AddCashWithdrawalUiAction>()
            val job = launch { viewModel.actions.collect { emittedActions.add(it) } }

            viewModel.onEvent(AddCashWithdrawalUiEvent.CloseWizard)
            advanceUntilIdle()

            assertTrue(emittedActions.any { it is AddCashWithdrawalUiAction.RequestExitConfirmation })
            job.cancel()
        }

        @Test
        fun `NextStep then PreviousStep returns to original step`() = runTest(testDispatcher) {
            viewModel.onEvent(AddCashWithdrawalUiEvent.NextStep)
            advanceUntilIdle()
            val stepAfterNext = viewModel.uiState.value.currentStep

            viewModel.onEvent(AddCashWithdrawalUiEvent.PreviousStep)
            advanceUntilIdle()

            assertEquals(CashWithdrawalStep.AMOUNT, viewModel.uiState.value.currentStep)
            assertEquals(CashWithdrawalStep.SCOPE, stepAfterNext)
        }

        @Test
        fun `JumpToReview sets currentStep to REVIEW and records jumpedFromStep`() =
            runTest(testDispatcher) {
                // Navigate to DETAILS (optional step): AMOUNT → SCOPE → DETAILS
                viewModel.onEvent(AddCashWithdrawalUiEvent.NextStep) // → SCOPE
                advanceUntilIdle()
                viewModel.onEvent(AddCashWithdrawalUiEvent.NextStep) // → DETAILS
                advanceUntilIdle()
                assertEquals(CashWithdrawalStep.DETAILS, viewModel.uiState.value.currentStep)
                assertTrue(viewModel.uiState.value.currentStep.isOptional)

                // When — Jump to Review
                viewModel.onEvent(AddCashWithdrawalUiEvent.JumpToReview)
                advanceUntilIdle()

                // Then
                assertEquals(CashWithdrawalStep.REVIEW, viewModel.uiState.value.currentStep)
                assertEquals(CashWithdrawalStep.DETAILS, viewModel.uiState.value.jumpedFromStep)
            }

        @Test
        fun `PreviousStep from REVIEW returns to jumpedFromStep`() = runTest(testDispatcher) {
            // Navigate to DETAILS and jump
            viewModel.onEvent(AddCashWithdrawalUiEvent.NextStep) // → SCOPE
            advanceUntilIdle()
            viewModel.onEvent(AddCashWithdrawalUiEvent.NextStep) // → DETAILS
            advanceUntilIdle()
            viewModel.onEvent(AddCashWithdrawalUiEvent.JumpToReview)
            advanceUntilIdle()
            assertEquals(CashWithdrawalStep.REVIEW, viewModel.uiState.value.currentStep)

            // When — Go back
            viewModel.onEvent(AddCashWithdrawalUiEvent.PreviousStep)
            advanceUntilIdle()

            // Then — Should return to DETAILS, not the step before REVIEW
            assertEquals(CashWithdrawalStep.DETAILS, viewModel.uiState.value.currentStep)
            assertNull(viewModel.uiState.value.jumpedFromStep)
        }

        @Test
        fun `JumpToReview is ignored on non-optional step`() = runTest(testDispatcher) {
            // AMOUNT is the first step and is NOT optional
            assertEquals(CashWithdrawalStep.AMOUNT, viewModel.uiState.value.currentStep)
            assertFalse(viewModel.uiState.value.currentStep.isOptional)

            // When
            viewModel.onEvent(AddCashWithdrawalUiEvent.JumpToReview)
            advanceUntilIdle()

            // Then — Should stay on AMOUNT
            assertEquals(CashWithdrawalStep.AMOUNT, viewModel.uiState.value.currentStep)
            assertNull(viewModel.uiState.value.jumpedFromStep)
        }

        @Test
        fun `JumpToStep navigates to the target step`() = runTest(testDispatcher) {
            // Given — advance two steps: AMOUNT → SCOPE → DETAILS
            viewModel.onEvent(AddCashWithdrawalUiEvent.NextStep)
            advanceUntilIdle()
            viewModel.onEvent(AddCashWithdrawalUiEvent.NextStep)
            advanceUntilIdle()
            assertEquals(CashWithdrawalStep.DETAILS, viewModel.uiState.value.currentStep)

            // When — jump back to step 0 (AMOUNT)
            viewModel.onEvent(AddCashWithdrawalUiEvent.JumpToStep(0))
            advanceUntilIdle()

            // Then
            assertEquals(CashWithdrawalStep.AMOUNT, viewModel.uiState.value.currentStep)
        }

        @Test
        fun `JumpToStep clears jumpedFromStep`() = runTest(testDispatcher) {
            // Navigate to DETAILS and jump to REVIEW (sets jumpedFromStep)
            viewModel.onEvent(AddCashWithdrawalUiEvent.NextStep) // → SCOPE
            advanceUntilIdle()
            viewModel.onEvent(AddCashWithdrawalUiEvent.NextStep) // → DETAILS
            advanceUntilIdle()
            viewModel.onEvent(AddCashWithdrawalUiEvent.JumpToReview)
            advanceUntilIdle()
            assertEquals(CashWithdrawalStep.REVIEW, viewModel.uiState.value.currentStep)
            assertEquals(CashWithdrawalStep.DETAILS, viewModel.uiState.value.jumpedFromStep)

            // When — jump back via step indicator
            viewModel.onEvent(AddCashWithdrawalUiEvent.JumpToStep(0))
            advanceUntilIdle()

            // Then — jumpedFromStep is cleared
            assertEquals(CashWithdrawalStep.AMOUNT, viewModel.uiState.value.currentStep)
            assertNull(viewModel.uiState.value.jumpedFromStep)
        }

        @Test
        fun `JumpToStep is a no-op for out-of-bounds index`() = runTest(testDispatcher) {
            // Given — advance one step
            viewModel.onEvent(AddCashWithdrawalUiEvent.NextStep)
            advanceUntilIdle()
            val stepBefore = viewModel.uiState.value.currentStep

            // When — wildly out of bounds
            viewModel.onEvent(AddCashWithdrawalUiEvent.JumpToStep(999))
            advanceUntilIdle()

            // Then — step unchanged
            assertEquals(stepBefore, viewModel.uiState.value.currentStep)
        }
    }

    // ── Member Selection ─────────────────────────────────────────────────

    @Nested
    inner class MemberSelection {

        @Test
        fun `MemberSelected updates state and re-filters subunits`() = runTest(testDispatcher) {
            val filteredSubunits = persistentListOf(
                SubunitOptionUiModel(id = "sub-1", name = "Couple A")
            )
            every { configHandler.filterSubunitsForMember("user-2") } returns filteredSubunits
            every {
                addCashWithdrawalUiMapper.resolveDisplayName("user-2", any())
            } returns "Ana"

            viewModel.onEvent(AddCashWithdrawalUiEvent.MemberSelected("user-2"))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("user-2", state.selectedMemberId)
            assertEquals("Ana", state.selectedMemberDisplayName)
            assertEquals(filteredSubunits, state.subunitOptions)
            assertEquals(PayerType.GROUP, state.withdrawalScope)
            assertNull(state.selectedSubunitId)
        }
    }

    @Nested
    inner class StepInfoSheetEvents {

        @Test
        fun `initial state has isStepInfoSheetVisible set to false`() = runTest(testDispatcher) {
            assertFalse(viewModel.uiState.value.isStepInfoSheetVisible)
        }

        @Test
        fun `ToggleStepInfoSheet with visible true updates isStepInfoSheetVisible to true`() =
            runTest(testDispatcher) {
                viewModel.onEvent(AddCashWithdrawalUiEvent.ToggleStepInfoSheet(visible = true))
                advanceUntilIdle()

                assertTrue(viewModel.uiState.value.isStepInfoSheetVisible)
            }

        @Test
        fun `ToggleStepInfoSheet with visible false updates isStepInfoSheetVisible to false`() =
            runTest(testDispatcher) {
                viewModel.onEvent(AddCashWithdrawalUiEvent.ToggleStepInfoSheet(visible = true))
                advanceUntilIdle()
                assertTrue(viewModel.uiState.value.isStepInfoSheetVisible)

                viewModel.onEvent(AddCashWithdrawalUiEvent.ToggleStepInfoSheet(visible = false))
                advanceUntilIdle()

                assertFalse(viewModel.uiState.value.isStepInfoSheetVisible)
            }

        @Test
        fun `ToggleStepInfoSheet does not mark form as dirty`() = runTest(testDispatcher) {
            _uiState.value = _uiState.value.copy(
                initialFormSnapshot = _uiState.value.toFormSnapshot()
            )
            assertFalse(viewModel.uiState.value.isDirty)

            viewModel.onEvent(AddCashWithdrawalUiEvent.ToggleStepInfoSheet(visible = true))
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isStepInfoSheetVisible)
            assertFalse(viewModel.uiState.value.isDirty)
        }
    }
}
