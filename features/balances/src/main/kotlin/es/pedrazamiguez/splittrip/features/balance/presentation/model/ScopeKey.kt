package es.pedrazamiguez.splittrip.features.balance.presentation.model

import es.pedrazamiguez.splittrip.domain.enums.PayerType

internal data class ScopeKey(
    val label: String,
    val type: PayerType,
    val id: String? = null
)

internal fun scopeComparator(currentUserId: String? = null): Comparator<ScopeKey> =
    Comparator { a, b ->
        val typeComparison = a.type.sortWeight().compareTo(b.type.sortWeight())
        if (typeComparison != 0) return@Comparator typeComparison

        val userComparison = compareUsers(a, b, currentUserId)
        if (userComparison != 0) return@Comparator userComparison

        a.label.compareTo(b.label, ignoreCase = true)
    }

private fun compareUsers(a: ScopeKey, b: ScopeKey, currentUserId: String?): Int {
    if (a.type != PayerType.USER || b.type != PayerType.USER || currentUserId == null) return 0
    val aIsCurrent = a.id == currentUserId
    val bIsCurrent = b.id == currentUserId
    return when {
        aIsCurrent && !bIsCurrent -> -1
        !aIsCurrent && bIsCurrent -> 1
        else -> 0
    }
}

private fun PayerType.sortWeight(): Int = when (this) {
    PayerType.GROUP -> 0
    PayerType.SUBUNIT -> 1
    PayerType.USER -> 2
}
