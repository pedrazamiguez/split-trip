package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.R
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CircleCheck
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CircleDotted
import es.pedrazamiguez.splittrip.domain.model.PasswordRequirementStatus

@Composable
fun PasswordRequirementsIndicator(
    status: PasswordRequirementStatus,
    modifier: Modifier = Modifier,
    minLengthText: String = stringResource(R.string.password_requirement_min_length),
    upperCaseText: String = stringResource(R.string.password_requirement_uppercase),
    lowerCaseText: String = stringResource(R.string.password_requirement_lowercase),
    digitText: String = stringResource(R.string.password_requirement_digit),
    specialCharText: String = stringResource(R.string.password_requirement_special_char)
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
    ) {
        RequirementRow(
            satisfied = status.isMinLengthValid,
            text = minLengthText
        )
        RequirementRow(
            satisfied = status.hasUpperCase,
            text = upperCaseText
        )
        RequirementRow(
            satisfied = status.hasLowerCase,
            text = lowerCaseText
        )
        RequirementRow(
            satisfied = status.hasDigit,
            text = digitText
        )
        RequirementRow(
            satisfied = status.hasSpecialChar,
            text = specialCharText
        )
    }
}

@Composable
private fun RequirementRow(
    satisfied: Boolean,
    text: String,
    modifier: Modifier = Modifier
) {
    val iconColor by animateColorAsState(
        targetValue = if (satisfied) {
            MaterialTheme.colorScheme.secondary
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        },
        label = "RequirementIconColor"
    )

    val textColor by animateColorAsState(
        targetValue = if (satisfied) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        },
        label = "RequirementTextColor"
    )

    val icon = if (satisfied) {
        TablerIcons.Outline.CircleCheck
    } else {
        TablerIcons.Outline.CircleDotted
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = textColor
        )
    }
}
