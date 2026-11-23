package es.pedrazamiguez.splittrip.features.settings.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.domain.enums.AppLanguage
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.component.SettingsSelectionRow

@Composable
fun LanguageScreen(
    availableLanguages: List<AppLanguage>,
    selectedLanguageCode: String,
    onLanguageSelected: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(MaterialTheme.spacing.Default)
    ) {
        item {
            FlatCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(MaterialTheme.spacing.ExtraSmall),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
                ) {
                    availableLanguages.forEach { language ->
                        SettingsSelectionRow(
                            title = stringResource(id = language.getDisplayNameRes()),
                            subtitle = language.englishName,
                            isSelected = language.code == selectedLanguageCode,
                            leadingBadgeText = language.toBadgeCode(),
                            onClick = { onLanguageSelected(language.code) }
                        )
                    }
                }
            }
        }
    }
}

private fun AppLanguage.getDisplayNameRes(): Int = when (this) {
    AppLanguage.EN -> R.string.settings_preferences_language_en
    AppLanguage.ES -> R.string.settings_preferences_language_es
    AppLanguage.ANDALUZ -> R.string.settings_preferences_language_andaluz
}

private fun AppLanguage.toBadgeCode(): String = when (this) {
    AppLanguage.EN -> "EN"
    AppLanguage.ES -> "ES"
    AppLanguage.ANDALUZ -> "AN"
}
