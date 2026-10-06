package es.pedrazamiguez.splittrip.features.expense.presentation.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTopPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.DeferredLoadingContainer
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.ExpenseDetailUiState

@Composable
fun ExpenseDetailScreen(
    uiState: ExpenseDetailUiState = ExpenseDetailUiState(),
    modifier: Modifier = Modifier,
    onReceiptTap: (() -> Unit)? = null,
    onConfirmPaymentTap: (() -> Unit)? = null
) {
    val topPadding = LocalTopPadding.current
    val bottomPadding = LocalBottomPadding.current

    DeferredLoadingContainer(
        isLoading = uiState.isLoading,
        loadingContent = { ShimmerLoadingList(modifier = Modifier.padding(top = topPadding)) }
    ) {
        when {
            uiState.hasError || uiState.expense == null -> {
                EmptyStateView(
                    title = stringResource(R.string.expense_detail_error_loading),
                    icon = TablerIcons.Outline.Receipt,
                    modifier = Modifier.padding(
                        top = topPadding,
                        bottom = bottomPadding
                    )
                )
            }
            else -> {
                ExpenseDetailContent(
                    expense = uiState.expense,
                    modifier = modifier,
                    onReceiptTap = onReceiptTap,
                    onConfirmPaymentTap = onConfirmPaymentTap
                )
            }
        }
    }
}
