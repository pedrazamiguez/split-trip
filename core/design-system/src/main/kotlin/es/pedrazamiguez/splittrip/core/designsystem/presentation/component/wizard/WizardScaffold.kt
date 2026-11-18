package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTopPadding

/**
 * Canonical top-level scaffold for all multi-step wizard flows (`Expenses`, `Withdrawals`,
 * `Contributions`, `Groups`, `Subunits`, and `Settlements`).
 *
 * Centralizes:
 * - [LocalTopPadding] and [LocalBottomPadding] integration.
 * - A shared [HazeState] connecting the scrollable step canvas ([hazeSource]) to the
 *   fixed-bottom [WizardNavigationBar]'s frosted-glass scrim.
 * - [WizardStepIndicator] + optional [headerBanner] (e.g. receipt auto-fill banner) above
 *   the step content slot.
 * - Optional full-screen [overlay] (e.g. receipt analysis overlay) above the navigation bar.
 */
@Suppress("LongParameterList")
@Composable
fun WizardScaffold(
    stepLabels: List<String>,
    currentStepIndex: Int,
    navigationConfig: WizardNavigationBarConfig,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    optionalStepIndices: Set<Int> = emptySet(),
    allowForwardJumps: Boolean = false,
    onStepClicked: ((Int) -> Unit)? = null,
    headerBanner: (@Composable () -> Unit)? = null,
    overlay: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val topPadding = LocalTopPadding.current
    val bottomPadding = LocalBottomPadding.current
    val wizardHazeState = remember { HazeState() }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = wizardHazeState)
                .padding(top = topPadding)
        ) {
            if (stepLabels.isNotEmpty()) {
                WizardStepIndicator(
                    stepLabels = stepLabels,
                    currentStepIndex = currentStepIndex,
                    optionalStepIndices = optionalStepIndices,
                    allowForwardJumps = allowForwardJumps,
                    onStepClicked = onStepClicked
                )
            }

            headerBanner?.invoke()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                content()
            }
        }

        WizardNavigationBar(
            config = navigationConfig,
            onBack = onBack,
            onNext = onNext,
            onSubmit = onSubmit,
            hazeState = wizardHazeState,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding)
        )

        overlay?.invoke()
    }
}

/**
 * Standard scrollable step container used inside `AnimatedContent` across all wizard flows.
 *
 * Applies [imePadding], [verticalScroll], and bottom clearance for the fixed [WizardNavigationBar]
 * (`LocalBottomPadding.current + rememberWizardNavBarPadding()`), and renders an optional
 * [fastTrackBanner] (such as [WizardFastTrackBanner]) at the top of the scrollable canvas.
 */
@Composable
fun WizardScrollableStepContainer(
    modifier: Modifier = Modifier,
    fastTrackBanner: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val bottomPadding = LocalBottomPadding.current
    val navBarPadding = rememberWizardNavBarPadding()

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomPadding + navBarPadding)
    ) {
        fastTrackBanner?.invoke()
        content()
    }
}
