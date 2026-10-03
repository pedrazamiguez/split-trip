package es.pedrazamiguez.splittrip.features.settlement.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.service.impl.PocketDebtDistributionServiceImpl
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MemberSpendingChartUiMapperTest {

    private val localeProvider: LocaleProvider = mockk()
    private val userUiMapper: UserUiMapper = mockk()

    private lateinit var mapper: MemberSpendingChartUiMapper

    @BeforeEach
    fun setUp() {
        every { localeProvider.getCurrentLocale() } returns Locale.US
        every { userUiMapper.mapToDisplayName(any(), any(), any(), any(), any()) } answers {
            val fallbackUserId = arg<String>(1)
            "Name_$fallbackUserId"
        }

        mapper = MemberSpendingChartUiMapper(
            localeProvider = localeProvider,
            userUiMapper = userUiMapper,
            pocketDebtDistributionService = PocketDebtDistributionServiceImpl()
        )
    }

    @Test
    fun `toChartUiModel - all within allowance - no spillover`() {
        val balances = listOf(
            MemberBalance(userId = "1", withdrawn = 1000L, cashSpent = 500L, totalSpent = 500L),
            MemberBalance(userId = "2", withdrawn = 1000L, cashSpent = 800L, totalSpent = 800L)
        )

        val result = mapper.toChartUiModel(
            memberBalances = balances,
            cashOnly = true,
            currentUserId = "1",
            memberProfiles = emptyMap(),
            groupCurrencyCode = "USD"
        )

        assertEquals(2, result.bars.size)
        assertTrue(result.bars.all { it.spilloverSegments.isEmpty() })
        assertEquals(500L, result.bars[0].ownSpendingCents)
        assertEquals(800L, result.bars[1].ownSpendingCents)
    }

    @Test
    fun `toChartUiModel - single overspender - spills into others`() {
        val balances = listOf(
            MemberBalance(userId = "Antonio", withdrawn = 166666L, cashSpent = 300000L, totalSpent = 300000L),
            MemberBalance(userId = "Andres", withdrawn = 166666L, cashSpent = 20000L, totalSpent = 20000L),
            MemberBalance(userId = "Pepe", withdrawn = 166666L, cashSpent = 0L, totalSpent = 0L)
        )

        val result = mapper.toChartUiModel(
            memberBalances = balances,
            cashOnly = true,
            currentUserId = null,

            memberProfiles = emptyMap(),
            groupCurrencyCode = "EUR"
        )

        val antonio = result.bars.find { it.userId == "Antonio" }!!
        val andres = result.bars.find { it.userId == "Andres" }!!
        val pepe = result.bars.find { it.userId == "Pepe" }!!

        assertTrue(antonio.spilloverSegments.isEmpty())
        assertEquals(166666L, antonio.ownSpendingCents)

        assertEquals(1, andres.spilloverSegments.size)
        assertEquals(66667L, andres.spilloverSegments.first().amountCents)

        assertEquals(1, pepe.spilloverSegments.size)
        assertEquals(66667L, pepe.spilloverSegments.first().amountCents)
    }

    @Test
    fun `toChartUiModel - multiple overspenders - spills correctly`() {
        val balances = listOf(
            MemberBalance(userId = "Antonio", withdrawn = 166666L, cashSpent = 300000L, totalSpent = 300000L),
            MemberBalance(userId = "Andres", withdrawn = 166666L, cashSpent = 170000L, totalSpent = 170000L),
            MemberBalance(userId = "Pepe", withdrawn = 166666L, cashSpent = 0L, totalSpent = 0L)
        )

        val result = mapper.toChartUiModel(
            memberBalances = balances,
            cashOnly = true,
            currentUserId = null,

            memberProfiles = emptyMap(),
            groupCurrencyCode = "EUR"
        )

        val pepe = result.bars.find { it.userId == "Pepe" }!!
        assertEquals(2, pepe.spilloverSegments.size)
        val sum = pepe.spilloverSegments.sumOf { it.amountCents }
        assertEquals(136668L, sum)
    }

    @Test
    fun `toChartUiModel - cash mode vs all expenses mode`() {
        val balances = listOf(
            MemberBalance(userId = "1", contributed = 1000L, withdrawn = 1000L, cashSpent = 100L, totalSpent = 200L)
        )

        val cashResult = mapper.toChartUiModel(
            memberBalances = balances,
            cashOnly = true,
            currentUserId = "1",
            memberProfiles = emptyMap(),
            groupCurrencyCode = "EUR"
        )
        assertEquals(100L, cashResult.bars[0].ownSpendingCents)

        val allResult = mapper.toChartUiModel(
            memberBalances = balances,
            cashOnly = false,
            currentUserId = "1",
            memberProfiles = emptyMap(),
            groupCurrencyCode = "EUR"
        )
        assertEquals(200L, allResult.bars[0].ownSpendingCents)
    }

    @Test
    fun `toChartUiModel - current user is sorted first`() {
        val balances = listOf(
            MemberBalance(userId = "3"),
            MemberBalance(userId = "1"),
            MemberBalance(userId = "2")
        )

        val result = mapper.toChartUiModel(
            memberBalances = balances,
            cashOnly = true,
            currentUserId = "1",
            memberProfiles = emptyMap(),
            groupCurrencyCode = "EUR"
        )

        assertTrue(result.bars[0].isCurrentUser)
        assertEquals("1", result.bars[0].userId)
    }

    @Test
    fun `toChartUiModel - spillover cents sum correctly (no penny loss)`() {
        val balances = listOf(
            MemberBalance(userId = "1", withdrawn = 0L, cashSpent = 100L, totalSpent = 100L),
            MemberBalance(userId = "2", withdrawn = 100L, cashSpent = 0L, totalSpent = 0L),
            MemberBalance(userId = "3", withdrawn = 100L, cashSpent = 0L, totalSpent = 0L),
            MemberBalance(userId = "4", withdrawn = 100L, cashSpent = 0L, totalSpent = 0L)
        )

        val result = mapper.toChartUiModel(
            memberBalances = balances,
            cashOnly = true,
            currentUserId = null,
            memberProfiles = emptyMap(),
            groupCurrencyCode = "EUR"
        )

        val spillSum = result.bars.flatMap { it.spilloverSegments }.sumOf { it.amountCents }
        assertEquals(100L, spillSum)
    }

    @Test
    fun `toChartUiModel - member with zero withdrawn - bar is fully empty`() {
        val balances = listOf(
            MemberBalance(userId = "1", withdrawn = 0L, cashSpent = 0L, totalSpent = 0L)
        )

        val result = mapper.toChartUiModel(
            memberBalances = balances,
            cashOnly = true,
            currentUserId = null,
            memberProfiles = emptyMap(),
            groupCurrencyCode = "EUR"
        )

        assertEquals(0L, result.bars[0].allowanceCents)
        assertEquals(0L, result.bars[0].ownSpendingCents)
        assertTrue(result.bars[0].spilloverSegments.isEmpty())
    }

    @Test
    fun `toChartUiModel - hasCashExpenses is true when member has withdrawn cash`() {
        val balances = listOf(
            MemberBalance(userId = "1", withdrawn = 1000L, cashSpent = 500L, totalSpent = 500L),
            MemberBalance(userId = "2", withdrawn = 0L, cashSpent = 0L, totalSpent = 0L)
        )

        val result = mapper.toChartUiModel(
            memberBalances = balances,
            cashOnly = true,
            currentUserId = "1",
            memberProfiles = emptyMap(),
            groupCurrencyCode = "USD"
        )

        assertTrue(result.hasCashExpenses)
    }

    @Test
    fun `toChartUiModel - hasCashExpenses is false when no member has withdrawn cash`() {
        val balances = listOf(
            MemberBalance(userId = "1", withdrawn = 0L, cashSpent = 0L, totalSpent = 500L),
            MemberBalance(userId = "2", withdrawn = 0L, cashSpent = 0L, totalSpent = 300L)
        )

        val result = mapper.toChartUiModel(
            memberBalances = balances,
            cashOnly = false,
            currentUserId = "1",
            memberProfiles = emptyMap(),
            groupCurrencyCode = "USD"
        )

        assertFalse(result.hasCashExpenses)
    }
}
