package es.pedrazamiguez.splittrip.features.expense.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CardTitleText
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.ExpenseStepInfoItem

@Composable
internal fun ExpenseStepInfoItemRow(
    item: ExpenseStepInfoItem,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
    ) {
        CardTitleText(
            text = stringResource(item.titleRes),
            maxLines = Int.MAX_VALUE
        )
        BodyText(
            text = stringResource(item.descriptionRes),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
