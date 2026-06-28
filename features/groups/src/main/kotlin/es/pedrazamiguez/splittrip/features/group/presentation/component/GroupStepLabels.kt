package es.pedrazamiguez.splittrip.features.group.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupStep

@Composable
internal fun rememberGroupStepLabels(steps: List<CreateEditGroupStep>): List<String> {
    val infoLabel = stringResource(R.string.group_wizard_step_info)
    val currencyLabel = stringResource(R.string.group_wizard_step_currency)
    val membersLabel = stringResource(R.string.group_wizard_step_members)
    val unregisteredNamesLabel = stringResource(R.string.group_wizard_step_unregistered_names)
    val imageLabel = stringResource(R.string.group_wizard_step_image)
    val reviewLabel = stringResource(R.string.group_wizard_step_review)

    val stepLabels = remember(infoLabel, currencyLabel, membersLabel, unregisteredNamesLabel, imageLabel, reviewLabel) {
        mapOf(
            CreateEditGroupStep.INFO to infoLabel,
            CreateEditGroupStep.CURRENCY to currencyLabel,
            CreateEditGroupStep.MEMBERS to membersLabel,
            CreateEditGroupStep.UNREGISTERED_NAMES to unregisteredNamesLabel,
            CreateEditGroupStep.IMAGE to imageLabel,
            CreateEditGroupStep.REVIEW to reviewLabel
        )
    }

    return remember(steps, stepLabels) {
        steps.map { stepLabels[it] ?: "" }
    }
}
