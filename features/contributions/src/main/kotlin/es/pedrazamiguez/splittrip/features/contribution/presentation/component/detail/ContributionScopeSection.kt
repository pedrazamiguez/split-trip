package es.pedrazamiguez.splittrip.features.contribution.presentation.component.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedClickable
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.BasketUp
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChevronRight
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CreditCardPay
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SectionCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.features.contribution.R
import es.pedrazamiguez.splittrip.features.contribution.presentation.model.ContributionDetailUiModel

private data class ScopeDetailRowItem(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val onClick: (() -> Unit)? = null
)

@Composable
internal fun ContributionScopeSection(
    contribution: ContributionDetailUiModel,
    modifier: Modifier = Modifier,
    onLinkedExpenseClick: (String) -> Unit = {}
) {
    val rows = buildScopeDetailRows(
        contribution = contribution,
        linkedExpenseTitle = stringResource(R.string.contribution_detail_linked_expense_title),
        linkedExpenseDesc = stringResource(R.string.contribution_detail_linked_expense_desc),
        settlementTitle = stringResource(R.string.contribution_detail_settlement_title),
        settlementDesc = stringResource(R.string.contribution_detail_settlement_desc),
        onLinkedExpenseClick = onLinkedExpenseClick
    )

    SectionCard(modifier = modifier, title = stringResource(R.string.contribution_detail_scope_section_title)) {
        rows.forEach { item ->
            val rowModifier = if (item.onClick != null) {
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                    .debouncedClickable(onClick = item.onClick)
                    .padding(vertical = MaterialTheme.spacing.ExtraSmall)
            } else {
                Modifier.fillMaxWidth()
            }
            Row(
                modifier = rowModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
            ) {
                Icon(item.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (item.description.isNotBlank()) {
                        SecondaryBodyText(text = item.description, maxLines = Int.MAX_VALUE)
                    }
                }
                if (item.onClick != null) {
                    Icon(
                        imageVector = TablerIcons.Outline.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun buildScopeDetailRows(
    contribution: ContributionDetailUiModel,
    linkedExpenseTitle: String,
    linkedExpenseDesc: String,
    settlementTitle: String,
    settlementDesc: String,
    onLinkedExpenseClick: (String) -> Unit
): List<ScopeDetailRowItem> = buildList {
    add(
        ScopeDetailRowItem(
            icon = contribution.scopeType.toIconVector(),
            title = contribution.scopeLabel,
            description = contribution.scopeDescription
        )
    )
    if (contribution.isLinkedContribution) {
        val linkedExpenseId = contribution.linkedExpenseId
        add(
            ScopeDetailRowItem(
                icon = TablerIcons.Outline.CreditCardPay,
                title = linkedExpenseTitle,
                description = linkedExpenseDesc,
                onClick = linkedExpenseId?.let { id -> { onLinkedExpenseClick(id) } }
            )
        )
    }
    if (contribution.isSettlementContribution) {
        add(
            ScopeDetailRowItem(
                icon = TablerIcons.Outline.BasketUp,
                title = settlementTitle,
                description = settlementDesc
            )
        )
    }
}
