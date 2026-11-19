package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.model.MemberBalanceEntry
import es.pedrazamiguez.splittrip.domain.model.Settlement
import es.pedrazamiguez.splittrip.domain.model.SettlementPocketType
import es.pedrazamiguez.splittrip.domain.service.CashDebtScalingService
import es.pedrazamiguez.splittrip.domain.service.DebtSimplificationService
import es.pedrazamiguez.splittrip.domain.service.PocketDebtDistributionService
import es.pedrazamiguez.splittrip.domain.service.cashdebt.CashDebtNode
import kotlin.math.min

class DebtSimplificationServiceImpl(
    private val cashDebtScalingService: CashDebtScalingService,
    private val pocketDebtDistributionService: PocketDebtDistributionService
) : DebtSimplificationService {
    override fun simplify(memberBalances: List<MemberBalance>): List<Settlement> =
        runGreedyAlgorithm(
            balances = memberBalances.map { MemberBalanceEntry(memberId = it.userId, amountCents = it.totalBalance) },
            sourcePocket = SettlementPocketType.NET,
            currency = ""
        )

    override fun simplifyByPocket(
        memberBalances: List<MemberBalance>,
        groupCurrency: String
    ): List<Settlement> =
        pocketDebtDistributionService.distributePocketDebts(
            memberBalances = memberBalances,
            groupCurrency = groupCurrency
        ) + buildCashSettlements(memberBalances, groupCurrency)

    private fun buildCashSettlements(
        memberBalances: List<MemberBalance>,
        groupCurrency: String
    ): List<Settlement> {
        val cashCurrencies = memberBalances
            .flatMap { mb ->
                mb.withdrawnByCurrency.map { it.currency } +
                    mb.cashSpentByCurrency.map { it.currency } +
                    mb.cashInHandByCurrency.map { it.currency }
            }
            .distinct()

        return if (cashCurrencies.isEmpty()) {
            val nodes = memberBalances.map { mb ->
                CashDebtNode(
                    userId = mb.userId,
                    balance = mb.withdrawn - mb.cashSpent,
                    weight = mb.withdrawn
                )
            }
            runGreedyAlgorithm(
                balances = cashDebtScalingService.scaleBalances(nodes).map {
                    MemberBalanceEntry(memberId = it.userId, amountCents = it.balance)
                },
                sourcePocket = SettlementPocketType.CASH,
                currency = groupCurrency
            )
        } else {
            cashCurrencies.flatMap { currencyCode ->
                val nodes = memberBalances.map { mb ->
                    val withdrawn = mb.withdrawnByCurrency
                        .find { it.currency == currencyCode }?.amountCents ?: 0L
                    val spent = mb.cashSpentByCurrency
                        .find { it.currency == currencyCode }?.amountCents ?: 0L
                    CashDebtNode(
                        userId = mb.userId,
                        balance = withdrawn - spent,
                        weight = withdrawn
                    )
                }
                runGreedyAlgorithm(
                    balances = cashDebtScalingService.scaleBalances(nodes).map {
                        MemberBalanceEntry(memberId = it.userId, amountCents = it.balance)
                    },
                    sourcePocket = SettlementPocketType.CASH,
                    currency = currencyCode
                )
            }
        }
    }

    private fun runGreedyAlgorithm(
        balances: List<MemberBalanceEntry>,
        sourcePocket: SettlementPocketType,
        currency: String
    ): List<Settlement> {
        val debtors = balances
            .filter { it.amountCents < 0L }
            .map { it.copy(amountCents = -it.amountCents) }
            .sortedByDescending { it.amountCents }
            .toMutableList()

        val creditors = balances
            .filter { it.amountCents > 0L }
            .sortedByDescending { it.amountCents }
            .toMutableList()

        val settlements = mutableListOf<Settlement>()
        var dIdx = 0
        var cIdx = 0

        while (dIdx < debtors.size && cIdx < creditors.size) {
            val debtor = debtors[dIdx]
            val creditor = creditors[cIdx]
            val settleAmount = min(debtor.amountCents, creditor.amountCents)

            if (settleAmount > 0L) {
                settlements += Settlement(
                    fromUserId = debtor.memberId,
                    toUserId = creditor.memberId,
                    amount = settleAmount,
                    currency = currency,
                    sourcePocket = sourcePocket
                )
            }

            debtors[dIdx] = debtor.copy(amountCents = debtor.amountCents - settleAmount)
            creditors[cIdx] = creditor.copy(amountCents = creditor.amountCents - settleAmount)

            if (debtors[dIdx].amountCents == 0L) dIdx++
            if (creditors[cIdx].amountCents == 0L) cIdx++
        }

        return settlements
    }
}
