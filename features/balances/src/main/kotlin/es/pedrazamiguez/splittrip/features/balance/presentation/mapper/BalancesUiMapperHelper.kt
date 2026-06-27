package es.pedrazamiguez.splittrip.features.balance.presentation.mapper

import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Subunit
import java.math.BigDecimal
import java.math.RoundingMode

/** Computes the user's attributed native share (in withdrawal currency cents) for display. */
internal fun computeUserNativeShare(
    withdrawal: CashWithdrawal,
    userId: String,
    groupMemberIds: List<String>,
    subunitsMap: Map<String, Subunit>
): Long {
    val remaining = withdrawal.remainingAmount
    return when (withdrawal.withdrawalScope) {
        PayerType.USER -> if (withdrawal.withdrawnBy == userId) remaining else 0L
        PayerType.GROUP -> {
            if (groupMemberIds.isEmpty()) 0L else remaining / groupMemberIds.size
        }
        PayerType.SUBUNIT -> {
            val subunit = withdrawal.subunitId?.let { subunitsMap[it] }
            val share = subunit?.memberShares?.get(userId) ?: return 0L
            BigDecimal(remaining)
                .multiply(share)
                .setScale(0, RoundingMode.HALF_UP)
                .toLong()
        }
    }
}

/** Computes the total scaled fee cents for a member over all withdrawals. */
internal fun computeMemberTotalFees(
    userId: String,
    withdrawals: List<CashWithdrawal>,
    groupMemberIds: List<String>,
    subunitsMap: Map<String, Subunit>
): Long {
    return withdrawals.sumOf { withdrawal ->
        if (withdrawal.amountWithdrawn == 0L || withdrawal.remainingAmount <= 0L) return@sumOf 0L
        val nativeShare = computeUserNativeShare(withdrawal, userId, groupMemberIds, subunitsMap)
        if (nativeShare > 0L) {
            val subunit = withdrawal.subunitId?.let { subunitsMap[it] }
            withdrawal.addOns
                .filter { it.type != AddOnType.DISCOUNT }
                .sumOf { addOn ->
                    computeAddOnShare(
                        addOn = addOn,
                        scope = withdrawal.withdrawalScope,
                        withdrawnBy = withdrawal.withdrawnBy,
                        userId = userId,
                        groupMemberIds = groupMemberIds,
                        subunit = subunit
                    )
                }
        } else {
            0L
        }
    }
}

/** Helper to compute a single add-on's share for a member. */
internal fun computeAddOnShare(
    addOn: AddOn,
    scope: PayerType,
    withdrawnBy: String,
    userId: String,
    groupMemberIds: List<String>,
    subunit: Subunit?
): Long {
    return when (scope) {
        PayerType.USER -> if (withdrawnBy == userId) addOn.groupAmountCents else 0L
        PayerType.GROUP -> if (groupMemberIds.isEmpty()) 0L else addOn.groupAmountCents / groupMemberIds.size
        PayerType.SUBUNIT -> {
            val share = subunit?.memberShares?.get(userId) ?: BigDecimal.ZERO
            BigDecimal(addOn.groupAmountCents)
                .multiply(share)
                .setScale(0, RoundingMode.HALF_UP)
                .toLong()
        }
    }
}
