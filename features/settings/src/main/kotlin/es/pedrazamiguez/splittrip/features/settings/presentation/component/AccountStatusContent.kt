package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Trash
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.DestructiveButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SectionHeadingText
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.AccountStatusUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.AccountStatusUiState

@Composable
internal fun AccountStatusContent(
    uiState: AccountStatusUiState,
    onLinkGoogleClick: () -> Unit,
    onEvent: (AccountStatusUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(MaterialTheme.spacing.Default),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        AccountStatusHeader(
            email = uiState.email,
            joinDateText = uiState.joinDateText,
            isAnonymous = uiState.isAnonymous
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Section))

        AccountStatusProviders(
            uiState = uiState,
            onLinkGoogleClick = onLinkGoogleClick,
            onEvent = onEvent
        )

        if (!uiState.isAnonymous) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.Section))

            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeadingText(
                    text = stringResource(R.string.account_status_delete_account_section_title)
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.Small))
                FlatCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.Default),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Default)
                    ) {
                        SecondaryBodyText(
                            text = stringResource(R.string.account_status_delete_account_section_description),
                            maxLines = Int.MAX_VALUE
                        )
                        DestructiveButton(
                            text = stringResource(R.string.account_status_delete_account_button),
                            leadingIcon = TablerIcons.Outline.Trash,
                            onClick = { onEvent(AccountStatusUiEvent.ShowDeleteAccountDialog) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
