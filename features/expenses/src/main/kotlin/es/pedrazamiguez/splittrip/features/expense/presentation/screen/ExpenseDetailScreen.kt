package es.pedrazamiguez.splittrip.features.expense.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.AlignJustified
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Calendar
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Coin
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CreditCard
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Scale
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Wallet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SyncStatusIndicator
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LabelText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.features.expense.presentation.model.AddOnDetailUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.CashTrancheDetailUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseDetailUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitDetailUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.ExpenseDetailUiState
import kotlinx.collections.immutable.ImmutableList

private val SECTION_ICON_SIZE = 16.dp
private val CATEGORY_ICON_SIZE = 24.dp
private val CATEGORY_ICON_CONTAINER_SIZE = 48.dp

@Composable
fun ExpenseDetailScreen(
    uiState: ExpenseDetailUiState = ExpenseDetailUiState(),
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> ShimmerLoadingList()
        uiState.hasError || uiState.expense == null -> {
            EmptyStateView(
                title = stringResource(R.string.expense_detail_error_loading),
                icon = TablerIcons.Outline.Receipt
            )
        }
        else -> ExpenseDetailContent(expense = uiState.expense, modifier = modifier)
    }
}

@Composable
private fun ExpenseDetailContent(
    expense: ExpenseDetailUiModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.spacing.Default),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Small))

        HeroSection(expense = expense)

        GeneralInfoSection(expense = expense)

        if (expense.notesText != null) {
            NotesSection(notesText = expense.notesText)
        }

        if (expense.isForeignCurrency) {
            CurrencyDetailsSection(expense = expense)
        }

        if (expense.cashTranches.isNotEmpty()) {
            CashTranchesSection(tranches = expense.cashTranches, sourceCurrency = expense.sourceCurrency)
        }

        SplitBreakdownSection(
            splitTypeText = expense.splitTypeText,
            splits = expense.splits
        )

        if (expense.hasAddOns) {
            AddOnsSection(
                addOns = expense.addOns,
                formattedEffectiveTotal = expense.formattedEffectiveTotal
            )
        }

        ProvenanceSection(expense = expense)

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraLarge))
    }
}

@Composable
private fun HeroSection(expense: ExpenseDetailUiModel) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)) {
        // Category icon + title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Default)
        ) {
            Box(
                modifier = Modifier
                    .size(CATEGORY_ICON_CONTAINER_SIZE)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = expense.category.toIconVector(),
                    contentDescription = null,
                    modifier = Modifier.size(CATEGORY_ICON_SIZE),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                text = expense.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Status chips row
        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusChip(text = expense.categoryText, isHighlighted = true)
            StatusChip(text = expense.paymentStatusText)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = TablerIcons.Outline.Calendar,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CaptionText(
                    text = expense.dateText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Amount hero card
        FlatCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.Default)) {
                CaptionText(
                    text = stringResource(R.string.expense_review_amount).uppercase(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = expense.formattedGroupAmount,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 40.sp
                )
                if (expense.isForeignCurrency && expense.formattedSourceAmount != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CaptionText(
                            text = stringResource(R.string.expense_detail_group_currency_label),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            BodyText(
                                text = expense.formattedSourceAmount,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (expense.formattedExchangeRate != null) {
                                CaptionText(
                                    text = stringResource(
                                        R.string.expense_detail_rate_label,
                                        expense.formattedExchangeRate
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneralInfoSection(expense: ExpenseDetailUiModel) {
    FlatCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Default),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
        ) {
            DetailSectionHeader(
                label = stringResource(R.string.expense_detail_section_info),
                icon = { SectionIcon(TablerIcons.Outline.CreditCard) }
            )
            DetailRow(
                label = stringResource(R.string.expense_review_payment_method),
                value = expense.paymentMethodText
            )
            DetailRow(label = stringResource(R.string.expense_detail_paid_by_label), value = expense.paidByText)
            if (expense.vendorText != null) {
                DetailRow(
                    label = stringResource(R.string.expense_review_vendor),
                    value = expense.vendorText
                )
            }
            if (expense.isOutOfPocket && expense.fundingSourceText != null) {
                DetailRow(
                    label = stringResource(R.string.expense_review_funding_source),
                    value = expense.fundingSourceText
                )
            }
        }
    }
}

@Composable
private fun NotesSection(notesText: String) {
    FlatCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Default),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            DetailSectionHeader(
                label = stringResource(R.string.expense_detail_section_notes),
                icon = { SectionIcon(TablerIcons.Outline.AlignJustified) }
            )
            BodyText(
                text = "\"$notesText\"",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CurrencyDetailsSection(expense: ExpenseDetailUiModel) {
    FlatCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Default),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
        ) {
            DetailSectionHeader(
                label = stringResource(R.string.expense_detail_section_currency),
                icon = { SectionIcon(TablerIcons.Outline.Coin) }
            )
            if (expense.formattedSourceAmount != null) {
                DetailRow(
                    label = "${expense.sourceCurrency} ${stringResource(R.string.expense_review_amount)}",
                    value = expense.formattedSourceAmount
                )
            }
            if (expense.formattedExchangeRate != null) {
                DetailRow(
                    label = stringResource(R.string.expense_review_exchange_rate),
                    value = expense.formattedExchangeRate
                )
            }
            DetailRow(
                label = "${expense.groupCurrency} ${stringResource(R.string.expense_review_group_amount)}",
                value = expense.formattedGroupAmount
            )
        }
    }
}

@Composable
private fun CashTranchesSection(
    tranches: ImmutableList<CashTrancheDetailUiModel>,
    sourceCurrency: String
) {
    FlatCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Default),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            DetailSectionHeader(
                label = stringResource(R.string.add_expense_cash_tranche_funded_from),
                icon = { SectionIcon(TablerIcons.Outline.Wallet) }
            )
            tranches.forEach { tranche ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SecondaryBodyText(text = sourceCurrency)
                    BodyText(text = tranche.formattedAmountConsumed)
                }
            }
        }
    }
}

@Composable
private fun SplitBreakdownSection(
    splitTypeText: String,
    splits: ImmutableList<SplitDetailUiModel>
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionIcon(TablerIcons.Outline.Scale)
            LabelText(text = stringResource(R.string.expense_detail_section_split))
            SecondaryBodyText(text = "· $splitTypeText")
        }
        FlatCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.Default),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
            ) {
                splits.forEach { split ->
                    SplitRow(split = split)
                }
            }
        }
    }
}

@Composable
private fun SplitRow(split: SplitDetailUiModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = split.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (split.isCurrentUser) FontWeight.SemiBold else FontWeight.Normal,
                color = if (split.isCurrentUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (split.shareText != null) {
                CaptionText(
                    text = split.shareText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        BodyText(
            text = split.formattedAmount,
            color = if (split.isExcluded) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun AddOnsSection(
    addOns: ImmutableList<AddOnDetailUiModel>,
    formattedEffectiveTotal: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionIcon(TablerIcons.Outline.Receipt)
            LabelText(text = stringResource(R.string.expense_detail_section_addons))
        }
        FlatCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.Default),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
            ) {
                addOns.forEach { addOn ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            BodyText(text = addOn.labelText)
                            CaptionText(
                                text = addOn.modeText.uppercase(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        BodyText(
                            text = if (addOn.isDiscount) "- ${addOn.formattedAmount}" else addOn.formattedAmount,
                            color = if (addOn.isDiscount) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
                if (formattedEffectiveTotal != null) {
                    DetailRow(
                        label = stringResource(R.string.expense_review_effective_total),
                        value = formattedEffectiveTotal
                    )
                }
            }
        }
    }
}

@Composable
private fun ProvenanceSection(expense: ExpenseDetailUiModel) {
    FlatCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Default),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)) {
                CaptionText(
                    text = stringResource(R.string.expense_detail_created_by, expense.createdByText),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CaptionText(
                    text = expense.createdAtText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SyncStatusIndicator(syncStatus = expense.syncStatus)
        }
    }
}

@Composable
private fun DetailSectionHeader(label: String, icon: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        LabelText(text = label)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        SecondaryBodyText(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
private fun StatusChip(text: String, isHighlighted: Boolean = false) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(
                if (isHighlighted) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
            )
            .padding(horizontal = MaterialTheme.spacing.Small, vertical = 4.dp)
    ) {
        CaptionText(
            text = text,
            color = if (isHighlighted) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun SectionIcon(imageVector: androidx.compose.ui.graphics.vector.ImageVector) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(SECTION_ICON_SIZE)
    )
}
