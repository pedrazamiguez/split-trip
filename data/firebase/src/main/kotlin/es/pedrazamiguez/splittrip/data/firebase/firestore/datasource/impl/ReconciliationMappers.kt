package es.pedrazamiguez.splittrip.data.firebase.firestore.datasource.impl

import com.google.firebase.firestore.DocumentReference
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.CashWithdrawalDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.ContributionDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.ExpenseDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.ExpenseSplitDocument

internal fun ExpenseDocument.getUpdatedIfNeedsUpdate(
    pendingUserId: String,
    activeUserId: String,
    userRef: DocumentReference
): ExpenseDocument? {
    var needsUpdate = false
    var payerId = this.payerId
    var payerRef = this.payerRef
    var createdBy = this.createdBy
    var createdByRef = this.createdByRef

    if (payerId == pendingUserId) {
        payerId = activeUserId
        payerRef = userRef
        needsUpdate = true
    }
    if (createdBy == pendingUserId) {
        createdBy = activeUserId
        createdByRef = userRef
        needsUpdate = true
    }

    val updatedSplits = this.splits.map { split ->
        val updatedSplit = split.getUpdatedIfNeedsUpdate(pendingUserId, activeUserId, userRef)
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
            payerRef = payerRef,
            createdBy = createdBy,
            createdByRef = createdByRef,
            splits = updatedSplits
        )
    } else {
        null
    }
}

internal fun ExpenseSplitDocument.getUpdatedIfNeedsUpdate(
    pendingUserId: String,
    activeUserId: String,
    userRef: DocumentReference
): ExpenseSplitDocument? {
    var splitUpdated = false
    var sUserId = this.userId
    var sUserRef = this.userRef
    var sCoveredById = this.isCoveredById
    var sCoveredByRef = this.isCoveredByRef

    if (sUserId == pendingUserId) {
        sUserId = activeUserId
        sUserRef = userRef
        splitUpdated = true
    }
    if (sCoveredById == pendingUserId) {
        sCoveredById = activeUserId
        sCoveredByRef = userRef
        splitUpdated = true
    }

    return if (splitUpdated) {
        this.copy(
            userId = sUserId,
            userRef = sUserRef,
            isCoveredById = sCoveredById,
            isCoveredByRef = sCoveredByRef
        )
    } else {
        null
    }
}

internal fun ContributionDocument.getUpdatedIfNeedsUpdate(
    pendingUserId: String,
    activeUserId: String,
    userRef: DocumentReference
): ContributionDocument? {
    var needsUpdate = false
    var cUserId = this.userId
    var cCreatedBy = this.createdBy
    var cCreatedByRef = this.createdByRef

    if (cUserId == pendingUserId) {
        cUserId = activeUserId
        needsUpdate = true
    }
    if (cCreatedBy == pendingUserId) {
        cCreatedBy = activeUserId
        cCreatedByRef = userRef
        needsUpdate = true
    }

    return if (needsUpdate) {
        this.copy(
            userId = cUserId,
            createdBy = cCreatedBy,
            createdByRef = cCreatedByRef
        )
    } else {
        null
    }
}

internal fun CashWithdrawalDocument.getUpdatedIfNeedsUpdate(
    pendingUserId: String,
    activeUserId: String,
    userRef: DocumentReference
): CashWithdrawalDocument? {
    var needsUpdate = false
    var wWithdrawnBy = this.withdrawnBy
    var wCreatedBy = this.createdBy
    var wCreatedByRef = this.createdByRef

    if (wWithdrawnBy == pendingUserId) {
        wWithdrawnBy = activeUserId
        needsUpdate = true
    }
    if (wCreatedBy == pendingUserId) {
        wCreatedBy = activeUserId
        wCreatedByRef = userRef
        needsUpdate = true
    }

    return if (needsUpdate) {
        this.copy(
            withdrawnBy = wWithdrawnBy,
            createdBy = wCreatedBy,
            createdByRef = wCreatedByRef
        )
    } else {
        null
    }
}
