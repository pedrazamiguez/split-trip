package es.pedrazamiguez.splittrip.features.group.presentation.model

import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class GroupUiModel(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val currency: String = "",
    /** Up to [MAX_VISIBLE_EXTRA_CURRENCIES] (or 3 when no overflow) secondary currency codes for group chips. */
    val extraCurrencies: ImmutableList<String> = persistentListOf(),
    /** Formatted overflow string (e.g. "+3") when extra currencies exceed [MAX_DIRECT_EXTRA_CURRENCIES], or null. */
    val extraCurrenciesOverflowText: String? = null,
    val membersCountText: String = "",
    val dateText: String = "",
    /** Formatted last-updated date string for the Group Detail screen. Empty if not available. */
    val lastUpdatedText: String = "",
    /** Cloud synchronization status of this group. */
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    /** Cover image URL resolved from the group's main image path. Null when no image is set. */
    val imageUrl: String? = null,
    /** Up to [MAX_VISIBLE_AVATARS] member profile-image URLs for the hero avatar stack. */
    val memberAvatarUrls: ImmutableList<String> = persistentListOf(),
    /** Number of members beyond [MAX_VISIBLE_AVATARS]; shown as "+N" overflow badge. */
    val memberOverflowCount: Int = 0,
    val members: ImmutableList<GroupMemberUiModel> = persistentListOf(),
    val status: GroupStatus = GroupStatus.ACTIVE,
    val createdBy: String = ""
) {
    companion object {
        /** Maximum number of avatar circles shown in the hero card avatar stack. */
        const val MAX_VISIBLE_AVATARS = 4

        /** Maximum number of visible extra currency chips when overflow occurs. */
        const val MAX_VISIBLE_EXTRA_CURRENCIES = 2

        /** Maximum extra currencies displayed directly without collapsing into an overflow badge. */
        const val MAX_DIRECT_EXTRA_CURRENCIES = 3
    }
}
