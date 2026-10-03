package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.model.CashTransfer
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.CurrencyAmount
import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.model.Settlement
import es.pedrazamiguez.splittrip.domain.model.SettlementPocketType
import es.pedrazamiguez.splittrip.domain.model.SettlementRecord
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SettlementReconciliationServiceImpl")
class SettlementReconciliationServiceImplTest {

    private lateinit var service: SettlementReconciliationServiceImpl

    @BeforeEach
    fun setUp() {
        service = SettlementReconciliationServiceImpl()
    }

    @Nested
    @DisplayName("applyResolvedSettlements")
    inner class ApplyResolvedSettlements {

        @Test
        fun `returns original balances when no resolved settlements exist`() {
            val balances = listOf(
                MemberBalance(userId = "user-1", pocketBalance = 1000L),
                MemberBalance(userId = "user-2", pocketBalance = -1000L)
            )
            val settlements = listOf(
                SettlementRecord(
                    id = "settlement-1",
                    groupId = "group-1",
                    settlement = Settlement("user-2", "user-1", 1000L, "EUR", SettlementPocketType.POCKET),
                    status = SettlementStatus.SUGGESTED,
                    createdAt = LocalDateTime.now()
                )
            )

            val result = service.applyResolvedSettlements(
                balances,
                settlements,
                emptyList(),
                emptyList(),
                emptyList(),
                "EUR"
            )

            assertEquals(balances, result)
        }

        @Test
        fun `applies pocket settlement correctly`() {
            val balances = listOf(
                MemberBalance(
                    userId = "user-1",
                    pocketBalance = 1000L,
                    withdrawn = 0L,
                    contributed = 1000L
                ),
                MemberBalance(
                    userId = "user-2",
                    pocketBalance = -1000L,
                    withdrawn = 1000L,
                    contributed = 0L
                )
            )
            val settlements = listOf(
                SettlementRecord(
                    id = "settlement-1",
                    groupId = "group-1",
                    settlement = Settlement("user-2", "user-1", 1000L, "EUR", SettlementPocketType.POCKET),
                    status = SettlementStatus.RESOLVED,
                    createdAt = LocalDateTime.now()
                )
            )

            val result = service.applyResolvedSettlements(
                balances,
                settlements,
                emptyList(),
                emptyList(),
                emptyList(),
                "EUR"
            )
            val balanceMap = result.associateBy { it.userId }

            val user1 = balanceMap["user-1"]!!
            val user2 = balanceMap["user-2"]!!

            // user-2 pays user-1.
            // user-2 pocketBalance becomes 0. user-2 contributed increases.
            assertEquals(0L, user2.pocketBalance)
            assertEquals(1000L, user2.contributed)

            // user-1 pocketBalance becomes 0. user-1 withdrawn increases.
            assertEquals(0L, user1.pocketBalance)
            assertEquals(1000L, user1.withdrawn)
        }

        @Test
        fun `applies cash settlement correctly adjusting withdrawn and cashInHand`() {
            val balances = listOf(
                MemberBalance(
                    userId = "user-1", // Creditor: withdrew 10000, 4000 left in hand
                    withdrawn = 10000L,
                    contributed = 6000L,
                    pocketBalance = -4000L, // contributed - withdrawn - nonCashSpent = 6000 - 10000 - 0
                    cashSpent = 6000L,
                    cashInHand = 4000L,
                    cashInHandByCurrency = listOf(CurrencyAmount("EUR", 4000L, 4000L)),
                    withdrawnByCurrency = listOf(CurrencyAmount("EUR", 10000L, 10000L))
                ),
                MemberBalance(
                    userId = "user-2", // Debtor: spent 6000 cash without withdrawing
                    withdrawn = 0L,
                    contributed = 0L,
                    pocketBalance = 0L,
                    cashSpent = 6000L,
                    cashInHand = 0L,
                    cashInHandByCurrency = emptyList(),
                    withdrawnByCurrency = emptyList()
                )
            )
            val cashTransfers = listOf(
                CashTransfer(
                    id = "cash-transfer-1",
                    groupId = "group-1",
                    fromUserId = "user-2",
                    toUserId = "user-1",
                    amountCents = 4000L,
                    currency = "EUR",
                    equivalentBaseAmountCents = 4000L,
                    createdAt = System.currentTimeMillis()
                )
            )

            val result = service.applyResolvedSettlements(
                balances,
                emptyList(),
                cashTransfers,
                emptyList(),
                emptyList(),
                "EUR"
            )
            val balanceMap = result.associateBy { it.userId }

            val user1 = balanceMap["user-1"]!!
            val user2 = balanceMap["user-2"]!!

            // user-2 (debtor) pays 4000 cash to user-1.
            // withdrawn INCREASES to offset cash debt (withdrawn - cashSpent → 0)
            assertEquals(4000L, user2.withdrawn)
            // pocketBalance is UNCHANGED — cash transfers don't affect the virtual pocket
            assertEquals(0L, user2.pocketBalance)
            // cashInHand DECREASES — they gave physical cash away
            assertEquals(-4000L, user2.cashInHand)
            val user2Eur = user2.withdrawnByCurrency.find { it.currency == "EUR" }
            assertEquals(4000L, user2Eur?.amountCents)

            // user-1 (creditor) receives 4000 cash from user-2.
            // withdrawn DECREASES to offset cash surplus (withdrawn - cashSpent → 0)
            assertEquals(6000L, user1.withdrawn) // 10000 - 4000
            // pocketBalance is UNCHANGED — cash transfers don't affect the virtual pocket
            assertEquals(-4000L, user1.pocketBalance)
            // cashInHand INCREASES — they received physical cash
            assertEquals(8000L, user1.cashInHand) // 4000 + 4000
            val user1Eur = user1.withdrawnByCurrency.find { it.currency == "EUR" }
            assertEquals(6000L, user1Eur?.amountCents) // 10000 - 4000
        }

        @Test
        fun `resolved cash settlement zeroes out cash debt and prevents feedback loop`() {
            // Regression test: after reconciliation, buildCashSettlements
            // should produce ZERO new settlements for the resolved pair.
            val balances = listOf(
                MemberBalance(
                    userId = "creditor",
                    withdrawn = 300L,
                    cashSpent = 0L,
                    pocketBalance = 0L,
                    cashInHand = 300L,
                    cashInHandByCurrency = listOf(CurrencyAmount("EUR", 300L, 300L)),
                    withdrawnByCurrency = listOf(CurrencyAmount("EUR", 300L, 300L))
                ),
                MemberBalance(
                    userId = "debtor",
                    withdrawn = 0L,
                    cashSpent = 300L,
                    pocketBalance = 0L,
                    cashInHand = 0L,
                    cashInHandByCurrency = emptyList(),
                    withdrawnByCurrency = emptyList()
                )
            )
            val cashTransfers = listOf(
                CashTransfer(
                    id = "cash-transfer-1",
                    groupId = "group-1",
                    fromUserId = "debtor",
                    toUserId = "creditor",
                    amountCents = 300L,
                    currency = "EUR",
                    equivalentBaseAmountCents = 300L,
                    createdAt = System.currentTimeMillis()
                )
            )

            val reconciled = service.applyResolvedSettlements(
                balances,
                emptyList(),
                cashTransfers,
                emptyList(),
                emptyList(),
                "EUR"
            )
            val reconciledMap = reconciled.associateBy { it.userId }

            // After reconciliation, cash debt should be zero for both
            val creditorBalance = reconciledMap["creditor"]!!
            val debtorBalance = reconciledMap["debtor"]!!

            // creditor: withdrawn - cashSpent = (300-300) - 0 = 0
            assertEquals(0L, creditorBalance.withdrawn - creditorBalance.cashSpent)
            // debtor: withdrawn - cashSpent = (0+300) - 300 = 0
            assertEquals(0L, debtorBalance.withdrawn - debtorBalance.cashSpent)

            // Verify no new cash settlements would be generated
            val simplificationService = DebtSimplificationServiceImpl(
                CashDebtScalingServiceImpl(RemainderDistributionServiceImpl()),
                PocketDebtDistributionServiceImpl()
            )
            val newSettlements = simplificationService.simplifyByPocket(reconciled, "EUR")
            val cashSettlements = newSettlements.filter {
                it.sourcePocket == SettlementPocketType.CASH
            }
            assertEquals(0, cashSettlements.size, "No new cash settlements should be generated after reconciliation")
        }

        @Test
        fun `applies pocket settlement correctly when materialized via linked contribution`() {
            val balances = listOf(
                MemberBalance(
                    userId = "user-1",
                    pocketBalance = 1000L,
                    withdrawn = 0L,
                    contributed = 1000L
                ),
                MemberBalance(
                    userId = "user-2",
                    pocketBalance = -1000L,
                    withdrawn = 1000L,
                    contributed = 0L
                )
            )
            val settlements = listOf(
                SettlementRecord(
                    id = "settlement-1",
                    groupId = "group-1",
                    settlement = Settlement("user-2", "user-1", 1000L, "EUR", SettlementPocketType.POCKET),
                    status = SettlementStatus.RESOLVED,
                    createdAt = LocalDateTime.now()
                )
            )
            val contributions = listOf(
                Contribution(
                    id = "contrib-1",
                    groupId = "group-1",
                    userId = "user-2",
                    amount = 1000L,
                    currency = "EUR",
                    equivalentBaseAmount = 1000L,
                    linkedSettlementId = "settlement-1",
                    createdAt = LocalDateTime.now()
                )
            )

            val result = service.applyResolvedSettlements(
                balances,
                settlements,
                emptyList(),
                contributions,
                emptyList(),
                "EUR"
            )
            val balanceMap = result.associateBy { it.userId }

            val user1 = balanceMap["user-1"]!!
            val user2 = balanceMap["user-2"]!!

            // Neither user-1 nor user-2 balances are mutated by the reconciliation strategy,
            // since the contribution logic itself already accounted for it.
            assertEquals(-1000L, user2.pocketBalance)
            assertEquals(0L, user2.contributed)

            assertEquals(1000L, user1.pocketBalance)
            assertEquals(0L, user1.withdrawn)
        }
    }
}
