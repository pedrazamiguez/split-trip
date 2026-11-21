package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagDefaults
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.features.settings.presentation.model.DeveloperInfoUiState

private val ROLE_CORNER_RADIUS = 8.dp

@Composable
fun DeveloperHeroCard(uiState: DeveloperInfoUiState) {
    FlatCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DeveloperAvatar(avatarUrl = uiState.avatarUrl)

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.Default))

            Text(
                text = uiState.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            if (uiState.role.isNotBlank()) {
                val roleColors = PassportTagDefaults.colors(PassportTagStyle.INFO)
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraSmall))
                Surface(
                    shape = RoundedCornerShape(ROLE_CORNER_RADIUS),
                    color = roleColors.containerColor
                ) {
                    Text(
                        text = uiState.role,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = roleColors.contentColor,
                        modifier = Modifier.padding(
                            horizontal = MaterialTheme.spacing.Small,
                            vertical = MaterialTheme.spacing.ExtraSmall
                        )
                    )
                }
            }

            if (uiState.bio.isNotBlank()) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.Default))
                Text(
                    text = uiState.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
