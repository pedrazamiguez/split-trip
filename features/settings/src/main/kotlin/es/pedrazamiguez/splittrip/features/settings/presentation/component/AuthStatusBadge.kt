package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.features.settings.R

@Composable
fun AuthStatusBadge(
    modifier: Modifier = Modifier
) {
    PassportTag(
        text = stringResource(R.string.account_security_status_verified),
        style = PassportTagStyle.SUCCESS,
        modifier = modifier
    )
}
