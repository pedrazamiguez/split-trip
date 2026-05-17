package es.pedrazamiguez.splittrip.features.contribution.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.util.DisplayNameResolver
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatAmountWithCurrency
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.resolveCurrencySymbol
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberOptionUiModel
import es.pedrazamiguez.splittrip.domain.model.User
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Presentation-layer mapper for the Add Contribution wizard.
 *
 * Provides locale-aware formatting utilities consumed by [AddContributionViewModel].
 * Follows the **concrete-only** UiMapper pattern (no interface).
 */
class AddContributionUiMapper(
    private val localeProvider: LocaleProvider
) {

    /**
     * Formats a raw user-entered amount string with currency symbol and locale formatting.
     *
     * Delegates to the design-system [formatAmountWithCurrency] utility.
     */
    fun formatInputAmountWithCurrency(amountInput: String, currencyCode: String): String =
        formatAmountWithCurrency(amountInput, currencyCode, localeProvider.getCurrentLocale())

    /**
     * Resolves the currency symbol for a given ISO 4217 currency code.
     *
     * Delegates to the design-system [resolveCurrencySymbol] utility which
     * falls back to the currency's native locale when the user's locale
     * returns the ISO code (e.g. "INR" instead of "₹").
     *
     * @return The human-readable symbol (e.g. "€", "₹", "US$"), or an empty
     *         string if the code is blank or unresolvable.
     */
    fun resolveCurrencySymbol(currencyCode: String): String =
        resolveCurrencySymbol(currencyCode, localeProvider.getCurrentLocale())

    /**
     * Maps a list of member user IDs and their profiles to [MemberOptionUiModel] items
     * for display in the member picker.
     *
     * @param memberIds     Group member user IDs.
     * @param memberProfiles Resolved profiles keyed by userId.
     * @param currentUserId The authenticated user's ID (used to set [MemberOptionUiModel.isCurrentUser]).
     */
    fun toMemberOptions(
        memberIds: List<String>,
        memberProfiles: Map<String, User>,
        currentUserId: String?
    ): ImmutableList<MemberOptionUiModel> = memberIds.map { memberId ->
        MemberOptionUiModel(
            userId = memberId,
            displayName = memberProfiles[memberId]?.displayName?.takeIf { it.isNotBlank() }
                ?: memberId,
            isCurrentUser = memberId == currentUserId
        )
    }.toImmutableList()

    /**
     * Looks up the display name for a given userId from a pre-mapped member list.
     *
     * Returns [youLabel] when the member has [MemberOptionUiModel.isCurrentUser] set to `true`,
     * delegating "you" personalisation to [DisplayNameResolver].
     *
     * @return The member's display name or [youLabel] for the current user; `""` if not found.
     */
    fun resolveDisplayName(
        userId: String?,
        members: ImmutableList<MemberOptionUiModel>,
        youLabel: String = ""
    ): String {
        if (userId == null) return ""
        val member = members.firstOrNull { it.userId == userId } ?: return ""
        // When no youLabel is provided, skip "you" personalisation and return the display
        // name directly — avoids blank labels at call sites that don't supply the string.
        if (youLabel.isBlank()) return member.displayName
        return DisplayNameResolver.resolve(
            userId = userId,
            currentUserId = if (member.isCurrentUser) userId else null,
            youLabel = youLabel,
            displayName = member.displayName
        )
    }
}
