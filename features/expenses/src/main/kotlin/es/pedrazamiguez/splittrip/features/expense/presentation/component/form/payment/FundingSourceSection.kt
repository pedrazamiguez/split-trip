package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SectionHeadingText
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.FundingSourceUiModel
import kotlinx.collections.immutable.ImmutableList

/**
 * Funding source selection using Horizon Narrative option cards.
 * Allows the user to choose between "Group Pocket" and "Your Money" with
 * always-visible plain-language descriptions for both options.
 */
@Composable
internal fun FundingSourceSection(
    fundingSources: ImmutableList<FundingSourceUiModel>,
    selectedFundingSource: FundingSourceUiModel?,
    onFundingSourceSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Default)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
        ) {
            SectionHeadingText(text = stringResource(R.string.funding_source_title))
            SecondaryBodyText(
                text = stringResource(R.string.funding_source_subtitle),
                maxLines = Int.MAX_VALUE
            )
        }

        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
        ) {
            fundingSources.forEach { source ->
                FundingSourceOptionCard(
                    option = source,
                    isSelected = source.id == selectedFundingSource?.id,
                    onClick = {
                        focusManager.clearFocus()
                        onFundingSourceSelected(source.id)
                    }
                )
            }
        }
    }
}
