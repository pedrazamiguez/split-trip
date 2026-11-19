package es.pedrazamiguez.splittrip.features.expense.presentation.model

import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import java.time.ZoneId

data class ExpenseMappingContext(
    val memberProfiles: Map<String, User> = emptyMap(),
    val currentUserId: String? = null,
    val pairedContributions: Map<String, Contribution> = emptyMap(),
    val subunits: Map<String, Subunit> = emptyMap(),
    val groupMemberIds: List<String> = emptyList(),
    val zoneId: ZoneId = ZoneId.systemDefault()
)
