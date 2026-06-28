package es.pedrazamiguez.splittrip.features.group.presentation.mapper.impl

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.extension.resolveLocalizedName
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatDisplay
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatShortDate
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.model.Currency
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.mapper.GroupUiMapper
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupUiModel
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupUiModel.Companion.MAX_VISIBLE_AVATARS
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class GroupUiMapperImpl(
    private val localeProvider: LocaleProvider,
    private val resourceProvider: ResourceProvider
) : GroupUiMapper {

    override fun toGroupUiModel(group: Group, memberProfiles: Map<String, User>): GroupUiModel =
        with(group) {
            val currentLocale = localeProvider.getCurrentLocale()
            val memberCount = members.size

            val avatarUrls = members
                .mapNotNull { userId -> memberProfiles[userId]?.profileImagePath }
                .take(MAX_VISIBLE_AVATARS)
            // Overflow = members not represented by any avatar circle.
            // When no avatars are shown at all (nobody has a profile image),
            // overflow stays 0 — the member-count text already conveys the number.
            val overflowCount = if (avatarUrls.isEmpty()) 0 else maxOf(0, memberCount - avatarUrls.size)

            GroupUiModel(
                id = id,
                name = name,
                description = description,
                currency = currency,
                membersCountText = resourceProvider.getQuantityString(
                    R.plurals.group_members_count,
                    memberCount,
                    memberCount
                ),
                dateText = createdAt?.formatShortDate(currentLocale) ?: "",
                lastUpdatedText = lastUpdatedAt?.formatShortDate(currentLocale) ?: "",
                syncStatus = syncStatus,
                imageUrl = mainImagePath?.takeIf { it.isNotBlank() },
                memberAvatarUrls = avatarUrls.toImmutableList(),
                memberOverflowCount = overflowCount,
                status = status,
                createdBy = createdBy
            )
        }

    override fun toGroupUiModelList(
        groups: List<Group>,
        memberProfiles: Map<String, User>
    ): ImmutableList<GroupUiModel> =
        groups.map { toGroupUiModel(it, memberProfiles) }.toImmutableList()

    override fun toCurrencyUiModel(currency: Currency): CurrencyUiModel = CurrencyUiModel(
        code = currency.code,
        displayText = currency.formatDisplay(),
        decimalDigits = currency.decimalDigits,
        defaultName = currency.defaultName,
        localizedName = currency.resolveLocalizedName(resourceProvider)
    )

    override fun toCurrencyUiModels(currencies: List<Currency>): ImmutableList<CurrencyUiModel> =
        currencies.map { toCurrencyUiModel(it) }.toImmutableList()
}
