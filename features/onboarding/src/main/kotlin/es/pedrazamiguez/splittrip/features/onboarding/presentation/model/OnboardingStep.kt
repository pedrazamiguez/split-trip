package es.pedrazamiguez.splittrip.features.onboarding.presentation.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Cash
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.PlaneTilt
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Scale
import es.pedrazamiguez.splittrip.features.onboarding.R

enum class OnboardingStep(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val icon: ImageVector
) {
    TRIPS_AND_GROUPS(
        titleRes = R.string.onboarding_step_trips_title,
        descriptionRes = R.string.onboarding_step_trips_description,
        icon = TablerIcons.Outline.PlaneTilt
    ),
    SMART_SPLITTING(
        titleRes = R.string.onboarding_step_split_title,
        descriptionRes = R.string.onboarding_step_split_description,
        icon = TablerIcons.Outline.Receipt
    ),
    CASH_AND_POCKET(
        titleRes = R.string.onboarding_step_cash_title,
        descriptionRes = R.string.onboarding_step_cash_description,
        icon = TablerIcons.Outline.Cash
    ),
    CONSENSUS_AND_SETTLEMENT(
        titleRes = R.string.onboarding_step_settle_title,
        descriptionRes = R.string.onboarding_step_settle_description,
        icon = TablerIcons.Outline.Scale
    )
}
