package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.SubunitOptionUiModel
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import kotlinx.collections.immutable.ImmutableList

/**
 * Reusable scope (payer type) selection card shared between the cash withdrawal
 * wizard and the add-contribution screen.
 *
 * Renders a card with a title and a radio group offering GROUP, each available
 * SUBUNIT, and USER (personal) options.
 *
 * @param labels               Localised labels for the card title and radio options.
 * @param selectedScope        Currently selected [PayerType].
 * @param selectedSubunitId    ID of the selected subunit when scope is [PayerType.SUBUNIT].
 * @param subunitOptions       Available subunit options to render as additional radio rows.
 * @param onScopeSelected      Callback emitting the chosen [PayerType] and optional subunit ID.
 */
@Composable
fun PayerTypeScopeCard(
    labels: PayerTypeScopeCardLabels,
    selectedScope: PayerType,
    selectedSubunitId: String?,
    subunitOptions: ImmutableList<SubunitOptionUiModel>,
    onScopeSelected: (PayerType, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    FlatCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.Large)) {
            Text(
                text = labels.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraSmall))
            Column(modifier = Modifier.selectableGroup()) {
                ScopeRadioRow(
                    text = labels.groupLabel,
                    selected = selectedScope == PayerType.GROUP,
                    onClick = { onScopeSelected(PayerType.GROUP, null) }
                )
                subunitOptions.forEach { option ->
                    ScopeRadioRow(
                        text = String.format(labels.subunitLabelTemplate, option.name),
                        selected = selectedScope == PayerType.SUBUNIT &&
                            selectedSubunitId == option.id,
                        onClick = { onScopeSelected(PayerType.SUBUNIT, option.id) }
                    )
                }
                ScopeRadioRow(
                    text = labels.personalLabel,
                    selected = selectedScope == PayerType.USER,
                    onClick = { onScopeSelected(PayerType.USER, null) }
                )
            }
        }
    }
}

@Composable
private fun ScopeRadioRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = MaterialTheme.spacing.ExtraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = MaterialTheme.spacing.Small)
        )
    }
}
