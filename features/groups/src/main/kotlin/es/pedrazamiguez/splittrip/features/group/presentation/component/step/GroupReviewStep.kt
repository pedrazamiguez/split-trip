package es.pedrazamiguez.splittrip.features.group.presentation.component.step

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SectionCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardReviewRow
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupUiState

/**
 * Step 4: Read-only summary of all entered data — final confirmation before creation.
 */
@Composable
fun GroupReviewStep(
    uiState: CreateEditGroupUiState,
    modifier: Modifier = Modifier
) {
    val none = stringResource(R.string.group_review_none)

    WizardStepLayout(modifier = modifier) {
        if (uiState.localGroupImagePath != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(uiState.localGroupImagePath)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(
                    R.string.group_cover_image_description,
                    uiState.groupName
                ),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(MaterialTheme.shapes.large)
            )
        }
        SectionCard(title = stringResource(R.string.group_review_title)) {
            WizardReviewRow(
                label = stringResource(R.string.group_review_name),
                value = uiState.groupName.ifBlank { none }
            )
            if (uiState.groupDescription.isNotBlank()) {
                WizardReviewRow(
                    label = stringResource(R.string.group_review_description),
                    value = uiState.groupDescription
                )
            }
            WizardReviewRow(
                label = stringResource(R.string.group_review_currency),
                value = uiState.selectedCurrency?.displayText ?: none
            )
            if (uiState.extraCurrencies.isNotEmpty()) {
                WizardReviewRow(
                    label = stringResource(R.string.group_review_extra_currencies),
                    value = uiState.extraCurrencies.joinToString { it.code }
                )
            }
            if (uiState.selectedMembers.isNotEmpty()) {
                WizardReviewRow(
                    label = stringResource(R.string.group_review_members),
                    value = uiState.selectedMembers.joinToString { it.displayName ?: it.email }
                )
            }
        }
    }
}
