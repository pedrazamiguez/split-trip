package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.SubunitOptionUiModel
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.WithdrawalPoolOption
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetAvailableWithdrawalPoolsUseCase
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseOptionsUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.model.WithdrawalPoolOptionUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [WithdrawalPoolSelectionDelegate].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WithdrawalPoolSelectionDelegateTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var delegate: WithdrawalPoolSelectionDelegate
    private lateinit var getAvailableWithdrawalPoolsUseCase: GetAvailableWithdrawalPoolsUseCase
    private lateinit var addExpenseOptionsMapper: AddExpenseOptionsUiMapper

    private val groupPoolDomain = WithdrawalPoolOption(scope = PayerType.GROUP, ownerId = null)
    private val userPoolDomain = WithdrawalPoolOption(scope = PayerType.USER, ownerId = "user-1")
    private val subunitPoolDomain = WithdrawalPoolOption(scope = PayerType.SUBUNIT, ownerId = "sub-1")

    private val groupPoolUi = WithdrawalPoolOptionUiModel(
        scope = PayerType.GROUP,
        ownerId = null,
        displayLabel = "Group cash"
    )
    private val userPoolUi = WithdrawalPoolOptionUiModel(
        scope = PayerType.USER,
        ownerId = "user-1",
        displayLabel = "My personal cash"
    )
    private val subunitPoolUi = WithdrawalPoolOptionUiModel(
        scope = PayerType.SUBUNIT,
        ownerId = "sub-1",
        displayLabel = "Alpha cash"
    )

    @BeforeEach
    fun setUp() {
        getAvailableWithdrawalPoolsUseCase = mockk(relaxed = true)
        addExpenseOptionsMapper = mockk(relaxed = true)

        delegate = WithdrawalPoolSelectionDelegate(
            getAvailableWithdrawalPoolsUseCase = getAvailableWithdrawalPoolsUseCase,
            addExpenseOptionsMapper = addExpenseOptionsMapper
        )
    }

    // ── fetchPools ──────────────────────────────────────────────────────────

    @Nested
    inner class FetchPools {

        @Test
        fun `when use case returns empty list - clears pool state and invokes callback`() =
            runTest(testDispatcher) {
                val stateFlow = MutableStateFlow(AddExpenseUiState())
                var callbackInvoked = false

                coEvery {
                    getAvailableWithdrawalPoolsUseCase(any(), any(), any(), any(), any())
                } returns emptyList()

                every {
                    addExpenseOptionsMapper.mapWithdrawalPoolOptions(emptyList(), any())
                } returns persistentListOf()

                delegate.fetchPools(
                    groupId = "group-1",
                    currency = "EUR",
                    payerType = PayerType.GROUP,
                    payerId = "user-1",
                    scope = testScope,
                    stateFlow = stateFlow,
                    onPoolResolved = { callbackInvoked = true }
                )
                advanceUntilIdle()

                assertTrue(stateFlow.value.availableWithdrawalPools.isEmpty())
                assertNull(stateFlow.value.selectedWithdrawalPool)
                assertTrue(callbackInvoked)
            }

        @Test
        fun `when use case returns single pool - auto-selects silently and invokes callback`() =
            runTest(testDispatcher) {
                val stateFlow = MutableStateFlow(AddExpenseUiState())
                var callbackInvoked = false

                coEvery {
                    getAvailableWithdrawalPoolsUseCase(any(), any(), any(), any(), any())
                } returns listOf(groupPoolDomain)

                every {
                    addExpenseOptionsMapper.mapWithdrawalPoolOptions(listOf(groupPoolDomain), any())
                } returns listOf(groupPoolUi).toImmutableList()

                delegate.fetchPools(
                    groupId = "group-1",
                    currency = "EUR",
                    payerType = PayerType.GROUP,
                    payerId = "user-1",
                    scope = testScope,
                    stateFlow = stateFlow,
                    onPoolResolved = { callbackInvoked = true }
                )
                advanceUntilIdle()

                // Pool list stays hidden (single pool → no widget shown)
                assertTrue(stateFlow.value.availableWithdrawalPools.isEmpty())
                assertEquals(groupPoolUi, stateFlow.value.selectedWithdrawalPool)
                assertTrue(callbackInvoked)
            }

        @Test
        fun `when use case returns multiple pools - populates list, pre-selects first, invokes callback`() =
            runTest(testDispatcher) {
                val stateFlow = MutableStateFlow(AddExpenseUiState())
                var callbackInvoked = false

                coEvery {
                    getAvailableWithdrawalPoolsUseCase(any(), any(), any(), any(), any())
                } returns listOf(groupPoolDomain, userPoolDomain)

                every {
                    addExpenseOptionsMapper.mapWithdrawalPoolOptions(
                        listOf(groupPoolDomain, userPoolDomain),
                        any()
                    )
                } returns listOf(groupPoolUi, userPoolUi).toImmutableList()

                delegate.fetchPools(
                    groupId = "group-1",
                    currency = "EUR",
                    payerType = PayerType.GROUP,
                    payerId = "user-1",
                    scope = testScope,
                    stateFlow = stateFlow,
                    onPoolResolved = { callbackInvoked = true }
                )
                advanceUntilIdle()

                assertEquals(2, stateFlow.value.availableWithdrawalPools.size)
                assertEquals(groupPoolUi, stateFlow.value.selectedWithdrawalPool)
                assertTrue(callbackInvoked)
            }

        @Test
        fun `when use case throws - clears pool state without invoking callback`() =
            runTest(testDispatcher) {
                val stateFlow = MutableStateFlow(
                    AddExpenseUiState(
                        selectedWithdrawalPool = groupPoolUi,
                        availableWithdrawalPools = listOf(groupPoolUi, userPoolUi).toImmutableList()
                    )
                )
                var callbackInvoked = false

                coEvery {
                    getAvailableWithdrawalPoolsUseCase(any(), any(), any(), any(), any())
                } throws RuntimeException("network error")

                delegate.fetchPools(
                    groupId = "group-1",
                    currency = "EUR",
                    payerType = PayerType.GROUP,
                    payerId = "user-1",
                    scope = testScope,
                    stateFlow = stateFlow,
                    onPoolResolved = { callbackInvoked = true }
                )
                advanceUntilIdle()

                assertTrue(stateFlow.value.availableWithdrawalPools.isEmpty())
                assertNull(stateFlow.value.selectedWithdrawalPool)
                assertFalse(callbackInvoked)
            }

        @Test
        fun `uses subunit ids from contributionSubunitOptions when building use case call`() =
            runTest(testDispatcher) {
                val subunitOption = SubunitOptionUiModel(id = "sub-1", name = "Alpha")
                val stateFlow = MutableStateFlow(
                    AddExpenseUiState(
                        contributionSubunitOptions = listOf(subunitOption).toImmutableList()
                    )
                )

                coEvery {
                    getAvailableWithdrawalPoolsUseCase(
                        groupId = "group-1",
                        currency = "EUR",
                        payerType = PayerType.SUBUNIT,
                        payerId = "user-1",
                        subunitIds = listOf("sub-1")
                    )
                } returns emptyList()

                every {
                    addExpenseOptionsMapper.mapWithdrawalPoolOptions(any(), any())
                } returns persistentListOf()

                delegate.fetchPools(
                    groupId = "group-1",
                    currency = "EUR",
                    payerType = PayerType.SUBUNIT,
                    payerId = "user-1",
                    scope = testScope,
                    stateFlow = stateFlow,
                    onPoolResolved = {}
                )
                advanceUntilIdle()

                coEvery {
                    getAvailableWithdrawalPoolsUseCase(
                        groupId = "group-1",
                        currency = "EUR",
                        payerType = PayerType.SUBUNIT,
                        payerId = "user-1",
                        subunitIds = listOf("sub-1")
                    )
                }
            }
    }

    // ── handlePoolSelected ──────────────────────────────────────────────────

    @Nested
    inner class HandlePoolSelected {

        @Test
        fun `when matching pool found - updates selectedWithdrawalPool and invokes callback`() {
            val stateFlow = MutableStateFlow(
                AddExpenseUiState(
                    availableWithdrawalPools = listOf(groupPoolUi, userPoolUi).toImmutableList()
                )
            )
            var callbackInvoked = false

            delegate.handlePoolSelected(
                poolScope = PayerType.USER,
                scopeOwnerId = "user-1",
                stateFlow = stateFlow,
                onPoolResolved = { callbackInvoked = true }
            )

            assertEquals(userPoolUi, stateFlow.value.selectedWithdrawalPool)
            assertTrue(callbackInvoked)
        }

        @Test
        fun `when pool with GROUP scope and null ownerId is found - selects it`() {
            val stateFlow = MutableStateFlow(
                AddExpenseUiState(
                    availableWithdrawalPools = listOf(groupPoolUi, userPoolUi).toImmutableList()
                )
            )
            var callbackInvoked = false

            delegate.handlePoolSelected(
                poolScope = PayerType.GROUP,
                scopeOwnerId = null,
                stateFlow = stateFlow,
                onPoolResolved = { callbackInvoked = true }
            )

            assertEquals(groupPoolUi, stateFlow.value.selectedWithdrawalPool)
            assertTrue(callbackInvoked)
        }

        @Test
        fun `when no matching pool found - state unchanged and callback not invoked`() {
            val stateFlow = MutableStateFlow(
                AddExpenseUiState(
                    selectedWithdrawalPool = groupPoolUi,
                    availableWithdrawalPools = listOf(groupPoolUi).toImmutableList()
                )
            )
            var callbackInvoked = false

            delegate.handlePoolSelected(
                poolScope = PayerType.USER,
                scopeOwnerId = "unknown-user",
                stateFlow = stateFlow,
                onPoolResolved = { callbackInvoked = true }
            )

            assertEquals(groupPoolUi, stateFlow.value.selectedWithdrawalPool)
            assertFalse(callbackInvoked)
        }
    }

    // ── clearPoolState ──────────────────────────────────────────────────────

    @Nested
    inner class ClearPoolState {

        @Test
        fun `clears availableWithdrawalPools and selectedWithdrawalPool`() {
            val stateFlow = MutableStateFlow(
                AddExpenseUiState(
                    availableWithdrawalPools = listOf(groupPoolUi, userPoolUi).toImmutableList(),
                    selectedWithdrawalPool = groupPoolUi
                )
            )

            delegate.clearPoolState(stateFlow)

            assertTrue(stateFlow.value.availableWithdrawalPools.isEmpty())
            assertNull(stateFlow.value.selectedWithdrawalPool)
        }

        @Test
        fun `is idempotent when pool state is already empty`() {
            val stateFlow = MutableStateFlow(AddExpenseUiState())

            delegate.clearPoolState(stateFlow)

            assertTrue(stateFlow.value.availableWithdrawalPools.isEmpty())
            assertNull(stateFlow.value.selectedWithdrawalPool)
        }
    }
}
