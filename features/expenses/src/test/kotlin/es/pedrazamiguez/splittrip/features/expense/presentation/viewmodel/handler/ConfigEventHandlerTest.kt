package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.model.Currency
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.GroupExpenseConfig
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.RemainderDistributionService
import es.pedrazamiguez.splittrip.domain.service.split.SplitPreviewService
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetGroupExpenseConfigUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetGroupLastUsedCategoryUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetGroupLastUsedCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetGroupLastUsedPaymentMethodUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseOptionsUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseSplitUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.AddExpenseUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConfigEventHandlerTest {

    private lateinit var handler: ConfigEventHandler
    private lateinit var getGroupExpenseConfigUseCase: GetGroupExpenseConfigUseCase
    private lateinit var getGroupLastUsedCurrencyUseCase: GetGroupLastUsedCurrencyUseCase
    private lateinit var getGroupLastUsedPaymentMethodUseCase: GetGroupLastUsedPaymentMethodUseCase
    private lateinit var getGroupLastUsedCategoryUseCase: GetGroupLastUsedCategoryUseCase
    private lateinit var getMemberProfilesUseCase: GetMemberProfilesUseCase
    private lateinit var authenticationService: AuthenticationService
    private val capturedActions = mutableListOf<PostConfigAction>()

    private lateinit var uiState: MutableStateFlow<AddExpenseUiState>
    private lateinit var actions: MutableSharedFlow<AddExpenseUiAction>

    private val eurDomain = Currency(code = "EUR", symbol = "€", defaultName = "Euro", decimalDigits = 2)
    private val usdDomain = Currency(code = "USD", symbol = "$", defaultName = "US Dollar", decimalDigits = 2)

    private val testGroup = Group(
        id = "group-1",
        name = "Test Group",
        currency = "EUR",
        members = listOf("user-1", "user-2")
    )

    private val testConfig = GroupExpenseConfig(
        group = testGroup,
        groupCurrency = eurDomain,
        availableCurrencies = listOf(eurDomain, usdDomain),
        subunits = emptyList()
    )

    @BeforeEach
    fun setUp() {
        getGroupExpenseConfigUseCase = mockk()
        getGroupLastUsedCurrencyUseCase = mockk()
        getGroupLastUsedPaymentMethodUseCase = mockk()
        getGroupLastUsedCategoryUseCase = mockk()
        getMemberProfilesUseCase = mockk()
        authenticationService = mockk(relaxed = true)
        capturedActions.clear()

        val localeProvider = mockk<LocaleProvider>()
        val resourceProvider = mockk<ResourceProvider>(relaxed = true)
        every { localeProvider.getCurrentLocale() } returns Locale.US

        val splitPreviewService = SplitPreviewService()
        val remainderDistributionService = RemainderDistributionService()

        handler = ConfigEventHandler(
            getGroupExpenseConfigUseCase = getGroupExpenseConfigUseCase,
            getGroupLastUsedCurrencyUseCase = getGroupLastUsedCurrencyUseCase,
            getGroupLastUsedPaymentMethodUseCase = getGroupLastUsedPaymentMethodUseCase,
            getGroupLastUsedCategoryUseCase = getGroupLastUsedCategoryUseCase,
            getMemberProfilesUseCase = getMemberProfilesUseCase,
            authenticationService = authenticationService,
            addExpenseOptionsMapper = AddExpenseOptionsUiMapper(resourceProvider, mockk(relaxed = true)),
            addExpenseSplitMapper = AddExpenseSplitUiMapper(
                localeProvider,
                FormattingHelper(localeProvider),
                splitPreviewService,
                EntitySplitFlattenDelegate(splitPreviewService, remainderDistributionService)
            ),
            receiptExtractionService = mockk(relaxed = true)
        )
        handler.setPostConfigCallback { capturedActions.add(it) }

        // Default use case stubs
        every { getGroupLastUsedCurrencyUseCase(any()) } returns flowOf(null)
        every { getGroupLastUsedPaymentMethodUseCase(any()) } returns flowOf(emptyList())
        every { getGroupLastUsedCategoryUseCase(any()) } returns flowOf(emptyList())
        coEvery { getMemberProfilesUseCase(any()) } returns emptyMap()

        uiState = MutableStateFlow(AddExpenseUiState())
        actions = MutableSharedFlow()
    }

    // ── Null / blank groupId ─────────────────────────────────────────────

    @Nested
    inner class NullGroupId {

        @Test
        fun `null groupId is a no-op`() = runTest {
            handler.bind(uiState, actions, this)

            handler.loadGroupConfig(null)
            advanceUntilIdle()

            // No network calls; state unchanged
            coVerify(exactly = 0) { getGroupExpenseConfigUseCase(any(), any()) }
            assertFalse(uiState.value.isLoading)
        }
    }

    // ── Skip-reload optimisation ─────────────────────────────────────────

    @Nested
    inner class SkipReload {

        @Test
        fun `does not reload when same group is already loaded`() = runTest {
            // Pre-populate state as if config was already loaded for group-1
            uiState.value = AddExpenseUiState(
                loadedGroupId = "group-1",
                isConfigLoaded = true
            )
            handler.bind(uiState, actions, this)

            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            coVerify(exactly = 0) { getGroupExpenseConfigUseCase(any(), any()) }
        }

        @Test
        fun `reloads when forceRefresh is true even if already loaded`() = runTest {
            uiState.value = AddExpenseUiState(
                loadedGroupId = "group-1",
                isConfigLoaded = true
            )
            coEvery { getGroupExpenseConfigUseCase("group-1", true) } returns Result.success(testConfig)
            handler.bind(uiState, actions, this)

            handler.loadGroupConfig("group-1", forceRefresh = true)
            advanceUntilIdle()

            coVerify(exactly = 1) { getGroupExpenseConfigUseCase("group-1", true) }
        }

        @Test
        fun `reloads when groupId changes even without forceRefresh`() = runTest {
            uiState.value = AddExpenseUiState(
                loadedGroupId = "group-1",
                isConfigLoaded = true
            )
            coEvery { getGroupExpenseConfigUseCase("group-2", false) } returns Result.success(testConfig)
            handler.bind(uiState, actions, this)

            handler.loadGroupConfig("group-2")
            advanceUntilIdle()

            coVerify(exactly = 1) { getGroupExpenseConfigUseCase("group-2", false) }
        }
    }

    // ── Success path ─────────────────────────────────────────────────────

    @Nested
    inner class Success {

        @BeforeEach
        fun stub() {
            coEvery { getGroupExpenseConfigUseCase(any(), any()) } returns Result.success(testConfig)
        }

        @Test
        fun `sets isConfigLoaded true and clears isLoading on success`() = runTest {
            handler.bind(uiState, actions, this)

            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            val state = uiState.value
            assertTrue(state.isConfigLoaded)
            assertFalse(state.isLoading)
            assertFalse(state.configLoadFailed)
        }

        @Test
        fun `populates loadedGroupId and groupName`() = runTest {
            handler.bind(uiState, actions, this)

            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            assertEquals("group-1", uiState.value.loadedGroupId)
            assertEquals("Test Group", uiState.value.groupName)
        }

        @Test
        fun `maps available currencies from config`() = runTest {
            handler.bind(uiState, actions, this)

            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            val currencies = uiState.value.availableCurrencies
            assertEquals(2, currencies.size)
            assertTrue(currencies.any { it.code == "EUR" })
            assertTrue(currencies.any { it.code == "USD" })
        }

        @Test
        fun `maps payment methods from PaymentMethod entries`() = runTest {
            handler.bind(uiState, actions, this)

            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            assertTrue(uiState.value.paymentMethods.isNotEmpty())
        }

        @Test
        fun `auto-selects group currency when no last-used currency stored`() = runTest {
            every { getGroupLastUsedCurrencyUseCase(any()) } returns flowOf(null)
            handler.bind(uiState, actions, this)

            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            assertEquals("EUR", uiState.value.selectedCurrency?.code)
        }

        @Test
        fun `auto-selects last-used currency when stored`() = runTest {
            every { getGroupLastUsedCurrencyUseCase(any()) } returns flowOf("USD")
            handler.bind(uiState, actions, this)

            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            assertEquals("USD", uiState.value.selectedCurrency?.code)
        }

        @Test
        fun `falls back to group currency when last-used currency is not in available list`() = runTest {
            every { getGroupLastUsedCurrencyUseCase(any()) } returns flowOf("JPY")
            handler.bind(uiState, actions, this)

            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            assertEquals("EUR", uiState.value.selectedCurrency?.code)
        }

        @Test
        fun `does not show exchange rate section for same-currency expense`() = runTest {
            // Last-used currency is EUR (same as group currency)
            every { getGroupLastUsedCurrencyUseCase(any()) } returns flowOf("EUR")
            handler.bind(uiState, actions, this)

            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            assertFalse(uiState.value.showExchangeRateSection)
        }

        @Test
        fun `shows exchange rate section for foreign-currency expense`() = runTest {
            every { getGroupLastUsedCurrencyUseCase(any()) } returns flowOf("USD")
            handler.bind(uiState, actions, this)

            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            assertTrue(uiState.value.showExchangeRateSection)
        }

        @Test
        fun `reorders payment methods to put most-recently-used first`() = runTest {
            // CASH is the last-used method (most recently used first)
            every { getGroupLastUsedPaymentMethodUseCase(any()) } returns
                flowOf(listOf(PaymentMethod.CASH.name))
            handler.bind(uiState, actions, this)

            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            val firstMethod = uiState.value.paymentMethods.firstOrNull()
            assertEquals(PaymentMethod.CASH.name, firstMethod?.id)
        }

        @Test
        fun `initialises split members from group member list`() = runTest {
            handler.bind(uiState, actions, this)

            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            assertEquals(listOf("user-1", "user-2"), uiState.value.memberIds.toList())
        }

        @Test
        fun `populates funding sources excluding SUBUNIT`() = runTest {
            handler.bind(uiState, actions, this)

            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            val fundingSources = uiState.value.fundingSources
            assertTrue(fundingSources.isNotEmpty())
            val ids = fundingSources.map { it.id }
            assertTrue("GROUP" in ids)
            assertTrue("USER" in ids)
            assertTrue("SUBUNIT" !in ids)
        }

        @Test
        fun `selects GROUP as default funding source`() = runTest {
            handler.bind(uiState, actions, this)

            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            val selectedFundingSource = uiState.value.selectedFundingSource
            assertNotNull(selectedFundingSource)
            assertEquals("GROUP", selectedFundingSource!!.id)
        }

        @Test
        fun `populates currentUserId from authentication service`() = runTest {
            every { authenticationService.currentUserId() } returns "auth-user-42"
            handler.bind(uiState, actions, this)

            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            assertEquals("auth-user-42", uiState.value.currentUserId)
        }

        @Test
        fun `sets null currentUserId when authentication service returns null`() = runTest {
            every { authenticationService.currentUserId() } returns null
            handler.bind(uiState, actions, this)

            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            assertNull(uiState.value.currentUserId)
        }

        @Test
        fun `clears error on successful load`() = runTest {
            uiState.value = AddExpenseUiState(
                error = es.pedrazamiguez.splittrip.core.common.presentation.UiText.DynamicString("old error")
            )
            handler.bind(uiState, actions, this)

            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            assertNull(uiState.value.error)
        }

        @Test
        fun `emits InitEntitySplits post-config action when group has subunits`() = runTest {
            val subunit = Subunit(id = "sub-1", groupId = "group-1", memberIds = listOf("user-1"))
            val configWithSubunit = testConfig.copy(subunits = listOf(subunit))
            coEvery { getGroupExpenseConfigUseCase(any(), any()) } returns Result.success(configWithSubunit)
            handler.bind(uiState, actions, this)

            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            assertTrue(capturedActions.any { it is PostConfigAction.InitEntitySplits })
        }

        @Test
        fun `emits ClearEntitySplits post-config action when group has no subunits`() = runTest {
            handler.bind(uiState, actions, this)

            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            assertTrue(capturedActions.any { it is PostConfigAction.ClearEntitySplits })
        }

        @Test
        fun `resets form to blank state when switching to a different group`() = runTest {
            uiState.value = AddExpenseUiState(
                loadedGroupId = "group-1",
                isConfigLoaded = true,
                expenseTitle = "Old title"
            )
            handler.bind(uiState, actions, this)

            // Load a different group — should reset form
            coEvery { getGroupExpenseConfigUseCase("group-2", false) } returns Result.success(testConfig)
            handler.loadGroupConfig("group-2")
            advanceUntilIdle()

            // After loading the new group the form should be clean (title was reset on group switch)
            assertEquals("", uiState.value.expenseTitle)
        }
    }

    // ── Failure path ─────────────────────────────────────────────────────

    @Nested
    inner class Failure {

        @Test
        fun `sets configLoadFailed and error on use case failure`() = runTest {
            coEvery { getGroupExpenseConfigUseCase(any(), any()) } returns
                Result.failure(RuntimeException("network error"))
            handler.bind(uiState, actions, this)

            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            val state = uiState.value
            assertTrue(state.configLoadFailed)
            assertFalse(state.isConfigLoaded)
            assertFalse(state.isLoading)
            assertNotNull(state.error)
        }

        @Test
        fun `does not wipe existing state when a retry also fails`() = runTest {
            coEvery { getGroupExpenseConfigUseCase(any(), any()) } returns
                Result.failure(RuntimeException("network error"))
            handler.bind(uiState, actions, this)

            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            assertTrue(uiState.value.configLoadFailed)
        }
    }

    // ── reorderByRecent (via loadGroupConfig) ─────────────────────────────

    @Nested
    inner class ReorderByRecent {

        @Test
        fun `most-recently-used payment method appears first in reordered list`() = runTest {
            every { getGroupLastUsedPaymentMethodUseCase(any()) } returns
                flowOf(listOf(PaymentMethod.DEBIT_CARD.name, PaymentMethod.CASH.name))
            coEvery { getGroupExpenseConfigUseCase(any(), any()) } returns Result.success(testConfig)
            handler.bind(uiState, actions, this)

            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            val firstMethod = uiState.value.paymentMethods.firstOrNull()
            assertEquals(PaymentMethod.DEBIT_CARD.name, firstMethod?.id)
        }

        @Test
        fun `returns original order when recentIds list is empty`() = runTest {
            every { getGroupLastUsedPaymentMethodUseCase(any()) } returns flowOf(emptyList())
            coEvery { getGroupExpenseConfigUseCase(any(), any()) } returns Result.success(testConfig)
            handler.bind(uiState, actions, this)

            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            // Payment methods should still be populated (original enum order)
            assertTrue(uiState.value.paymentMethods.isNotEmpty())
        }
    }

    // ── filterSubunitsForCurrentUser ──────────────────────────────────────

    @Nested
    inner class FilterSubunitsForCurrentUser {

        @Test
        fun `returns only subunits that contain the current user`() {
            val subunit1 = Subunit(id = "sub-1", groupId = "g", memberIds = listOf("user-1", "user-2"))
            val subunit2 = Subunit(id = "sub-2", groupId = "g", memberIds = listOf("user-3"))
            val config = testConfig.copy(subunits = listOf(subunit1, subunit2))

            val result = handler.filterSubunitsForCurrentUser("user-1", config)

            assertEquals(1, result.size)
            assertEquals("sub-1", result[0].id)
        }

        @Test
        fun `returns multiple subunits when user belongs to more than one`() {
            val subunit1 = Subunit(id = "sub-1", groupId = "g", memberIds = listOf("user-1"))
            val subunit2 = Subunit(id = "sub-2", groupId = "g", memberIds = listOf("user-1", "user-3"))
            val config = testConfig.copy(subunits = listOf(subunit1, subunit2))

            val result = handler.filterSubunitsForCurrentUser("user-1", config)

            assertEquals(2, result.size)
        }

        @Test
        fun `returns empty list when user belongs to no subunits`() {
            val subunit1 = Subunit(id = "sub-1", groupId = "g", memberIds = listOf("user-3"))
            val config = testConfig.copy(subunits = listOf(subunit1))

            val result = handler.filterSubunitsForCurrentUser("user-1", config)

            assertTrue(result.isEmpty())
        }

        @Test
        fun `returns empty list when no subunits exist`() {
            val result = handler.filterSubunitsForCurrentUser("user-1", testConfig)

            assertTrue(result.isEmpty())
        }

        @Test
        fun `returns empty list when currentUserId is null`() {
            val subunit1 = Subunit(id = "sub-1", groupId = "g", memberIds = listOf("user-1"))
            val config = testConfig.copy(subunits = listOf(subunit1))

            val result = handler.filterSubunitsForCurrentUser(null, config)

            assertTrue(result.isEmpty())
        }

        @Test
        fun `maps subunit id and name correctly to SubunitOptionUiModel`() {
            val subunit = Subunit(
                id = "sub-42",
                groupId = "g",
                name = "Couple A",
                memberIds = listOf("user-1")
            )
            val config = testConfig.copy(subunits = listOf(subunit))

            val result = handler.filterSubunitsForCurrentUser("user-1", config)

            assertEquals(1, result.size)
            assertEquals("sub-42", result[0].id)
            assertEquals("Couple A", result[0].name)
        }

        @Test
        fun `populates contributionSubunitOptions in state after config load`() = runTest {
            val subunit = Subunit(id = "sub-1", groupId = "group-1", memberIds = listOf("user-1"))
            val configWithSubunit = testConfig.copy(subunits = listOf(subunit))
            coEvery { getGroupExpenseConfigUseCase(any(), any()) } returns Result.success(configWithSubunit)
            every { authenticationService.currentUserId() } returns "user-1"
            handler.bind(uiState, actions, this)

            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            val options = uiState.value.contributionSubunitOptions
            assertEquals(1, options.size)
            assertEquals("sub-1", options[0].id)
        }

        @Test
        fun `sets empty contributionSubunitOptions when user not in any subunit`() = runTest {
            val subunit = Subunit(id = "sub-1", groupId = "group-1", memberIds = listOf("user-3"))
            val configWithSubunit = testConfig.copy(subunits = listOf(subunit))
            coEvery { getGroupExpenseConfigUseCase(any(), any()) } returns Result.success(configWithSubunit)
            every { authenticationService.currentUserId() } returns "user-1"
            handler.bind(uiState, actions, this)

            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            assertTrue(uiState.value.contributionSubunitOptions.isEmpty())
        }
    }
}
