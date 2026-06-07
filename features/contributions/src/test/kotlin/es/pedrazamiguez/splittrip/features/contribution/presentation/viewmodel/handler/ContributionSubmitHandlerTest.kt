package es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberOptionUiModel
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.SubunitOptionUiModel
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.service.ContributionValidationService
import es.pedrazamiguez.splittrip.domain.service.impl.ContributionValidationServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.AddContributionUseCase
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.action.AddContributionUiAction
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.state.AddContributionUiState
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("ContributionSubmitHandler")
class ContributionSubmitHandlerTest {

    private lateinit var handler: ContributionSubmitHandler
    private lateinit var addContributionUseCase: AddContributionUseCase
    private lateinit var contributionValidationService: ContributionValidationService

    private lateinit var uiState: MutableStateFlow<AddContributionUiState>
    private lateinit var actions: MutableSharedFlow<AddContributionUiAction>

    private val testMembers = persistentListOf(
        MemberOptionUiModel(userId = "user-1", displayName = "Andrés", isCurrentUser = true),
        MemberOptionUiModel(userId = "user-2", displayName = "Ana", isCurrentUser = false)
    )

    private val testSubunits = persistentListOf(
        SubunitOptionUiModel(id = "subunit-1", name = "Couple A")
    )

    /** Minimal valid state for submission. */
    private val validState = AddContributionUiState(
        amountInput = "100",
        amountError = false,
        groupCurrencyCode = "EUR",
        groupCurrencySymbol = "€",
        contributionScope = PayerType.USER,
        selectedMemberId = "user-1",
        selectedMemberDisplayName = "Andrés",
        groupMembers = testMembers,
        subunitOptions = testSubunits
    )

    @BeforeEach
    fun setUp() {
        addContributionUseCase = mockk()
        contributionValidationService = ContributionValidationServiceImpl()

        uiState = MutableStateFlow(validState)
        actions = MutableSharedFlow(extraBufferCapacity = 16)

        handler = ContributionSubmitHandler(
            addContributionUseCase = addContributionUseCase,
            contributionValidationService = contributionValidationService,
            groupCurrencyProvider = { "EUR" }
        )
    }

    @Nested
    @DisplayName("Early-exit guards")
    inner class EarlyExitGuards {

        @Test
        fun `null groupId is a no-op`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleSubmit(null) {}
            advanceUntilIdle()

            coVerify(exactly = 0) { addContributionUseCase(any(), any()) }
        }

        @Test
        fun `invalid amount sets amountError`() = runTest {
            uiState.value = validState.copy(amountInput = "")
            handler.bind(uiState, actions, this)

            handler.handleSubmit("group-1") {}
            advanceUntilIdle()

            assertTrue(uiState.value.amountError)
            coVerify(exactly = 0) { addContributionUseCase(any(), any()) }
        }

        @Test
        fun `zero amount sets amountError`() = runTest {
            uiState.value = validState.copy(amountInput = "0")
            handler.bind(uiState, actions, this)

            handler.handleSubmit("group-1") {}
            advanceUntilIdle()

            assertTrue(uiState.value.amountError)
            coVerify(exactly = 0) { addContributionUseCase(any(), any()) }
        }
    }

    @Nested
    @DisplayName("Subunit validation")
    inner class SubunitValidation {

        @Test
        fun `invalid subunitId emits ShowError`() = runTest {
            uiState.value = validState.copy(
                contributionScope = PayerType.SUBUNIT,
                selectedSubunitId = "invalid-subunit"
            )
            handler.bind(uiState, actions, this)

            val emitted = mutableListOf<AddContributionUiAction>()
            val collectJob = launch { actions.collect { emitted.add(it) } }

            handler.handleSubmit("group-1") {}
            advanceUntilIdle()

            assertTrue(emitted.any { it is AddContributionUiAction.ShowError })
            coVerify(exactly = 0) { addContributionUseCase(any(), any()) }
            collectJob.cancel()
        }

        @Test
        fun `valid subunitId proceeds to submission`() = runTest {
            uiState.value = validState.copy(
                contributionScope = PayerType.SUBUNIT,
                selectedSubunitId = "subunit-1"
            )
            coEvery { addContributionUseCase(any(), any()) } just Runs
            handler.bind(uiState, actions, this)

            handler.handleSubmit("group-1") {}
            advanceUntilIdle()

            coVerify(exactly = 1) { addContributionUseCase("group-1", any()) }
        }
    }

    @Nested
    @DisplayName("Successful submission")
    inner class SuccessfulSubmission {

        @Test
        fun `calls use case and emits ShowSuccess`() = runTest {
            coEvery { addContributionUseCase(any(), any()) } just Runs
            handler.bind(uiState, actions, this)

            val emitted = mutableListOf<AddContributionUiAction>()
            val collectJob = launch { actions.collect { emitted.add(it) } }

            handler.handleSubmit("group-1") {}
            advanceUntilIdle()

            assertTrue(emitted.any { it is AddContributionUiAction.ShowSuccess })
            assertFalse(uiState.value.isLoading)
            coVerify(exactly = 1) { addContributionUseCase("group-1", any()) }
            collectJob.cancel()
        }

        @Test
        fun `invokes onSuccess callback`() = runTest {
            coEvery { addContributionUseCase(any(), any()) } just Runs
            handler.bind(uiState, actions, this)

            var callbackInvoked = false
            handler.handleSubmit("group-1") { callbackInvoked = true }
            advanceUntilIdle()

            assertTrue(callbackInvoked)
        }

        @Test
        fun `passes selectedMemberId as contribution userId`() = runTest {
            coEvery { addContributionUseCase(any(), any()) } just Runs
            handler.bind(uiState, actions, this)

            handler.handleSubmit("group-1") {}
            advanceUntilIdle()

            coVerify {
                addContributionUseCase(
                    "group-1",
                    match { it.userId == "user-1" }
                )
            }
        }

        @Test
        fun `passes correct contribution scope`() = runTest {
            uiState.value = validState.copy(
                contributionScope = PayerType.SUBUNIT,
                selectedSubunitId = "subunit-1"
            )
            coEvery { addContributionUseCase(any(), any()) } just Runs
            handler.bind(uiState, actions, this)

            handler.handleSubmit("group-1") {}
            advanceUntilIdle()

            coVerify {
                addContributionUseCase(
                    "group-1",
                    match {
                        it.contributionScope == PayerType.SUBUNIT &&
                            it.subunitId == "subunit-1"
                    }
                )
            }
        }
    }

    @Nested
    @DisplayName("Failure handling")
    inner class FailureHandling {

        @Test
        fun `use case failure emits ShowError`() = runTest {
            coEvery {
                addContributionUseCase(any(), any())
            } throws RuntimeException("Boom")
            handler.bind(uiState, actions, this)

            val emitted = mutableListOf<AddContributionUiAction>()
            val collectJob = launch { actions.collect { emitted.add(it) } }

            handler.handleSubmit("group-1") {}
            advanceUntilIdle()

            assertTrue(emitted.any { it is AddContributionUiAction.ShowError })
            assertFalse(uiState.value.isLoading)
            collectJob.cancel()
        }

        @Test
        fun `use case failure does not invoke onSuccess`() = runTest {
            coEvery {
                addContributionUseCase(any(), any())
            } throws RuntimeException("Boom")
            handler.bind(uiState, actions, this)

            var callbackInvoked = false
            handler.handleSubmit("group-1") { callbackInvoked = true }
            advanceUntilIdle()

            assertFalse(callbackInvoked)
        }
    }
}
