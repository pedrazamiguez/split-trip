package es.pedrazamiguez.splittrip.features.balance.presentation.model

import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AddOnCalculationService
import java.util.Locale

internal data class HeldBreakdownContext(
    val groupCurrency: String,
    val memberProfiles: Map<String, User>,
    val subunitsMap: Map<String, Subunit>,
    val currentUserId: String?,
    val addOnCalculationService: AddOnCalculationService,
    val locale: Locale,
    val resourceProvider: ResourceProvider,
    val userUiMapper: UserUiMapper
)
