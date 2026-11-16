package es.pedrazamiguez.splittrip.features.expense.presentation.preview

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.preview.MappedPreview
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemes
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.payment.FundingSourceSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.FundingSourceStep
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseOptionsUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.model.FundingSourceUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import kotlinx.collections.immutable.ImmutableList

private val PREVIEW_PAYER_TYPES = listOf(PayerType.GROUP, PayerType.USER)

@Composable
private fun FundingSourcePreviewHelper(
    selectedPayerType: PayerType,
    content: @Composable (
        fundingSources: ImmutableList<FundingSourceUiModel>,
        selectedFundingSource: FundingSourceUiModel?
    ) -> Unit
) {
    MappedPreview(
        domain = PREVIEW_PAYER_TYPES,
        mapper = { localeProvider, resourceProvider ->
            AddExpenseOptionsUiMapper(
                resourceProvider = resourceProvider,
                formattingHelper = FormattingHelper(localeProvider)
            )
        },
        transform = { mapper, payerTypes -> mapper.mapFundingSources(payerTypes) }
    ) { fundingSources ->
        val selectedFundingSource = fundingSources.firstOrNull { it.id == selectedPayerType.name }
        content(fundingSources, selectedFundingSource)
    }
}

@PreviewThemes
@Composable
private fun FundingSourceSectionGroupPocketSelectedPreview() {
    FundingSourcePreviewHelper(selectedPayerType = PayerType.GROUP) { fundingSources, selectedFundingSource ->
        FundingSourceSection(
            fundingSources = fundingSources,
            selectedFundingSource = selectedFundingSource,
            onFundingSourceSelected = {},
            modifier = Modifier.padding(MaterialTheme.spacing.Default)
        )
    }
}

@PreviewThemes
@Composable
private fun FundingSourceSectionYourMoneySelectedPreview() {
    FundingSourcePreviewHelper(selectedPayerType = PayerType.USER) { fundingSources, selectedFundingSource ->
        FundingSourceSection(
            fundingSources = fundingSources,
            selectedFundingSource = selectedFundingSource,
            onFundingSourceSelected = {},
            modifier = Modifier.padding(MaterialTheme.spacing.Default)
        )
    }
}

@PreviewComplete
@Composable
private fun FundingSourceStepGroupPocketSelectedPreview() {
    FundingSourcePreviewHelper(selectedPayerType = PayerType.GROUP) { fundingSources, selectedFundingSource ->
        FundingSourceStep(
            uiState = AddExpenseUiState(
                fundingSources = fundingSources,
                selectedFundingSource = selectedFundingSource
            ),
            onEvent = {}
        )
    }
}

@PreviewComplete
@Composable
private fun FundingSourceStepYourMoneySelectedPreview() {
    FundingSourcePreviewHelper(selectedPayerType = PayerType.USER) { fundingSources, selectedFundingSource ->
        FundingSourceStep(
            uiState = AddExpenseUiState(
                fundingSources = fundingSources,
                selectedFundingSource = selectedFundingSource
            ),
            onEvent = {}
        )
    }
}
