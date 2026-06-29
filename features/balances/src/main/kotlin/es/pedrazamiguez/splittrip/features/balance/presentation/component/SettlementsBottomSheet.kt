package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SheetTitleText
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SettlementUiModel
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementsBottomSheet(
    settlements: ImmutableList<SettlementUiModel>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = MaterialTheme.spacing.Large,
                    end = MaterialTheme.spacing.Large,
                    top = MaterialTheme.spacing.ExtraLarge,
                    bottom = MaterialTheme.spacing.Section
                )
        ) {
            SheetTitleText(
                text = stringResource(R.string.balances_settlements_title)
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.Small))

            BodyText(
                text = stringResource(R.string.balances_settlements_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.Large))

            if (settlements.isEmpty()) {
                BodyText(
                    text = stringResource(R.string.balances_settlements_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
                ) {
                    settlements.forEach { settlement ->
                        SettlementItem(settlement = settlement)
                    }
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraLarge))

            GradientButton(
                text = stringResource(R.string.balances_settlements_dismiss),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
