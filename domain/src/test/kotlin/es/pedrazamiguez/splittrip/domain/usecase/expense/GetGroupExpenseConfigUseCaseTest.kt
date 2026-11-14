package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Currency
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.CurrencyRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.usecase.expense.impl.GetGroupExpenseConfigUseCaseImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GetGroupExpenseConfigUseCaseTest {

    private lateinit var groupRepository: GroupRepository
    private lateinit var currencyRepository: CurrencyRepository
    private lateinit var subunitRepository: SubunitRepository
    private lateinit var contributionRepository: ContributionRepository
    private lateinit var cashWithdrawalRepository: CashWithdrawalRepository
    private lateinit var useCase: GetGroupExpenseConfigUseCaseImpl

    @BeforeEach
    fun setUp() {
        groupRepository = mockk()
        currencyRepository = mockk()
        subunitRepository = mockk {
            coEvery { getGroupSubunits(any()) } returns emptyList()
        }
        contributionRepository = mockk {
            every { getGroupContributionsFlow(any()) } returns flowOf(emptyList())
        }
        cashWithdrawalRepository = mockk {
            every { getGroupWithdrawalsFlow(any()) } returns flowOf(emptyList())
        }
        useCase = GetGroupExpenseConfigUseCaseImpl(
            groupRepository = groupRepository,
            currencyRepository = currencyRepository,
            subunitRepository = subunitRepository,
            contributionRepository = contributionRepository,
            cashWithdrawalRepository = cashWithdrawalRepository
        )
    }

    private val testCurrencies = listOf(
        Currency("EUR", "€", "Euro", 2),
        Currency("USD", "$", "US Dollar", 2),
        Currency("GBP", "£", "British Pound", 2),
        Currency("THB", "฿", "Thai Baht", 2)
    )

    @Test
    fun `returns config with group currency and allowed currencies`() = runTest {
        // Given
        val groupId = "group-123"
        val group = Group(
            id = groupId,
            name = "Trip to Paris",
            currency = "EUR",
            extraCurrencies = listOf("USD", "GBP")
        )

        coEvery { groupRepository.getGroupById(groupId) } returns group
        coEvery { currencyRepository.getCurrencies(any()) } returns testCurrencies

        // When
        val result = useCase(groupId)

        // Then
        assertTrue(result.isSuccess)
        val config = result.getOrThrow()

        assertEquals(group, config.group)
        assertEquals("EUR", config.groupCurrency.code)
        assertEquals(3, config.availableCurrencies.size)
        assertTrue(config.availableCurrencies.any { it.code == "EUR" })
        assertTrue(config.availableCurrencies.any { it.code == "USD" })
        assertTrue(config.availableCurrencies.any { it.code == "GBP" })
        // THB should not be included as it's not in extraCurrencies
        assertTrue(config.availableCurrencies.none { it.code == "THB" })
    }

    @Test
    fun `fails when groupId is null`() = runTest {
        val result = useCase(null)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `fails when groupId is blank`() = runTest {
        val result = useCase("   ")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `fails when group not found`() = runTest {
        val groupId = "non-existent"
        coEvery { groupRepository.getGroupById(groupId) } returns null

        val result = useCase(groupId)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `fails when group currency not in available currencies`() = runTest {
        val groupId = "group-123"
        val group = Group(
            id = groupId,
            name = "Test Group",
            currency = "XYZ" // Currency that doesn't exist
        )

        coEvery { groupRepository.getGroupById(groupId) } returns group
        coEvery { currencyRepository.getCurrencies(any()) } returns testCurrencies

        val result = useCase(groupId)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `returns only group currency when no extra currencies`() = runTest {
        val groupId = "group-123"
        val group = Group(
            id = groupId,
            name = "Simple Group",
            currency = "EUR",
            extraCurrencies = emptyList()
        )

        coEvery { groupRepository.getGroupById(groupId) } returns group
        coEvery { currencyRepository.getCurrencies(any()) } returns testCurrencies

        val result = useCase(groupId)

        assertTrue(result.isSuccess)
        val config = result.getOrThrow()

        assertEquals(1, config.availableCurrencies.size)
        assertEquals("EUR", config.availableCurrencies[0].code)
    }

    @Test
    fun `handles duplicate currencies in extraCurrencies gracefully`() = runTest {
        val groupId = "group-123"
        val group = Group(
            id = groupId,
            name = "Test Group",
            currency = "EUR",
            extraCurrencies = listOf("EUR", "USD", "EUR") // EUR duplicated
        )

        coEvery { groupRepository.getGroupById(groupId) } returns group
        coEvery { currencyRepository.getCurrencies(any()) } returns testCurrencies

        val result = useCase(groupId)

        assertTrue(result.isSuccess)
        val config = result.getOrThrow()

        // Should have only 2 unique currencies
        assertEquals(2, config.availableCurrencies.size)
    }

    @Test
    fun `passes forceRefresh parameter to currency repository`() = runTest {
        // Given
        val groupId = "group-123"
        val group = Group(
            id = groupId,
            name = "Test Group",
            currency = "EUR",
            extraCurrencies = emptyList()
        )

        coEvery { groupRepository.getGroupById(groupId) } returns group
        coEvery { currencyRepository.getCurrencies(any()) } returns testCurrencies

        // When
        useCase(groupId, forceRefresh = true)

        // Then
        coVerify { currencyRepository.getCurrencies(true) }
    }

    @Test
    fun `defaults forceRefresh to false`() = runTest {
        // Given
        val groupId = "group-123"
        val group = Group(
            id = groupId,
            name = "Test Group",
            currency = "EUR",
            extraCurrencies = emptyList()
        )

        coEvery { groupRepository.getGroupById(groupId) } returns group
        coEvery { currencyRepository.getCurrencies(any()) } returns testCurrencies

        // When
        useCase(groupId)

        // Then
        coVerify { currencyRepository.getCurrencies(false) }
    }

    @Nested
    inner class PocketActivityDetection {

        private val groupId = "group-123"
        private val group = Group(
            id = groupId,
            name = "Test Group",
            currency = "EUR",
            extraCurrencies = emptyList()
        )

        @BeforeEach
        fun setUpGroupAndCurrencies() {
            coEvery { groupRepository.getGroupById(groupId) } returns group
            coEvery { currencyRepository.getCurrencies(any()) } returns testCurrencies
        }

        @Test
        fun `hasManualPocketContributions is false when no contributions and no withdrawals exist`() = runTest {
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(emptyList())
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(emptyList())

            val config = useCase(groupId).getOrThrow()

            assertFalse(config.hasManualPocketContributions)
        }

        @Test
        fun `hasManualPocketContributions is false when only expense-linked contributions exist`() = runTest {
            val expenseLinked = Contribution(
                id = "c-1",
                groupId = groupId,
                linkedExpenseId = "exp-1",
                linkedSettlementId = null
            )
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(listOf(expenseLinked))

            val config = useCase(groupId).getOrThrow()

            assertFalse(config.hasManualPocketContributions)
        }

        @Test
        fun `hasManualPocketContributions is false when only settlement-linked contributions exist`() = runTest {
            val settlementLinked = Contribution(
                id = "c-2",
                groupId = groupId,
                linkedExpenseId = null,
                linkedSettlementId = "settle-1"
            )
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(listOf(settlementLinked))

            val config = useCase(groupId).getOrThrow()

            assertFalse(config.hasManualPocketContributions)
        }

        @Test
        fun `hasManualPocketContributions is true when at least one manual contribution exists`() = runTest {
            val manual = Contribution(
                id = "c-3",
                groupId = groupId,
                linkedExpenseId = null,
                linkedSettlementId = null
            )
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(listOf(manual))

            val config = useCase(groupId).getOrThrow()

            assertTrue(config.hasManualPocketContributions)
        }

        @Test
        fun `hasManualPocketContributions is true when mix of linked and manual contributions exist`() = runTest {
            val expenseLinked = Contribution(
                id = "c-1",
                groupId = groupId,
                linkedExpenseId = "exp-1",
                linkedSettlementId = null
            )
            val manual = Contribution(
                id = "c-2",
                groupId = groupId,
                linkedExpenseId = null,
                linkedSettlementId = null
            )
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns
                flowOf(listOf(expenseLinked, manual))

            val config = useCase(groupId).getOrThrow()

            assertTrue(config.hasManualPocketContributions)
        }

        @Test
        fun `hasManualPocketContributions is true with a cash withdrawal`() = runTest {
            val withdrawal = CashWithdrawal(
                id = "w-1",
                groupId = groupId,
                amountWithdrawn = 10000L,
                remainingAmount = 10000L
            )
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(emptyList())
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(listOf(withdrawal))

            val config = useCase(groupId).getOrThrow()

            assertTrue(config.hasManualPocketContributions)
        }
    }
}
