package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.model.SettlementPocketType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PocketDebtDistributionServiceImplTest {

    private val service = PocketDebtDistributionServiceImpl()

    @Test
    fun `distributePocketDebts with empty balances returns empty list`() {
        val result = service.distributePocketDebts(emptyList(), "EUR")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `distributePocketDebts with all zero pocket balances returns empty list`() {
        val balances = listOf(
            MemberBalance(userId = "1", pocketBalance = 0),
            MemberBalance(userId = "2", pocketBalance = 0)
        )
        val result = service.distributePocketDebts(balances, "EUR")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `distributePocketDebts with no debtors returns empty list`() {
        val balances = listOf(
            MemberBalance(userId = "1", pocketBalance = 1000),
            MemberBalance(userId = "2", pocketBalance = 500)
        )
        val result = service.distributePocketDebts(balances, "EUR")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `distributePocketDebts with single debtor and single creditor matches exact amount`() {
        val balances = listOf(
            MemberBalance(userId = "A", pocketBalance = -1000),
            MemberBalance(userId = "B", pocketBalance = 1000)
        )
        val result = service.distributePocketDebts(balances, "EUR")

        assertEquals(1, result.size)
        val settlement = result[0]
        assertEquals("A", settlement.fromUserId)
        assertEquals("B", settlement.toUserId)
        assertEquals(1000L, settlement.amount)
        assertEquals("EUR", settlement.currency)
        assertEquals(SettlementPocketType.POCKET, settlement.sourcePocket)
    }

    @Test
    fun `distributePocketDebts with single debtor distributes evenly among multiple creditors with equal capacity`() {
        val balances = listOf(
            MemberBalance(userId = "Antonio", pocketBalance = -133334),
            MemberBalance(userId = "Andres", pocketBalance = 166667),
            MemberBalance(userId = "Pepe", pocketBalance = 166666)
        )
        val result = service.distributePocketDebts(balances, "EUR")

        assertEquals(2, result.size)
        val toAndres = result.first { it.toUserId == "Andres" }
        val toPepe = result.first { it.toUserId == "Pepe" }

        assertEquals("Antonio", toAndres.fromUserId)
        assertEquals(66667L, toAndres.amount)
        assertEquals(SettlementPocketType.POCKET, toAndres.sourcePocket)

        assertEquals("Antonio", toPepe.fromUserId)
        assertEquals(66667L, toPepe.amount)
        assertEquals(SettlementPocketType.POCKET, toPepe.sourcePocket)

        assertEquals(133334L, result.sumOf { it.amount })
    }

    @Test
    fun `distributePocketDebts spills over debt when one creditor hits capacity limit`() {
        val balances = listOf(
            MemberBalance(userId = "Antonio", pocketBalance = -133333),
            MemberBalance(userId = "Andres", pocketBalance = 46667),
            MemberBalance(userId = "Pepe", pocketBalance = 166666)
        )
        val result = service.distributePocketDebts(balances, "EUR")

        assertEquals(2, result.size)
        val toAndres = result.first { it.toUserId == "Andres" }
        val toPepe = result.first { it.toUserId == "Pepe" }

        assertEquals("Antonio", toAndres.fromUserId)
        assertEquals(46667L, toAndres.amount)

        assertEquals("Antonio", toPepe.fromUserId)
        assertEquals(86666L, toPepe.amount)

        assertEquals(133333L, result.sumOf { it.amount })
    }

    @Test
    fun `distributePocketDebts distributes odd cent remainders without penny loss`() {
        val balances = listOf(
            MemberBalance(userId = "Debtor", pocketBalance = -100),
            MemberBalance(userId = "Creditor1", pocketBalance = 100),
            MemberBalance(userId = "Creditor2", pocketBalance = 100),
            MemberBalance(userId = "Creditor3", pocketBalance = 100)
        )
        val result = service.distributePocketDebts(balances, "EUR")

        assertEquals(3, result.size)
        val toCreditor1 = result.first { it.toUserId == "Creditor1" }
        val toCreditor2 = result.first { it.toUserId == "Creditor2" }
        val toCreditor3 = result.first { it.toUserId == "Creditor3" }

        assertEquals(34L, toCreditor1.amount)
        assertEquals(33L, toCreditor2.amount)
        assertEquals(33L, toCreditor3.amount)

        assertEquals(100L, result.sumOf { it.amount })
    }

    @Test
    fun `distributePocketDebts with multiple debtors reduces remaining creditor capacities progressively`() {
        val balances = listOf(
            MemberBalance(userId = "Debtor1", pocketBalance = -100),
            MemberBalance(userId = "Debtor2", pocketBalance = -50),
            MemberBalance(userId = "Creditor1", pocketBalance = 80),
            MemberBalance(userId = "Creditor2", pocketBalance = 70)
        )
        val result = service.distributePocketDebts(balances, "EUR")

        val totalToCreditor1 = result.filter { it.toUserId == "Creditor1" }.sumOf { it.amount }
        val totalToCreditor2 = result.filter { it.toUserId == "Creditor2" }.sumOf { it.amount }

        assertEquals(80L, totalToCreditor1)
        assertEquals(70L, totalToCreditor2)
        assertEquals(150L, result.sumOf { it.amount })
    }
}
