package es.pedrazamiguez.splittrip.provider.impl

import android.content.Context
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import java.util.Locale

class LocaleProviderImpl(private val context: Context) : LocaleProvider {

    override fun getCurrentLocale(): Locale {
        val locale = context.resources.configuration.locales[0]
        val tag = locale.toLanguageTag()
        return if (tag.contains("andaluh", ignoreCase = true) ||
            tag.contains("es-rAN", ignoreCase = true) ||
            tag.contains("es-AN", ignoreCase = true)
        ) {
            Locale.forLanguageTag("es-ES")
        } else {
            locale
        }
    }
}
