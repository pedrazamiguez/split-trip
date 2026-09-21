package es.pedrazamiguez.splittrip.features.contribution.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.util.DisplayNameResolver
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatAmountWithCurrency
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.resolveCurrencySymbol
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberOptionUiModel
import es.pedrazamiguez.splittrip.domain.model.User
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import kotlinx.collections.immutable.ImmutableList

/**
 * Presentation-layer mapper for the Add Contribution wizard.
 *
 * Provides locale-aware formatting utilities consumed by [AddContributionViewModel].
 * Follows the **concrete-only** UiMapper pattern (no interface).
 */
class AddContributionUiMapper(
    private val localeProvider: LocaleProvider,
    private val userUiMapper: UserUiMapper
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
     * Resolves the number of decimal digits for a given ISO 4217 currency code.
     *
     * Falls back to [UiConstants.DEFAULT_MAX_DECIMAL_PLACES] when the currency code
     * is blank, invalid, or has non-standard fraction digits.
     *
     * @param currencyCode ISO 4217 code (e.g. "EUR", "USD", "JPY").
     * @return The number of decimal digits (e.g. 2 for EUR, 0 for JPY, 3 for TND).
     */
    fun resolveCurrencyDecimalDigits(currencyCode: String): Int {
        if (currencyCode.isBlank()) return UiConstants.DEFAULT_MAX_DECIMAL_PLACES
        return runCatching {
            Currency.getInstance(currencyCode).defaultFractionDigits.takeIf { it >= 0 }
        }.getOrNull() ?: UiConstants.DEFAULT_MAX_DECIMAL_PLACES
    }

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
    ): ImmutableList<MemberOptionUiModel> = userUiMapper.toMemberOptions(
        memberIds = memberIds,
        memberProfiles = memberProfiles,
        currentUserId = currentUserId
    )

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

    /**
     * Formats a given timestamp (in milliseconds) to a localized medium date + short time string.
     */
    fun formatMediumDateTime(dateMillis: Long): String {
        val localDateTime = Instant.ofEpochMilli(dateMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        return DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(localeProvider.getCurrentLocale())
            .format(localDateTime)
    }
}
