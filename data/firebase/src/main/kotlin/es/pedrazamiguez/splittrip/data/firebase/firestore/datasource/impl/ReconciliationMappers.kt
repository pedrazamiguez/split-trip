package es.pedrazamiguez.splittrip.data.firebase.firestore.datasource.impl

import es.pedrazamiguez.splittrip.data.firebase.firestore.document.CashWithdrawalDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.ContributionDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.ExpenseDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.ExpenseSplitDocument

internal fun ExpenseDocument.getUpdatedIfNeedsUpdate(
    pendingUserId: String,
    activeUserId: String
): ExpenseDocument? {
    var needsUpdate = false
    var payerId = this.payerId
    var createdBy = this.createdBy

    if (payerId == pendingUserId) {
        payerId = activeUserId
        needsUpdate = true
    }
    if (createdBy == pendingUserId) {
        createdBy = activeUserId
        needsUpdate = true
    }

    val updatedSplits = this.splits.map { split ->
        val updatedSplit = split.getUpdatedIfNeedsUpdate(pendingUserId, activeUserId)
        if (updatedSplit != null) {
            needsUpdate = true
            updatedSplit
        } else {
            split
        }
    }

    return if (needsUpdate) {
        this.copy(
            payerId = payerId,
            createdBy = createdBy,
            splits = updatedSplits,
            serverUpdatedAt = null
        )
    } else {
        null
    }
}

internal fun ExpenseSplitDocument.getUpdatedIfNeedsUpdate(
    pendingUserId: String,
    activeUserId: String
): ExpenseSplitDocument? {
    var splitUpdated = false
    var sUserId = this.userId
    var sCoveredById = this.isCoveredById

    if (sUserId == pendingUserId) {
        sUserId = activeUserId
        splitUpdated = true
    }
    if (sCoveredById == pendingUserId) {
        sCoveredById = activeUserId
        splitUpdated = true
    }

    return if (splitUpdated) {
        this.copy(
            userId = sUserId,
            isCoveredById = sCoveredById
        )
    } else {
        null
    }
}

internal fun ContributionDocument.getUpdatedIfNeedsUpdate(
    pendingUserId: String,
    activeUserId: String
): ContributionDocument? {
    var needsUpdate = false
    var cUserId = this.userId
    var cCreatedBy = this.createdBy

    if (cUserId == pendingUserId) {
        cUserId = activeUserId
        needsUpdate = true
    }
    if (cCreatedBy == pendingUserId) {
        cCreatedBy = activeUserId
        needsUpdate = true
    }

    return if (needsUpdate) {
        this.copy(
            userId = cUserId,
            createdBy = cCreatedBy,
            serverUpdatedAt = null
        )
    } else {
        null
    }
}

internal fun CashWithdrawalDocument.getUpdatedIfNeedsUpdate(
    pendingUserId: String,
    activeUserId: String
): CashWithdrawalDocument? {
    var needsUpdate = false
    var wWithdrawnBy = this.withdrawnBy
    var wCreatedBy = this.createdBy

    if (wWithdrawnBy == pendingUserId) {
        wWithdrawnBy = activeUserId
        needsUpdate = true
    }
    if (wCreatedBy == pendingUserId) {
        wCreatedBy = activeUserId
        needsUpdate = true
    }

    return if (needsUpdate) {
        this.copy(
            withdrawnBy = wWithdrawnBy,
            createdBy = wCreatedBy,
            serverUpdatedAt = null
        )
    } else {
        null
    }
}
