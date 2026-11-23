package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Mail
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SectionHeadingText
import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.AccountStatusUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.AccountStatusUiState

private const val DIVIDER_ALPHA = 0.5f

@Composable
internal fun AccountStatusProviders(
    uiState: AccountStatusUiState,
    onLinkGoogleClick: () -> Unit,
    onEvent: (AccountStatusUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeadingText(
            text = stringResource(R.string.account_status_linked_providers)
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Small))

        FlatCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(MaterialTheme.spacing.Default),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Default)
            ) {
                val isEmailLinked = uiState.linkedProviders.contains(AuthProviderType.EMAIL_PASSWORD)
                AccountProviderRow(
                    name = stringResource(R.string.account_status_provider_email_password),
                    icon = TablerIcons.Outline.Mail,
                    isLinked = isEmailLinked,
                    onLinkClick = { onEvent(AccountStatusUiEvent.ShowLinkEmailSheet) },
                    onUnlinkClick = {
                        onEvent(AccountStatusUiEvent.UnlinkProvider(AuthProviderType.EMAIL_PASSWORD))
                    },
                    canUnlink = uiState.linkedProviders.size > 1,
                    isActionLoading = uiState.isLinking
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DIVIDER_ALPHA)
                )

                val isGoogleLinked = uiState.linkedProviders.contains(AuthProviderType.GOOGLE)
                AccountProviderRow(
                    name = stringResource(R.string.account_status_provider_google),
                    icon = ImageVector.vectorResource(id = DesignSystemR.drawable.ic_google_logo),
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
