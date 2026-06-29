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
    val status: GroupStatus = GroupStatus.ACTIVE,
    val createdBy: String = ""
) {
    companion object {
        /** Maximum number of avatar circles shown in the hero card avatar stack. */
        const val MAX_VISIBLE_AVATARS = 4
    }
}
