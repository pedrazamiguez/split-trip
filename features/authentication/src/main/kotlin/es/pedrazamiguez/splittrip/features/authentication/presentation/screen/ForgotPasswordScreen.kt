package es.pedrazamiguez.splittrip.features.authentication.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.DynamicTopAppBar
import es.pedrazamiguez.splittrip.features.authentication.R
import es.pedrazamiguez.splittrip.features.authentication.presentation.component.ForgotPasswordFormCard
import es.pedrazamiguez.splittrip.features.authentication.presentation.component.ForgotPasswordHeader
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.ForgotPasswordUiEvent
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.ForgotPasswordUiState

private const val FORM_WIDTH_FRACTION = 0.85f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    modifier: Modifier = Modifier,
    uiState: ForgotPasswordUiState,
    onEvent: (ForgotPasswordUiEvent) -> Unit = {}
) {
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f),
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.05f)
        )
    )

    Scaffold(
        topBar = {
            DynamicTopAppBar(
                title = stringResource(id = R.string.forgot_password_title),
                onBack = { if (!uiState.isLoading) onEvent(ForgotPasswordUiEvent.BackClicked) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth(FORM_WIDTH_FRACTION)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = MaterialTheme.spacing.Large)
                    .imePadding()
            ) {
                ForgotPasswordHeader(
                    modifier = Modifier.fillMaxWidth()
                )

                ForgotPasswordFormCard(
                    modifier = Modifier.fillMaxWidth(),
                    uiState = uiState,
                    onEvent = onEvent
                )
            }
        }
    }
}
