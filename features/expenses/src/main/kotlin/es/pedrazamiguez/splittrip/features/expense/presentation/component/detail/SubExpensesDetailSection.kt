package es.pedrazamiguez.splittrip.features.expense.presentation.component.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Calendar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.AmountText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LabelText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SubExpenseDetailUiModel
import kotlinx.collections.immutable.ImmutableList

@Suppress("LongMethod", "CognitiveComplexMethod")
@Composable
internal fun SubExpensesDetailSection(
    subExpenses: ImmutableList<SubExpenseDetailUiModel>
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionIcon(TablerIcons.Outline.Calendar)
            LabelText(text = stringResource(R.string.expense_detail_section_sub_expenses))
        }
        FlatCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.Default),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
            ) {
                subExpenses.forEachIndexed { index, sub ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BodyText(
                                text = sub.title.ifBlank {
                                    stringResource(R.string.expense_sub_expense_number, index + 1)
                                }
                            )
                            AmountText(text = sub.formattedAmount)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = sub.paymentMethodIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                CaptionText(
                                    text = sub.paymentMethodText,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (sub.dateText.isNotBlank()) {
                                    CaptionText(
                                        text = "• ${sub.dateText}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (sub.payerText != null) {
                                    CaptionText(
                                        text = "• ${sub.payerText}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (sub.badgeText != null && sub.badgeIcon != null) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = sub.badgeIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = if (sub.isBadgeUrgent) {
                                            MaterialTheme.colorScheme.tertiary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    CaptionText(
                                        text = sub.badgeText,
                                        color = if (sub.isBadgeUrgent) {
                                            MaterialTheme.colorScheme.tertiary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            } else if (sub.formattedGroupAmount != null) {
                                CaptionText(
                                    text = sub.formattedGroupAmount,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (!sub.notesText.isNullOrBlank()) {
                            SecondaryBodyText(
                                text = sub.notesText,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
