package es.pedrazamiguez.splittrip.features.profile.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.filled.UserFilled
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.features.profile.R

@Composable
internal fun GuestProfileContent(
    onLinkAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomPadding = LocalBottomPadding.current
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = MaterialTheme.spacing.ExtraLarge)
            .padding(
                top = MaterialTheme.spacing.ExtraLarge,
                bottom = MaterialTheme.spacing.ExtraLarge + bottomPadding
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Section))
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                imageVector = TablerIcons.Filled.UserFilled,
                contentDescription = stringResource(R.string.profile_picture_description),
                modifier = Modifier.padding(MaterialTheme.spacing.ExtraLarge),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraLarge))
        Text(
            text = stringResource(R.string.profile_guest_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraSmall))
        Text(
            text = stringResource(R.string.profile_guest_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Section))

        GuestProfilePromoCard(
            onLinkAccountClick = onLinkAccountClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
