package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.model.CurrencyAmount
import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.model.SettlementPocketType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DebtSimplificationServiceImplTest {

    private val service = DebtSimplificationServiceImpl(
        CashDebtScalingServiceImpl(RemainderDistributionServiceImpl()),
        PocketDebtDistributionServiceImpl()
    )

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

    @Test
    fun `simplify with totalBalance unaffected by opposite pocket and cash signs`() {
        val balances = listOf(
            MemberBalance(userId = "A", pocketBalance = 500, cashInHand = -300),
            MemberBalance(userId = "B", pocketBalance = -500, cashInHand = 300)
        )
        val result = service.simplify(balances)
        assertEquals(1, result.size)
        assertEquals("B", result[0].fromUserId)
        assertEquals("A", result[0].toUserId)
        assertEquals(200L, result[0].amount)
        assertEquals("", result[0].currency)
        assertEquals(SettlementPocketType.NET, result[0].sourcePocket)
    }

    class SimplifyByPocket {
        private val service = DebtSimplificationServiceImpl(
            CashDebtScalingServiceImpl(RemainderDistributionServiceImpl()),
            PocketDebtDistributionServiceImpl()
        )

        @Test
        fun `simplifyByPocket with single pocket debtor distributes fairly among multiple pocket creditors`() {
            val balances = listOf(
                MemberBalance(userId = "Antonio", pocketBalance = -133334),
                MemberBalance(userId = "Andres", pocketBalance = 166667),
                MemberBalance(userId = "Pepe", pocketBalance = 166666)
            )
            val result = service.simplifyByPocket(balances, "EUR")
            val pocketSettlements = result.filter { it.sourcePocket == SettlementPocketType.POCKET }

            assertEquals(2, pocketSettlements.size)
            val toAndres = pocketSettlements.first { it.toUserId == "Andres" }
            val toPepe = pocketSettlements.first { it.toUserId == "Pepe" }

            assertEquals("Antonio", toAndres.fromUserId)
            assertEquals(66667L, toAndres.amount)
            assertEquals("Antonio", toPepe.fromUserId)
            assertEquals(66667L, toPepe.amount)
            assertEquals(133334L, pocketSettlements.sumOf { it.amount })
        }

        @Test
        fun `pocket creditor and cash debtor with opposite signs produce separate settlements`() {
            val balances = listOf(
                MemberBalance(userId = "A", pocketBalance = 500, withdrawn = 0, cashSpent = 300),
                MemberBalance(userId = "B", pocketBalance = -500, withdrawn = 300, cashSpent = 0)
            )
            val result = service.simplifyByPocket(balances, "EUR")
            assertEquals(2, result.size)
            val pocketSettlement = result.find { it.sourcePocket == SettlementPocketType.POCKET }!!
            val cashSettlement = result.find { it.sourcePocket == SettlementPocketType.CASH }!!
            assertEquals("B", pocketSettlement.fromUserId)
            assertEquals("A", pocketSettlement.toUserId)
            assertEquals(500L, pocketSettlement.amount)
            assertEquals("EUR", pocketSettlement.currency)
            assertEquals("A", cashSettlement.fromUserId)
            assertEquals("B", cashSettlement.toUserId)
            assertEquals(300L, cashSettlement.amount)
            assertEquals("EUR", cashSettlement.currency)
        }

        @Test
        fun `zero pocketBalance for all members produces only cash settlements`() {
            val balances = listOf(
                MemberBalance(userId = "A", pocketBalance = 0, withdrawn = 0, cashSpent = 300),
                MemberBalance(userId = "B", pocketBalance = 0, withdrawn = 300, cashSpent = 0)
            )
            val result = service.simplifyByPocket(balances, "EUR")
            assertEquals(1, result.size)
            assertEquals(SettlementPocketType.CASH, result[0].sourcePocket)
            assertEquals("A", result[0].fromUserId)
            assertEquals("B", result[0].toUserId)
            assertEquals(300L, result[0].amount)
        }

        @Test
        fun `zero cashInHand for all members produces only pocket settlements`() {
            val balances = listOf(
                MemberBalance(userId = "A", pocketBalance = 500, withdrawn = 0, cashSpent = 0),
                MemberBalance(userId = "B", pocketBalance = -500, withdrawn = 0, cashSpent = 0)
            )
            val result = service.simplifyByPocket(balances, "EUR")
            assertEquals(1, result.size)
            assertEquals(SettlementPocketType.POCKET, result[0].sourcePocket)
            assertEquals("B", result[0].fromUserId)
            assertEquals("A", result[0].toUserId)
            assertEquals(500L, result[0].amount)
        }

        @Test
        fun `single-currency group with empty cashByCurrency uses scalar withdrawn and cashSpent`() {
            val balances = listOf(
                MemberBalance(userId = "A", pocketBalance = 0, withdrawn = 0, cashSpent = 300),
                MemberBalance(userId = "B", pocketBalance = 0, withdrawn = 300, cashSpent = 0)
            )
            val result = service.simplifyByPocket(balances, "EUR")
            val cashSettlements = result.filter { it.sourcePocket == SettlementPocketType.CASH }
            assertEquals(1, cashSettlements.size)
            assertEquals("A", cashSettlements[0].fromUserId)
            assertEquals("B", cashSettlements[0].toUserId)
            assertEquals(300L, cashSettlements[0].amount)
            assertEquals("EUR", cashSettlements[0].currency)
        }

        @Test
        fun `multi-currency cash produces one settlement set per currency`() {
            val balances = listOf(
                MemberBalance(
                    userId = "A",
                    pocketBalance = 0,
                    withdrawnByCurrency = listOf(CurrencyAmount("THB", 0, 0)),
                    cashSpentByCurrency = listOf(CurrencyAmount("THB", 5000, 0))
                ),
                MemberBalance(
                    userId = "B",
                    pocketBalance = 0,
                    withdrawnByCurrency = listOf(
                        CurrencyAmount("THB", 5000, 0),
                        CurrencyAmount("USD", 100, 0)
                    ),
                    cashSpentByCurrency = emptyList()
                ),
                MemberBalance(
                    userId = "C",
                    pocketBalance = 0,
                    withdrawnByCurrency = listOf(CurrencyAmount("USD", 0, 0)),
                    cashSpentByCurrency = listOf(CurrencyAmount("USD", 100, 0))
                )
            )
            val result = service.simplifyByPocket(balances, "EUR")
            val cashSettlements = result.filter { it.sourcePocket == SettlementPocketType.CASH }
            assertEquals(2, cashSettlements.size)
            val thbSettlement = cashSettlements.find { it.currency == "THB" }!!
            val usdSettlement = cashSettlements.find { it.currency == "USD" }!!
            assertEquals("A", thbSettlement.fromUserId)
            assertEquals("B", thbSettlement.toUserId)
            assertEquals(5000L, thbSettlement.amount)
            assertEquals("C", usdSettlement.fromUserId)
            assertEquals("B", usdSettlement.toUserId)
            assertEquals(100L, usdSettlement.amount)
        }

        @Test
        fun `group cash pool overspending generates CASH settlement from cash holder to overspender`() {
            val balances = listOf(
                MemberBalance(userId = "Andres", withdrawn = 500, cashSpent = 800),
                MemberBalance(userId = "Antonio", withdrawn = 500, cashSpent = 0)
            )
            val result = service.simplifyByPocket(balances, "EUR")
            val cashSettlement = result.find { it.sourcePocket == SettlementPocketType.CASH }!!
            assertEquals("Andres", cashSettlement.fromUserId)
            assertEquals("Antonio", cashSettlement.toUserId)
            assertEquals(300L, cashSettlement.amount)
            assertEquals("EUR", cashSettlement.currency)
        }

        @Test
        fun `group cash pool overspending is distributed proportionally among members with remaining cash shares`() {
            val balances = listOf(
                MemberBalance(userId = "Antonio", withdrawn = 166667, cashSpent = 300000),
                MemberBalance(userId = "Andres", withdrawn = 166667, cashSpent = 0),
                MemberBalance(userId = "Pepe", withdrawn = 166666, cashSpent = 0)
            )
            val result = service.simplifyByPocket(balances, "EUR")
            val cashSettlements = result.filter { it.sourcePocket == SettlementPocketType.CASH }
            assertEquals(2, cashSettlements.size)

            val antonioToAndres = cashSettlements.find { it.toUserId == "Andres" }!!
            assertEquals("Antonio", antonioToAndres.fromUserId)
            assertEquals(66667L, antonioToAndres.amount)

            val antonioToPepe = cashSettlements.find { it.toUserId == "Pepe" }!!
            assertEquals("Antonio", antonioToPepe.fromUserId)
            assertEquals(66666L, antonioToPepe.amount)
        }

        @Test
        fun `equal group cash pool distribution and zero spending produces zero CASH settlements`() {
            val balances = listOf(
                MemberBalance(userId = "Andres", withdrawn = 500, cashSpent = 0),
                MemberBalance(userId = "Antonio", withdrawn = 500, cashSpent = 0)
            )
            val result = service.simplifyByPocket(balances, "EUR")
            val cashSettlements = result.filter { it.sourcePocket == SettlementPocketType.CASH }
            assertTrue(cashSettlements.isEmpty())
        }

        @Test
        fun `USER scope cash withdrawal spent by another member generates CASH settlement`() {
            val balances = listOf(
                MemberBalance(userId = "Andres", withdrawn = 1000, cashSpent = 0),
                MemberBalance(userId = "Antonio", withdrawn = 0, cashSpent = 400)
            )
            val result = service.simplifyByPocket(balances, "EUR")
            val cashSettlement = result.find { it.sourcePocket == SettlementPocketType.CASH }!!
            assertEquals("Antonio", cashSettlement.fromUserId)
            assertEquals("Andres", cashSettlement.toUserId)
            assertEquals(400L, cashSettlement.amount)
            assertEquals("EUR", cashSettlement.currency)
        }

        @Test
        fun `multi-currency cash overspending simplifies independently per currency`() {
            val balances = listOf(
                MemberBalance(
                    userId = "A",
                    withdrawnByCurrency = listOf(
                        CurrencyAmount("THB", 5000, 0),
                        CurrencyAmount("USD", 100, 0)
                    ),
                    cashSpentByCurrency = listOf(
                        CurrencyAmount("THB", 8000, 0),
                        CurrencyAmount("USD", 0, 0)
                    )
                ),
                MemberBalance(
                    userId = "B",
                    withdrawnByCurrency = listOf(
                        CurrencyAmount("THB", 5000, 0),
                        CurrencyAmount("USD", 0, 0)
                    ),
                    cashSpentByCurrency = listOf(
                        CurrencyAmount("THB", 0, 0),
                        CurrencyAmount("USD", 40, 0)
                    )
                )
            )
            val result = service.simplifyByPocket(balances, "EUR")
            val thbSettlement = result.find { it.currency == "THB" }!!
            val usdSettlement = result.find { it.currency == "USD" }!!

            assertEquals("A", thbSettlement.fromUserId)
            assertEquals("B", thbSettlement.toUserId)
            assertEquals(3000L, thbSettlement.amount)

            assertEquals("B", usdSettlement.fromUserId)
            assertEquals("A", usdSettlement.toUserId)
            assertEquals(40L, usdSettlement.amount)
        }

        @Test
        fun `net-zero totalBalance but non-zero pockets produces both pocket and cash settlements`() {
            val balances = listOf(
                MemberBalance(userId = "A", pocketBalance = 200, withdrawn = 0, cashSpent = 200, cashInHand = -200),
                MemberBalance(userId = "B", pocketBalance = -200, withdrawn = 200, cashSpent = 0, cashInHand = 200)
            )
            val netResult = service.simplify(balances)
            assertTrue(netResult.isEmpty())
            val byPocketResult = service.simplifyByPocket(balances, "EUR")
            assertEquals(2, byPocketResult.size)
            assertTrue(byPocketResult.any { it.sourcePocket == SettlementPocketType.POCKET })
            assertTrue(byPocketResult.any { it.sourcePocket == SettlementPocketType.CASH })
        }

        @Test
        fun `creditor spending within allowance does not shift debt assigned to them`() {
            // Antonio overspent 133333
            // Andres spent 0 (capacity 166667)
            // Pepe spent 0 (capacity 166666)
            val balancesBefore = listOf(
                MemberBalance(userId = "Antonio", withdrawn = 166667, cashSpent = 300000),
                MemberBalance(userId = "Andres", withdrawn = 166667, cashSpent = 0),
                MemberBalance(userId = "Pepe", withdrawn = 166666, cashSpent = 0)
            )
            val resultBefore = service.simplifyByPocket(balancesBefore, "EUR")
                .filter { it.sourcePocket == SettlementPocketType.CASH }

            // Now Andres spends 20000, which is within his safe share.
            val balancesAfter = listOf(
                MemberBalance(userId = "Antonio", withdrawn = 166667, cashSpent = 300000),
                MemberBalance(userId = "Andres", withdrawn = 166667, cashSpent = 20000),
                MemberBalance(userId = "Pepe", withdrawn = 166666, cashSpent = 0)
            )
            val resultAfter = service.simplifyByPocket(balancesAfter, "EUR")
                .filter { it.sourcePocket == SettlementPocketType.CASH }

            assertEquals(resultBefore.size, resultAfter.size)
            resultBefore.forEachIndexed { index, expected ->
                assertEquals(expected.amount, resultAfter[index].amount)
                assertEquals(expected.fromUserId, resultAfter[index].fromUserId)
                assertEquals(expected.toUserId, resultAfter[index].toUserId)
            }
        }

        @Test
        fun `creditor spending beyond allowance spills over debt to other creditors`() {
            // Antonio overspent 133333.
            // If Andres spends 120000, his capacity is 46667. His fair share of debt was 66667.
            // Since 46667 < 66667, his assigned debt is capped at 46667, and the excess 20000 spills over to Pepe.
            val balances = listOf(
                MemberBalance(userId = "Antonio", withdrawn = 166667, cashSpent = 300000),
                MemberBalance(userId = "Andres", withdrawn = 166667, cashSpent = 120000),
                MemberBalance(userId = "Pepe", withdrawn = 166666, cashSpent = 0)
            )
            val result = service.simplifyByPocket(balances, "EUR")
                .filter { it.sourcePocket == SettlementPocketType.CASH }

            val antonioToAndres = result.find { it.toUserId == "Andres" }!!
            assertEquals("Antonio", antonioToAndres.fromUserId)
            assertEquals(46667L, antonioToAndres.amount) // Capped at capacity (166667 - 120000)

            val antonioToPepe = result.find { it.toUserId == "Pepe" }!!
            assertEquals("Antonio", antonioToPepe.fromUserId)
            assertEquals(86666L, antonioToPepe.amount) // 66666 original + 20000 spilled over
        }

        @Test
        fun `all balances zero returns empty list`() {
            val balances = listOf(
                MemberBalance(userId = "A", pocketBalance = 0, cashInHand = 0),
                MemberBalance(userId = "B", pocketBalance = 0, cashInHand = 0)
            )
            val result = service.simplifyByPocket(balances, "EUR")
            assertTrue(result.isEmpty())
        }
    }
}
