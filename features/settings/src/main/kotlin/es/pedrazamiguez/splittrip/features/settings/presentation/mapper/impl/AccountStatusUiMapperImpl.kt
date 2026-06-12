package es.pedrazamiguez.splittrip.features.settings.presentation.mapper.impl

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatMediumDate
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.AccountStatusUiMapper
import java.time.LocalDateTime

class AccountStatusUiMapperImpl(
    private val localeProvider: LocaleProvider
) : AccountStatusUiMapper {

    override fun formatJoinDate(createdAt: LocalDateTime?): String {
        if (createdAt == null) return ""
        val locale = localeProvider.getCurrentLocale()
        return createdAt.formatMediumDate(locale)
    }
}
