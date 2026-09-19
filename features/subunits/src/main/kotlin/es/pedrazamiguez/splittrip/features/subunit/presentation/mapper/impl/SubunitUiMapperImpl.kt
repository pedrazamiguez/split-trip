package es.pedrazamiguez.splittrip.features.subunit.presentation.mapper.impl

import es.pedrazamiguez.splittrip.core.common.enums.SelfIdentificationContextEnum
import es.pedrazamiguez.splittrip.core.common.extensions.localeAwareComparator
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatNumberForDisplay
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.subunit.R
import es.pedrazamiguez.splittrip.features.subunit.presentation.mapper.SubunitUiMapper
import es.pedrazamiguez.splittrip.features.subunit.presentation.model.MemberShareUiModel
import es.pedrazamiguez.splittrip.features.subunit.presentation.model.MemberUiModel
import es.pedrazamiguez.splittrip.features.subunit.presentation.model.SubunitUiModel
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class SubunitUiMapperImpl(
    private val localeProvider: LocaleProvider,
    private val resourceProvider: ResourceProvider,
    private val userUiMapper: UserUiMapper
) :
    SubunitUiMapper {

    override fun toSubunitUiModel(
        subunit: Subunit,
        memberProfiles: Map<String, User>,
        currentUserId: String?
    ): SubunitUiModel {
        val memberCount = subunit.memberIds.size
        val memberCountText = resourceProvider.getQuantityString(
            R.plurals.subunit_member_count,
            memberCount,
            memberCount
        )

        val memberShares = toMemberShareUiModels(subunit, memberProfiles, currentUserId)

        return SubunitUiModel(
            id = subunit.id,
            name = subunit.name,
            memberShares = memberShares,
            memberCount = memberCountText
        )
    }

    override fun toSubunitUiModelList(
        subunits: List<Subunit>,
        memberProfiles: Map<String, User>,
        currentUserId: String?
    ): ImmutableList<SubunitUiModel> = subunits.map {
        toSubunitUiModel(it, memberProfiles, currentUserId)
    }.toImmutableList()

    override fun toMemberUiModelList(
        memberIds: List<String>,
        memberProfiles: Map<String, User>,
        subunits: List<Subunit>,
        excludeSubunitId: String?,
        currentUserId: String?
    ): ImmutableList<MemberUiModel> {
        // Build a lookup: userId → subunit name (excluding the subunit being edited)
        val assignmentMap = buildMap<String, String> {
            subunits
                .filter { it.id != excludeSubunitId }
                .forEach { subunit ->
                    subunit.memberIds.forEach { memberId ->
                        put(memberId, subunit.name)
                    }
                }
        }

        return memberIds.map { userId ->
            val profile = memberProfiles[userId]
            val displayName = userUiMapper.mapToDisplayName(
                user = profile,
                fallbackUserId = userId,
                currentUserId = currentUserId,
                selfIdentificationContext = if (currentUserId !=
                    null
                ) {
                    SelfIdentificationContextEnum.NOMINATIVE
                } else {
                    null
                }
            )
            val assignedSubunitName = assignmentMap[userId]

            MemberUiModel(
                userId = userId,
                displayName = displayName,
                isAssigned = assignedSubunitName != null,
                assignedSubunitName = assignedSubunitName ?: ""
            )
        }.sortedWith(
            localeAwareComparator(localeProvider.getCurrentLocale()) { it.displayName }
        ).toImmutableList()
    }

    override fun formatShareAsPercentage(share: BigDecimal): String {
        val percentage = share.multiply(BigDecimal("100"))
        return formatPercentageForInput(percentage)
    }

    override fun formatPercentageForInput(percentage: BigDecimal): String =
        percentage.toPlainString().formatNumberForDisplay(
            locale = localeProvider.getCurrentLocale(),
            maxDecimalPlaces = 2,
            minDecimalPlaces = 0
        )

    /**
     * Builds a list of [MemberShareUiModel] pairing each member's display name
     * with their locale-formatted percentage share.
     */
    private fun toMemberShareUiModels(
        subunit: Subunit,
        memberProfiles: Map<String, User>,
        currentUserId: String?
    ): ImmutableList<MemberShareUiModel> {
        if (subunit.memberShares.isEmpty()) return emptyList<MemberShareUiModel>().toImmutableList()

        val locale = localeProvider.getCurrentLocale()
        val percentFormat = NumberFormat.getPercentInstance(locale).apply {
            maximumFractionDigits = 0
        } as DecimalFormat

        return subunit.memberIds.mapNotNull { userId ->
            val share = subunit.memberShares[userId] ?: return@mapNotNull null
            val displayName = userUiMapper.mapToDisplayName(
                user = memberProfiles[userId],
                fallbackUserId = userId,
                currentUserId = currentUserId,
                selfIdentificationContext = if (currentUserId !=
                    null
                ) {
                    SelfIdentificationContextEnum.NOMINATIVE
                } else {
                    null
                }
            )

            MemberShareUiModel(
                displayName = displayName,
                shareText = percentFormat.format(share),
                avatarUrl = memberProfiles[userId]?.profileImagePath
            )
        }.sortedWith(
            localeAwareComparator(localeProvider.getCurrentLocale()) { it.displayName }
        ).toImmutableList()
    }
}
