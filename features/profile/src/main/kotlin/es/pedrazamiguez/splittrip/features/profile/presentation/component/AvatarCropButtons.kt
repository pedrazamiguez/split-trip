package es.pedrazamiguez.splittrip.features.profile.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.features.profile.R

@Composable
internal fun AvatarCropButtons(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = MaterialTheme.spacing.ExtraLarge)
            .padding(bottom = MaterialTheme.spacing.Large),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SecondaryButton(
            text = stringResource(R.string.crop_cancel),
            onClick = onCancel,
            modifier = Modifier.weight(1f)
        )

        GradientButton(
            text = stringResource(R.string.crop_confirm),
            onClick = onConfirm,
            modifier = Modifier.weight(1f)
        )
    }
}
