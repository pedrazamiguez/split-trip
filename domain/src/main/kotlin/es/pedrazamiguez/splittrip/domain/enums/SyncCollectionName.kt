package es.pedrazamiguez.splittrip.domain.enums

enum class SyncCollectionName(val collectionPath: String) {
    EXPENSES("expenses"),
    CONTRIBUTIONS("contributions"),
    CASH_WITHDRAWALS("cash_withdrawals"),
    CASH_TRANSFERS("cash_transfers"),
    SUBUNITS("subunits"),
    SETTLEMENTS("settlements")
}
