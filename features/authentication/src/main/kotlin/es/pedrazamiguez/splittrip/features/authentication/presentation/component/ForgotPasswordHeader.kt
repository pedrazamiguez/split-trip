package es.pedrazamiguez.splittrip.features.authentication.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.features.authentication.R

@Composable
internal fun ForgotPasswordHeader(
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .shadow(
                    elevation = if (isDark) 0.dp else 8.dp,
                    shape = CircleShape
                )
                .background(
                    color = if (isDark) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else {
                        Color.White
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = DesignSystemR.drawable.ic_brand_logo),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Large))

        BodyText(
            text = stringResource(id = R.string.forgot_password_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = MaterialTheme.spacing.Large)
        )
    }
}
