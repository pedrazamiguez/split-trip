package es.pedrazamiguez.splittrip.domain.exception

class CannotRemoveCurrencyException(
    val currencyCode: String,
    val reason: Reason
) : Exception("Cannot remove currency $currencyCode from group: ${reason.code}") {

    enum class Reason(val code: String) {
        CURRENCY_IN_USE("currency_in_use")
    }
}
