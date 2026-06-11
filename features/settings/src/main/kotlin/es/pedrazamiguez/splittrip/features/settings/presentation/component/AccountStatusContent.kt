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
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.DestructiveButton
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
            .padding(horizontal = MaterialTheme.spacing.ExtraLarge)
            .padding(top = MaterialTheme.spacing.ExtraLarge, bottom = MaterialTheme.spacing.ExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        AccountStatusHeader(
            email = uiState.email,
            joinDateText = uiState.joinDateText
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Section))

        AccountStatusProviders(
            uiState = uiState,
            onLinkGoogleClick = onLinkGoogleClick,
            onEvent = onEvent
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Section))

        DestructiveButton(
            text = stringResource(R.string.account_status_delete_account_button),
            onClick = { onEvent(AccountStatusUiEvent.ShowDeleteAccountDialog) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
