package es.pedrazamiguez.splittrip.features.expense.presentation.component.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportChip
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberOptionUiModel
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.category.filterChipLeadingIcon
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemberFilterSection(
    availableMembers: ImmutableList<MemberOptionUiModel>,
    criteria: ExpenseFilterCriteria,
    onCriteriaChange: (ExpenseFilterCriteria) -> Unit,
    modifier: Modifier = Modifier
) {
    if (availableMembers.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        FilterSectionHeader(
            title = stringResource(R.string.expenses_filter_section_members),
            selectedCount = criteria.selectedMemberIds.size
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            availableMembers.forEach { member ->
                val isSelected = member.userId in criteria.selectedMemberIds
                PassportChip(
                    label = member.displayName,
                    selected = isSelected,
                    onClick = {
                        onCriteriaChange(toggleMember(criteria, member.userId))
                    },
                    leadingIcon = filterChipLeadingIcon(isSelected = isSelected)
                )
            }
        }
    }
}

private fun toggleMember(
    criteria: ExpenseFilterCriteria,
    memberId: String
): ExpenseFilterCriteria {
    val isSelected = memberId in criteria.selectedMemberIds
    val updatedMemberIds = if (isSelected) {
        criteria.selectedMemberIds - memberId
    } else {
        criteria.selectedMemberIds + memberId
    }
    return criteria.copy(selectedMemberIds = updatedMemberIds)
}
