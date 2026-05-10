package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.DestructiveButton
import es.pedrazamiguez.splittrip.features.settings.R

@Composable
fun LogoutButton(onLogoutClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.ExtraLarge, vertical = MaterialTheme.spacing.Section),
        contentAlignment = Alignment.Center
    ) {
        DestructiveButton(
            text = stringResource(R.string.settings_logout_title),
            onClick = onLogoutClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
