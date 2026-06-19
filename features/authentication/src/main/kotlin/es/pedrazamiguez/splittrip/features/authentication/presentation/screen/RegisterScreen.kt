package es.pedrazamiguez.splittrip.features.authentication.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.FormErrorBanner
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.features.authentication.R
import es.pedrazamiguez.splittrip.features.authentication.presentation.component.RegisterFooter
import es.pedrazamiguez.splittrip.features.authentication.presentation.component.RegisterFormFields
import es.pedrazamiguez.splittrip.features.authentication.presentation.component.RegisterHeader
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.RegisterUiEvent
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.RegisterUiState

private const val FORM_WIDTH_FRACTION = 0.85f

@Composable
fun RegisterScreen(
    modifier: Modifier = Modifier,
    uiState: RegisterUiState,
    onEvent: (RegisterUiEvent) -> Unit = {},
    onLoginClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val bgGradient = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f),
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.05f)
        )
    )

    Scaffold { innerPadding ->
        Box(
            modifier = modifier.fillMaxSize().background(bgGradient).padding(innerPadding),
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
                RegisterHeader()
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.Section))
                FlatCard(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(MaterialTheme.spacing.Large),
                        verticalArrangement = Arrangement.spacedBy(
                            MaterialTheme.spacing.Default
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RegisterFormFields(uiState = uiState, anyLoading = uiState.isLoading, onEvent = onEvent)
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraSmall))
                        GradientButton(
                            text = stringResource(R.string.register_submit_button),
                            onClick = { onEvent(RegisterUiEvent.SubmitSignUp) },
                            enabled = !uiState.isLoading,
                            isLoading = uiState.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        )
                        FormErrorBanner(error = uiState.error)
                    }
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.Large))
                RegisterFooter(onLoginClick = onLoginClick)
            }
        }
    }

    BackHandler { onBackClick() }
}
