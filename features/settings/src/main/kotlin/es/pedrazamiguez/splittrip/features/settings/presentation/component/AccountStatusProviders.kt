package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.AccountStatusUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.AccountStatusUiState

@Composable
internal fun AccountStatusProviders(
    uiState: AccountStatusUiState,
    onLinkGoogleClick: () -> Unit,
    onEvent: (AccountStatusUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.account_status_linked_providers),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Small))

        FlatCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(MaterialTheme.spacing.Medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
            ) {
                val isEmailLinked = uiState.linkedProviders.contains(AuthProviderType.EMAIL_PASSWORD)
                AccountProviderRow(
                    name = stringResource(R.string.account_status_provider_email_password),
                    isLinked = isEmailLinked,
                    onLinkClick = { onEvent(AccountStatusUiEvent.ShowLinkEmailDialog) },
                    onUnlinkClick = {
                        onEvent(AccountStatusUiEvent.UnlinkProvider(AuthProviderType.EMAIL_PASSWORD))
                    },
                    canUnlink = uiState.linkedProviders.size > 1,
                    isActionLoading = uiState.isLinking
                )

                val isGoogleLinked = uiState.linkedProviders.contains(AuthProviderType.GOOGLE)
                AccountProviderRow(
                    name = stringResource(R.string.account_status_provider_google),
                    isLinked = isGoogleLinked,
                    onLinkClick = onLinkGoogleClick,
                    onUnlinkClick = { onEvent(AccountStatusUiEvent.UnlinkProvider(AuthProviderType.GOOGLE)) },
                    canUnlink = uiState.linkedProviders.size > 1,
                    isActionLoading = uiState.isLinking
                )
            }
        }
    }
}
