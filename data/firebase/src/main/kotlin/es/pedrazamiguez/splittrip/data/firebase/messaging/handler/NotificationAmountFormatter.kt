package es.pedrazamiguez.splittrip.data.firebase.messaging.handler

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatCurrencyAmount

/**
 * Extracts `amountCents` and `currencyCode` from the FCM data map and formats
 * them using the app's current locale via [LocaleProvider] and [formatCurrencyAmount].
 *
 * The formatted amount's `\u00A0` (NO-BREAK SPACE) is replaced with `\u2800`
 * (BRAILLE PATTERN BLANK). Android's notification [android.widget.RemoteViews]
 * does **not** honour `\u00A0`, `\u202F`, or `\u2060` as non-breaking — the
 * text layout engine treats them all as valid line-break opportunities, causing
 * the currency symbol to detach from the value (e.g. "1,00" on one line and "€"
 * on the next). `\u2800` is classified as a **symbol** (Unicode category So),
 * not a space separator, so the text layout engine never breaks at it.
 *
 * Falls back to an empty string if the required fields are missing.
 */
fun formatNotificationAmount(data: Map<String, String>, localeProvider: LocaleProvider): String {
    val amountCents = data["amountCents"]?.toLongOrNull()
    val currencyCode = data["currencyCode"]

    if (amountCents != null && !currencyCode.isNullOrBlank()) {
        val formatted = runCatching {
            formatCurrencyAmount(
                amount = amountCents,
                currencyCode = currencyCode,
                locale = localeProvider.getCurrentLocale()
            )
        }.getOrNull()

        if (formatted != null) {
            return formatted.replace("\u00A0", "\u2800")
        }
    }

    return data["formattedAmount"]?.replace("\u00A0", "\u2800") ?: ""
}
