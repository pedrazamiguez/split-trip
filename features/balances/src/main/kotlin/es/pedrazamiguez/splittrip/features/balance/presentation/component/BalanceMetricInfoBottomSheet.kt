package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.HorizonModalBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SheetTitleText
import es.pedrazamiguez.splittrip.features.balance.presentation.model.BalanceMetricType
import es.pedrazamiguez.splittrip.features.balance.presentation.model.MetricScopeBreakdownUiModel
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BalanceMetricInfoBottomSheet(
    metricType: BalanceMetricType,
    onDismiss: () -> Unit,
    breakdown: ImmutableList<MetricScopeBreakdownUiModel>? = null,
    formattedGrandTotal: String = ""
) {
    HorizonModalBottomSheet(onDismissRequest = onDismiss) {
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
                text = stringResource(metricType.titleRes),
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.Default)
            )

            BodyText(
                text = stringResource(metricType.descriptionRes)
            )

            if (breakdown != null) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraLarge))

                MetricBreakdownContent(
                    metricType = metricType,
                    breakdown = breakdown,
                    formattedGrandTotal = formattedGrandTotal
                )
            }
        }
    }
}
