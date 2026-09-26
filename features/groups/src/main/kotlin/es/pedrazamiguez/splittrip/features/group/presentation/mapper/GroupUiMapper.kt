package es.pedrazamiguez.splittrip.features.group.presentation.mapper

import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.model.Currency
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupCurrencyRateUiModel
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupUiModel
import java.math.BigDecimal
import kotlinx.collections.immutable.ImmutableList

interface GroupUiMapper {
    /**
     * Maps a [Group] to a [GroupUiModel] without member profile enrichment.
     * Avatar fields will be empty; use the overload accepting a `memberProfiles` map for the hero card.
     */
    fun toGroupUiModel(group: Group): GroupUiModel = toGroupUiModel(group, emptyMap())

    /**
     * Maps a [Group] to a [GroupUiModel] enriched with resolved member profiles.
     * [memberProfiles] is a `userId → User` map used to populate avatar URLs.
     */
    fun toGroupUiModel(group: Group, memberProfiles: Map<String, User>): GroupUiModel

    /**
     * Maps a list of [Group]s to [GroupUiModel]s without member profile enrichment.
     */
    fun toGroupUiModelList(groups: List<Group>): ImmutableList<GroupUiModel> =
        toGroupUiModelList(groups, emptyMap())

    /**
     * Maps a list of [Group]s to [GroupUiModel]s enriched with resolved member profiles.
     */
    fun toGroupUiModelList(groups: List<Group>, memberProfiles: Map<String, User>): ImmutableList<GroupUiModel>

    fun toCurrencyUiModel(currency: Currency): CurrencyUiModel
    fun toCurrencyUiModels(currencies: List<Currency>): ImmutableList<CurrencyUiModel>

    fun mapCurrencyExchangeRates(
        baseCurrency: String,
        rates: Map<String, BigDecimal?>
    ): ImmutableList<GroupCurrencyRateUiModel>
}
