package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.SubscriptionsUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.SubscriptionsUiState
import kotlinx.coroutines.launch

private val PAGER_HORIZONTAL_PADDING = 28.dp
private val PAGER_PAGE_SPACING = 16.dp
private const val PRO_PAGE_INDEX = 1
private const val FREE_PAGE_INDEX = 0

@Suppress("LongMethod", "CognitiveComplexMethod")
@Composable
fun SubscriptionsContent(
    uiState: SubscriptionsUiState,
    onEvent: (SubscriptionsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomPadding = LocalBottomPadding.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val initialPage = if (uiState.plans.size > PRO_PAGE_INDEX) PRO_PAGE_INDEX else FREE_PAGE_INDEX
    val pagerState = rememberPagerState(initialPage = initialPage) { uiState.plans.size }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                top = MaterialTheme.spacing.ExtraLarge,
                bottom = MaterialTheme.spacing.ExtraLarge + bottomPadding
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        BillingIntervalSelector(
            selectedInterval = uiState.selectedInterval,
            onIntervalSelected = { interval ->
                onEvent(SubscriptionsUiEvent.SelectBillingInterval(interval))
                if (pagerState.currentPage == FREE_PAGE_INDEX && uiState.plans.size > PRO_PAGE_INDEX) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(PRO_PAGE_INDEX)
                    }
                }
            },
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.ExtraLarge)
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Large))

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = PAGER_HORIZONTAL_PADDING),
            pageSpacing = PAGER_PAGE_SPACING,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val plan = uiState.plans.getOrNull(page)
            if (plan != null) {
                PlanComparisonCard(
                    plan = plan,
                    onCtaClick = { onEvent(SubscriptionsUiEvent.UpgradePlan(it)) },
                    isProcessingAction = uiState.isProcessingAction
                )
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Medium))

        SubscriptionsPageIndicator(
            pageCount = uiState.plans.size,
            currentPage = pagerState.currentPage
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Large))

        SubscriptionsDisclaimerFooter(
            onRestorePurchasesClick = { onEvent(SubscriptionsUiEvent.RestorePurchases) },
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.ExtraLarge)
        )
    }
}
