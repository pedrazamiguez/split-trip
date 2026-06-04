package es.pedrazamiguez.splittrip.core.designsystem.preview

import android.content.Context
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import java.util.Locale

class PreviewLocaleProvider(private val context: Context) : LocaleProvider {

    override fun getCurrentLocale(): Locale {
        val locale = context.resources.configuration.locales[0]
        return if (locale.toLanguageTag().contains("andaluh", ignoreCase = true) ||
            locale.toLanguageTag().contains("es-rAN", ignoreCase = true)
        ) {
            Locale.forLanguageTag("es-ES")
        } else {
            locale
        }
    }
}
