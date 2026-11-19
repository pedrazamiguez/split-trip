package es.pedrazamiguez.splittrip.features.group.presentation.model

import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.model.Group
import kotlinx.collections.immutable.ImmutableList

internal data class LoadedGroupCurrencies(
    val currencies: ImmutableList<CurrencyUiModel>,
    val defaultCurrency: CurrencyUiModel?,
    val group: Group?
)
