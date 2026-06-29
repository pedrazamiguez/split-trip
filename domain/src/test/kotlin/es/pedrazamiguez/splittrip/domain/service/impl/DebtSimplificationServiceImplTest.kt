package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DebtSimplificationServiceImplTest {

    private val service = DebtSimplificationServiceImpl()

    @Test
    fun `simplify with empty list returns empty settlements`() {
        val result = service.simplify(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `simplify with all zero balances returns empty settlements`() {
        val balances = listOf(
            MemberBalance(userId = "1", pocketBalance = 0, cashInHand = 0),
            MemberBalance(userId = "2", pocketBalance = 0, cashInHand = 0)
        )
        val result = service.simplify(balances)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `simplify with single debtor and single creditor matches amounts`() {
        // User 1 has totalBalance = -1000 (debtor, owes 1000)
        // User 2 has totalBalance = 1000 (creditor, receives 1000)
        val balances = listOf(
            MemberBalance(userId = "1", pocketBalance = -1000, cashInHand = 0),
            MemberBalance(userId = "2", pocketBalance = 1000, cashInHand = 0)
        )
        val result = service.simplify(balances)

        assertEquals(1, result.size)
        val settlement = result[0]
        assertEquals("1", settlement.fromUserId)
        assertEquals("2", settlement.toUserId)
        assertEquals(1000L, settlement.amount)
    }

    @Test
    fun `simplify with cyclical debts resolves to net zero`() {
        // In a cyclical scenario:
        // A owes B 1000, B owes C 1000, C owes A 1000.
        // Net positions for everyone are 0.
        val balances = listOf(
            MemberBalance(userId = "A", pocketBalance = 0, cashInHand = 0),
            MemberBalance(userId = "B", pocketBalance = 0, cashInHand = 0),
            MemberBalance(userId = "C", pocketBalance = 0, cashInHand = 0)
        )
        val result = service.simplify(balances)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `simplify with cascading debts simplifies to direct settlement`() {
        // A owes B 1000 (net -1000), B owes C 1000 but receives 1000 from A (net 0), C receives 1000 (net 1000)
        // Expected result: A pays C 1000
        val balances = listOf(
            MemberBalance(userId = "A", pocketBalance = -1000, cashInHand = 0),
            MemberBalance(userId = "B", pocketBalance = 0, cashInHand = 0),
            MemberBalance(userId = "C", pocketBalance = 1000, cashInHand = 0)
        )
        val result = service.simplify(balances)

        assertEquals(1, result.size)
        val settlement = result[0]
        assertEquals("A", settlement.fromUserId)
        assertEquals("C", settlement.toUserId)
        assertEquals(1000L, settlement.amount)
    }

    @Test
    fun `simplify with exact matching offsets pairs correctly`() {
        // A net -1000, B net -500, C net 1500
        // Expected: A pays C 1000, B pays C 500
        val balances = listOf(
            MemberBalance(userId = "A", pocketBalance = -1000, cashInHand = 0),
            MemberBalance(userId = "B", pocketBalance = -500, cashInHand = 0),
            MemberBalance(userId = "C", pocketBalance = 1500, cashInHand = 0)
        )
        val result = service.simplify(balances)

        assertEquals(2, result.size)
        // Sorted by absolute amount descending, so debtor A (1000) matched with creditor C (1500) first
        val first = result.find { it.fromUserId == "A" }!!
        assertEquals("C", first.toUserId)
        assertEquals(1000L, first.amount)

        val second = result.find { it.fromUserId == "B" }!!
        assertEquals("C", second.toUserId)
        assertEquals(500L, second.amount)
    }

    @Test
    fun `simplify with large group of 8 members resolves correctly`() {
        // Sum of all net balances must equal 0
        // A: -1000, B: -2000, C: -3000, D: -4000
        // E: 1000, F: 2000, G: 3000, H: 4000
        val balances = listOf(
            MemberBalance(userId = "A", pocketBalance = -1000, cashInHand = 0),
            MemberBalance(userId = "B", pocketBalance = -2000, cashInHand = 0),
            MemberBalance(userId = "C", pocketBalance = -3000, cashInHand = 0),
            MemberBalance(userId = "D", pocketBalance = -4000, cashInHand = 0),
            MemberBalance(userId = "E", pocketBalance = 1000, cashInHand = 0),
            MemberBalance(userId = "F", pocketBalance = 2000, cashInHand = 0),
            MemberBalance(userId = "G", pocketBalance = 3000, cashInHand = 0),
            MemberBalance(userId = "H", pocketBalance = 4000, cashInHand = 0)
        )
        val result = service.simplify(balances)

        // Verify total sum settled is 10000 (total positive/negative balance magnitude)
        val totalSettled = result.sumOf { it.amount }
        assertEquals(10000L, totalSettled)

        // Verify all debts settled (debtor balances and creditor balances become 0 after applying settlements)
        val initialBalances = balances.associate { it.userId to it.totalBalance }.toMutableMap()
        for (settlement in result) {
            initialBalances[settlement.fromUserId] = initialBalances[settlement.fromUserId]!! + settlement.amount
            initialBalances[settlement.toUserId] = initialBalances[settlement.toUserId]!! - settlement.amount
        }

        assertTrue(initialBalances.values.all { it == 0L })
    }

    @Test
    fun `simplify ignores zero balances`() {
        val balances = listOf(
            MemberBalance(userId = "A", pocketBalance = -1000, cashInHand = 0),
            MemberBalance(userId = "B", pocketBalance = 0, cashInHand = 0),
            MemberBalance(userId = "C", pocketBalance = 1000, cashInHand = 0)
        )
        val result = service.simplify(balances)

        assertEquals(1, result.size)
        assertEquals("A", result[0].fromUserId)
        assertEquals("C", result[0].toUserId)
        assertEquals(1000L, result[0].amount)
    }
}
