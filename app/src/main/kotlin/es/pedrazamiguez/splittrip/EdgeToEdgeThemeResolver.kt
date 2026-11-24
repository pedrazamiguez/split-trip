package es.pedrazamiguez.splittrip

import es.pedrazamiguez.splittrip.domain.enums.AppTheme

internal fun resolveDarkTheme(themeCode: String?, isSystemDark: Boolean): Boolean =
    when (AppTheme.fromCode(themeCode)) {
        AppTheme.SYSTEM -> isSystemDark
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }
