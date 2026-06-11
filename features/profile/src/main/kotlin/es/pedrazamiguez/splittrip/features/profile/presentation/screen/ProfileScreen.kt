package es.pedrazamiguez.splittrip.features.profile.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LargeBodyText
import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType
import es.pedrazamiguez.splittrip.features.profile.R
import es.pedrazamiguez.splittrip.features.profile.presentation.component.LinkEmailPasswordDialog
import es.pedrazamiguez.splittrip.features.profile.presentation.component.ProviderRow
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.event.ProfileUiEvent
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.state.ProfileUiState

@Suppress("LongMethod")
@Composable
fun ProfileScreen(
    uiState: ProfileUiState = ProfileUiState(),
    onLinkGoogleClick: () -> Unit = {},
    onEvent: (ProfileUiEvent) -> Unit = {}
) {
    DeferredLoadingContainer(
        isLoading = uiState.isLoading,
        loadingContent = { ShimmerLoadingList() }
    ) {
        when {
            uiState.hasError && uiState.profile == null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    SecondaryButton(
                        text = stringResource(R.string.profile_retry_button),
                        onClick = { onEvent(ProfileUiEvent.LoadProfile) },
                        leadingIcon = TablerIcons.Outline.Refresh
                    )
                }
            }
            uiState.profile != null -> {
                val profile = uiState.profile
                Column(
                    modifier = Modifier.fillMaxSize().padding(MaterialTheme.spacing.ExtraLarge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.Section))
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
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraLarge))
                    Text(
                        text = profile.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraSmall))
                    LargeBodyText(
                        text = profile.email,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (profile.memberSinceText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Default))
                        BodyText(
                            text = stringResource(R.string.profile_member_since, profile.memberSinceText),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.Section))

                    Text(
                        text = stringResource(R.string.profile_linked_accounts_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.Small))

                    FlatCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(MaterialTheme.spacing.Medium),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
                        ) {
                            val isEmailLinked = uiState.linkedProviders.contains(AuthProviderType.EMAIL_PASSWORD)
                            ProviderRow(
                                name = stringResource(R.string.profile_provider_email_password),
                                isLinked = isEmailLinked,
                                onLinkClick = { onEvent(ProfileUiEvent.ShowLinkEmailDialog) },
                                onUnlinkClick = {
                                    onEvent(ProfileUiEvent.UnlinkProvider(AuthProviderType.EMAIL_PASSWORD))
                                },
                                canUnlink = uiState.linkedProviders.size > 1,
                                isActionLoading = uiState.isLinking
                            )

                            val isGoogleLinked = uiState.linkedProviders.contains(AuthProviderType.GOOGLE)
                            ProviderRow(
                                name = stringResource(R.string.profile_provider_google),
                                isLinked = isGoogleLinked,
                                onLinkClick = onLinkGoogleClick,
                                onUnlinkClick = { onEvent(ProfileUiEvent.UnlinkProvider(AuthProviderType.GOOGLE)) },
                                canUnlink = uiState.linkedProviders.size > 1,
                                isActionLoading = uiState.isLinking
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.showLinkEmailDialog) {
        LinkEmailPasswordDialog(
            uiState = uiState,
            onEvent = onEvent
        )
    }
}
