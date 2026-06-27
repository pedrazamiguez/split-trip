package es.pedrazamiguez.splittrip.core.common.util

/**
 * Stateless utility for resolving a human-readable display name for a given user ID.
 *
 * Centralises two concerns that were previously duplicated across feature mappers:
 * 1. The fallback hierarchy: displayName → email → userId.
 * 2. "You" personalisation: when [userId] matches [currentUserId] the caller-supplied
 *    [youLabel] is returned instead of the real name, keeping the UI personal.
 *
 * Features that already have `MemberOptionUiModel.isCurrentUser` can pass
 * `currentUserId = if (member.isCurrentUser) userId else null` to trigger the same logic.
 */
object DisplayNameResolver {

    /**
     * Resolves the best display name for [userId].
     *
     * @param userId        The user ID to resolve. Returns `""` immediately when `null`.
     * @param currentUserId The authenticated user's ID. When equal to [userId], [youLabel] is
     *                      returned to avoid showing the real name ("Paid by you" UX).
     * @param youLabel      Localised string for the current user (e.g. `"You"` / `"tú"`).
     * @param displayName   The target user's display name; may be `null` or blank.
     * @param email         The target user's email, used as a fallback when [displayName] is
     *                      absent. Defaults to `""`.
     * @return The resolved display name, fallen back through the priority chain, or `""` when
     *         [userId] is `null`.
     */
    fun resolve(
        userId: String?,
        currentUserId: String?,
        youLabel: String,
        displayName: String?,
        email: String = "",
        pendingLabel: String? = null
    ): String {
        if (userId == null) return ""
        if (userId == currentUserId) return youLabel

        val resolvedName = displayName?.takeIf { it.isNotBlank() }
            ?: email.takeIf { it.isNotBlank() }

        if (resolvedName != null) return resolvedName

        if (userId.startsWith("pending_") && !pendingLabel.isNullOrBlank()) {
            return pendingLabel
        }

        return userId
    }
}
