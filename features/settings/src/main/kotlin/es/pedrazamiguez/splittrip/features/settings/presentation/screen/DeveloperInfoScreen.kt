package es.pedrazamiguez.splittrip.features.settings.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.features.settings.presentation.component.DeveloperCreditsCard
import es.pedrazamiguez.splittrip.features.settings.presentation.component.DeveloperHeroCard
import es.pedrazamiguez.splittrip.features.settings.presentation.component.DeveloperLinksCard
import es.pedrazamiguez.splittrip.features.settings.presentation.model.DeveloperInfoUiState

@Composable
fun DeveloperInfoScreen(
    uiState: DeveloperInfoUiState,
    onLinkClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = MaterialTheme.spacing.ExtraLarge,
            vertical = MaterialTheme.spacing.ExtraLarge
        ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraLarge)
    ) {
        item(key = "developer_hero_card") {
            DeveloperHeroCard(uiState = uiState)
        }

        item(key = "developer_links_card") {
            DeveloperLinksCard(
                uiState = uiState,
                onLinkClick = onLinkClick
            )
        }

        if (uiState.credits.isNotBlank()) {
            item(key = "developer_credits_card") {
                DeveloperCreditsCard(credits = uiState.credits)
            }
        }

        if (uiState.copyright.isNotBlank()) {
            item(key = "developer_copyright_footer") {
                Text(
                    text = uiState.copyright,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = MaterialTheme.spacing.Small)
                )
            }
        }
    }
}
