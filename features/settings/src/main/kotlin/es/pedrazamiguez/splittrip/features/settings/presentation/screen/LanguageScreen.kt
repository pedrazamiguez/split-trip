package es.pedrazamiguez.splittrip.features.settings.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Check
import es.pedrazamiguez.splittrip.domain.enums.AppLanguage
import es.pedrazamiguez.splittrip.features.settings.R

@Composable
fun LanguageScreen(
    availableLanguages: List<AppLanguage>,
    selectedLanguageCode: String,
    onLanguageSelected: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(availableLanguages) { language ->

            val isSelected = language.code == selectedLanguageCode
            val displayName = stringResource(id = language.getDisplayNameRes())

            ListItem(
                headlineContent = { Text(text = displayName) },
                supportingContent = { Text(text = language.englishName) },
                trailingContent = {
                    if (isSelected) {
                        Icon(
                            imageVector = TablerIcons.Outline.Check,
                            contentDescription = null
                        )
                    }
                },
                modifier = Modifier.clickable {
                    onLanguageSelected(language.code)
                }
            )
        }
    }
}

private fun AppLanguage.getDisplayNameRes(): Int = when (this) {
    AppLanguage.EN -> R.string.settings_preferences_language_en
    AppLanguage.ES -> R.string.settings_preferences_language_es
}
