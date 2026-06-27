package es.pedrazamiguez.splittrip.features.balance.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import java.util.Locale

internal data class ExtrasBreakdownContext(
    val groupCurrency: String,
    val memberProfiles: Map<String, User>,
    val subunitsMap: Map<String, Subunit>,
    val currentUserId: String?,
    val locale: Locale,
    val resourceProvider: ResourceProvider,
    val userUiMapper: UserUiMapper
)
