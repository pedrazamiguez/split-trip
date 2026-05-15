package es.pedrazamiguez.splittrip.features.expense.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
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
private val CATEGORY_ICON_SIZE = 20.dp

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
    val bottomPadding = LocalBottomPadding.current

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

        Spacer(modifier = Modifier.height(bottomPadding))
    }
}

// ── Hero ──────────────────────────────────────────────────────────────────────

@Composable
private fun HeroSection(expense: ExpenseDetailUiModel) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            CategoryChip(
                icon = expense.category.toIconVector(),
                label = expense.categoryText
            )
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
                CaptionText(
                    text = expense.paidByText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (expense.isForeignCurrency && expense.formattedSourceAmount != null) {
                    Spacer(Modifier.height(MaterialTheme.spacing.Small))
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

// ── General info ──────────────────────────────────────────────────────────────

@Composable
private fun GeneralInfoSection(expense: ExpenseDetailUiModel) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionIcon(TablerIcons.Outline.CreditCard)
            LabelText(text = stringResource(R.string.expense_detail_section_info))
        }
        FlatCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.Default),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
            ) {
                DetailRow(
                    label = stringResource(R.string.expense_review_payment_method),
                    value = expense.paymentMethodText
                )
                DetailRow(
                    label = stringResource(R.string.expense_detail_paid_by_label),
                    value = expense.createdByText
                )
                if (expense.vendorText != null) {
                    DetailRow(
                        label = stringResource(R.string.expense_review_vendor),
                        value = expense.vendorText
                    )
                }
            }
        }
    }
}

// ── Notes ─────────────────────────────────────────────────────────────────────

@Composable
private fun NotesSection(notesText: String) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionIcon(TablerIcons.Outline.AlignJustified)
            LabelText(text = stringResource(R.string.expense_detail_section_notes))
        }
        FlatCard(modifier = Modifier.fillMaxWidth()) {
            BodyText(
                text = "\"$notesText\"",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.Default)
            )
        }
    }
}

// ── Currency details ──────────────────────────────────────────────────────────

@Composable
private fun CurrencyDetailsSection(expense: ExpenseDetailUiModel) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionIcon(TablerIcons.Outline.Coin)
            LabelText(text = stringResource(R.string.expense_detail_section_currency))
        }
        FlatCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.Default),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
            ) {
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
}

// ── Cash tranches ─────────────────────────────────────────────────────────────

@Composable
private fun CashTranchesSection(
    tranches: ImmutableList<CashTrancheDetailUiModel>,
    sourceCurrency: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionIcon(TablerIcons.Outline.Wallet)
            LabelText(text = stringResource(R.string.add_expense_cash_tranche_funded_from))
        }
        FlatCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.Default),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
            ) {
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
}

// ── Split breakdown ───────────────────────────────────────────────────────────

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
            SmallChip(text = splitTypeText, isPrimary = false)
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
                text = if (split.isCurrentUser) {
                    stringResource(R.string.expense_detail_split_you_badge)
                } else {
                    split.displayName
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
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

// ── Add-ons ───────────────────────────────────────────────────────────────────

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

// ── Provenance footer ─────────────────────────────────────────────────────────

@Composable
private fun ProvenanceSection(expense: ExpenseDetailUiModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.spacing.ExtraSmall,
                vertical = MaterialTheme.spacing.Small
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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

// ── Reusable primitives ───────────────────────────────────────────────────────

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

/** Filled primary-container chip with icon + label. Used for the category in the hero. */
@Composable
private fun CategoryChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = MaterialTheme.spacing.Small, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(CATEGORY_ICON_SIZE),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        CaptionText(text = label, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

/** Neutral surface chip. Used for payment status. */
@Composable
private fun StatusChip(text: String) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = MaterialTheme.spacing.Small, vertical = 4.dp)
    ) {
        CaptionText(text = text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Small outlined chip used in two contexts:
 * - [isPrimary] = true  → current user label in the split breakdown (primary colour)
 * - [isPrimary] = false → split-type label next to section header (neutral colour)
 */
@Composable
private fun SmallChip(text: String, isPrimary: Boolean) {
    val borderColor = if (isPrimary) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val textColor = if (isPrimary) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .border(width = 1.dp, color = borderColor, shape = MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Medium
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
