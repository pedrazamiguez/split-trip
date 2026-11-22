package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.R

/**
 * Standardized read-only tag indicating that a referenced member has left the group.
 */
@Composable
fun FormerMemberTag(
    modifier: Modifier = Modifier
) {
    PassportTag(
        text = stringResource(R.string.member_left_group_badge),
        style = PassportTagStyle.NEUTRAL,
        modifier = modifier
    )
}
