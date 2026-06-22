package es.pedrazamiguez.splittrip.features.onboarding.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.BrandedLoadingScreen
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.ScreenTitleText
import es.pedrazamiguez.splittrip.features.onboarding.R
import es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.state.ReconciliationUiState

@Composable
fun ReconciliationScreen(
    uiState: ReconciliationUiState,
    email: String,
    onMigrateClick: () -> Unit
) {
    // Prevent leaving the screen via back gestures
    BackHandler(enabled = true) {
        // Non-dismissible
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        when (uiState) {
            is ReconciliationUiState.Migrating -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    BrandedLoadingScreen(
                        painter = painterResource(DesignSystemR.drawable.ic_brand_logo),
                        contentDescription = stringResource(R.string.reconciliation_progress_message)
                    )
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(MaterialTheme.spacing.Large),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ScreenTitleText(
                            text = stringResource(R.string.reconciliation_title),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Medium))

                        BodyText(
                            text = stringResource(R.string.reconciliation_message, email),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.Medium)
                        )

                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraLarge))

                        GradientButton(
                            text = stringResource(R.string.reconciliation_migrate_button),
                            onClick = onMigrateClick,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
