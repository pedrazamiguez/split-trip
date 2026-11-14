package es.pedrazamiguez.splittrip.domain.constant

object BillingConstants {
    const val PRODUCT_ID_PRO_MONTHLY = "split_trip_pro_monthly"
    const val PRODUCT_ID_PRO_ANNUAL = "split_trip_pro_annual"
    const val BILLING_CONNECT_TIMEOUT_MS = 6_000L
    val PRO_PRODUCT_IDS: Set<String> = setOf(PRODUCT_ID_PRO_MONTHLY, PRODUCT_ID_PRO_ANNUAL)
}
