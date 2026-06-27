package es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.common.util.DisplayNameResolver
import es.pedrazamiguez.splittrip.core.designsystem.R
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberOptionUiModel
import es.pedrazamiguez.splittrip.domain.model.User
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Presentation-layer mapper for User domain models in the core design-system.
 *
 * Provides shared formatting and resolution for User profiles.
 */
class UserUiMapper(
    private val resourceProvider: ResourceProvider
) {

    /**
     * Resolves the display name for a [User] domain object, falling back through the hierarchy:
     * displayName -> email -> userId.
     *
     * @param user          The [User] profile, or null if not resolved.
     * @param fallbackUserId The fallback user ID (used if [user] is null, or if both displayName and email are blank/null).
     * @param currentUserId The authenticated user's ID. When equal to [fallbackUserId], [youLabel] is returned if provided.
     * @param youLabel      Optional localized label for the current user (e.g., "You").
     */
    fun mapToDisplayName(
        user: User?,
        fallbackUserId: String,
        currentUserId: String? = null,
        youLabel: String = ""
    ): String {
        return DisplayNameResolver.resolve(
            userId = fallbackUserId,
            currentUserId = if (youLabel.isNotBlank()) currentUserId else null,
            youLabel = youLabel,
            displayName = user?.displayName,
            email = user?.email.orEmpty(),
            pendingLabel = resourceProvider.getString(R.string.user_pending_fallback)
        )
    }

    /**
     * Resolves the display name for a non-null [User] domain object.
     *
     * @param user          The non-null [User] profile.
     * @param currentUserId The authenticated user's ID. When equal to [user]'s userId, [youLabel] is returned if provided.
     * @param youLabel      Optional localized label for the current user (e.g., "You").
     */
    fun mapToDisplayName(
        user: User,
        currentUserId: String? = null,
        youLabel: String = ""
    ): String {
        return mapToDisplayName(
            user = user,
            fallbackUserId = user.userId,
            currentUserId = currentUserId,
            youLabel = youLabel
        )
    }

    /**
     * Maps a list of member user IDs and their profiles to [MemberOptionUiModel] items
     * for display in member pickers.
     *
     * @param memberIds     Group member user IDs.
     * @param memberProfiles Resolved profiles keyed by userId.
     * @param currentUserId The authenticated user's ID.
     */
    fun toMemberOptions(
        memberIds: List<String>,
        memberProfiles: Map<String, User>,
        currentUserId: String?
    ): ImmutableList<MemberOptionUiModel> = memberIds.map { memberId ->
        val user = memberProfiles[memberId]
        MemberOptionUiModel(
            userId = memberId,
            displayName = mapToDisplayName(
                user = user,
                fallbackUserId = memberId
            ),
            isCurrentUser = memberId == currentUserId
        )
    }.toImmutableList()
}
