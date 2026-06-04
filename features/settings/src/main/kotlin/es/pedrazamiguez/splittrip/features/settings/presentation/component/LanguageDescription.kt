package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.domain.enums.AppLanguage
import es.pedrazamiguez.splittrip.features.settings.R

@Composable
internal fun LanguageDescription(currentLanguageCode: String) {
    val languageName = when (AppLanguage.fromCode(currentLanguageCode)) {
        AppLanguage.ES -> stringResource(R.string.settings_preferences_language_es)
        AppLanguage.EN -> stringResource(R.string.settings_preferences_language_en)
        AppLanguage.ANDALUZ -> stringResource(R.string.settings_preferences_language_andaluz)
    }
    Text(text = languageName)
}
