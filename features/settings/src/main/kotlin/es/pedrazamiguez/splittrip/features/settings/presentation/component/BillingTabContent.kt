package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.badge.ProBadge
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.FormErrorBanner
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SectionCard
import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesUiState

@Composable
fun BillingTabContent(
    uiState: DeveloperServicesUiState,
    onEvent: (DeveloperServicesUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MaterialTheme.spacing
    val canUpdate = uiState.hasSignedInUser && !uiState.isUpdatingTier
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = spacing.ExtraLarge,
            top = spacing.ExtraLarge,
            end = spacing.ExtraLarge,
            bottom = spacing.ExtraLarge + LocalBottomPadding.current
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.ExtraLarge)
    ) {
        item {
            SectionCard(title = stringResource(R.string.developer_services_billing_section_title)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.developer_services_billing_current_tier),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.Small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.currentTier == SubscriptionTier.PRO) ProBadge()
                        Text(text = uiState.currentTier.name, style = MaterialTheme.typography.titleMedium)
                    }
                }
                SecondaryButton(
                    text = stringResource(R.string.developer_services_billing_reset_free),
                    onClick = { onEvent(DeveloperServicesUiEvent.SetSubscriptionTier(SubscriptionTier.FREE)) },
                    enabled = canUpdate && uiState.currentTier != SubscriptionTier.FREE,
                    isLoading = uiState.isUpdatingTier && uiState.pendingTargetTier == SubscriptionTier.FREE,
                    modifier = Modifier.fillMaxWidth()
                )
                GradientButton(
                    text = stringResource(R.string.developer_services_billing_set_pro),
                    onClick = { onEvent(DeveloperServicesUiEvent.SetSubscriptionTier(SubscriptionTier.PRO)) },
                    enabled = canUpdate && uiState.currentTier != SubscriptionTier.PRO,
                    isLoading = uiState.isUpdatingTier && uiState.pendingTargetTier == SubscriptionTier.PRO,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            AnimatedVisibility(
                visible = uiState.billingErrorMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                FormErrorBanner(error = uiState.billingErrorMessage)
            }
        }
    }
}
