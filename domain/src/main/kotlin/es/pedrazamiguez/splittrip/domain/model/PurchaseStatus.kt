package es.pedrazamiguez.splittrip.domain.model

sealed interface PurchaseStatus {
    data class Success(val productId: String, val purchaseToken: String) : PurchaseStatus
    data object Pending : PurchaseStatus
    data object UserCanceled : PurchaseStatus
    data object AlreadyOwned : PurchaseStatus
    data class Error(val message: String? = null) : PurchaseStatus
}
