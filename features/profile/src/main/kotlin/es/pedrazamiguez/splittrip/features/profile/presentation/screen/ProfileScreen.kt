package es.pedrazamiguez.splittrip.features.profile.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.filled.UserFilled
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Refresh
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.DeferredLoadingContainer
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.features.profile.R
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.event.ProfileUiEvent
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.state.ProfileUiState

@Composable
fun ProfileScreen(uiState: ProfileUiState = ProfileUiState(), onEvent: (ProfileUiEvent) -> Unit = {}) {
    DeferredLoadingContainer(
        isLoading = uiState.isLoading,
        loadingContent = { ShimmerLoadingList() }
    ) {
        when {
            uiState.hasError && uiState.profile == null -> {
                ProfileErrorContent(
                    onRetry = { onEvent(ProfileUiEvent.LoadProfile) }
                )
            }
            uiState.profile != null -> ProfileLoadedContent(profile = uiState.profile)
        }
    }
}

@Composable
private fun ProfileErrorContent(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        SecondaryButton(
            text = stringResource(R.string.profile_retry_button),
            onClick = onRetry,
            leadingIcon = TablerIcons.Outline.Refresh
        )
    }
}

@Composable
private fun ProfileLoadedContent(
    profile: es.pedrazamiguez.splittrip.features.profile.presentation.model.ProfileUiModel
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(MaterialTheme.spacing.ExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Section))
        ProfileAvatarSection(profile = profile)
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraLarge))
        Text(
            text = profile.displayName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraSmall))
        Text(
            text = profile.email,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (profile.memberSinceText.isNotBlank()) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.Default))
            Text(
                text = stringResource(R.string.profile_member_since, profile.memberSinceText),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileAvatarSection(
    profile: es.pedrazamiguez.splittrip.features.profile.presentation.model.ProfileUiModel
) {
    if (profile.profileImageUrl != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(profile.profileImageUrl)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(R.string.profile_picture_description),
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(120.dp).clip(CircleShape)
        )
    } else {
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
    }
}
