package es.pedrazamiguez.splittrip.core.designsystem.preview.topbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.DynamicTopAppBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.ProfileAvatarButton
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper

private const val DUMMY_AVATAR_URL = "https://example.com/avatar.jpg"

@PreviewComplete
@Composable
private fun ProfileAvatarButtonFreeIconPreview() {
    PreviewThemeWrapper {
        Box(modifier = Modifier.padding(16.dp)) {
            ProfileAvatarButton(
                avatarUrl = null,
                onClick = {},
                isPro = false
            )
        }
    }
}

@PreviewComplete
@Composable
private fun ProfileAvatarButtonProIconPreview() {
    PreviewThemeWrapper {
        Box(modifier = Modifier.padding(16.dp)) {
            ProfileAvatarButton(
                avatarUrl = null,
                onClick = {},
                isPro = true
            )
        }
    }
}

@PreviewComplete
@Composable
private fun ProfileAvatarButtonFreeImagePreview() {
    PreviewThemeWrapper {
        Box(modifier = Modifier.padding(16.dp)) {
            ProfileAvatarButton(
                avatarUrl = DUMMY_AVATAR_URL,
                onClick = {},
                isPro = false
            )
        }
    }
}

@PreviewComplete
@Composable
private fun ProfileAvatarButtonProImagePreview() {
    PreviewThemeWrapper {
        Box(modifier = Modifier.padding(16.dp)) {
            ProfileAvatarButton(
                avatarUrl = DUMMY_AVATAR_URL,
                onClick = {},
                isPro = true
            )
        }
    }
}

@PreviewComplete
@Composable
private fun ProfileAvatarButtonInTopBarPreview() {
    PreviewThemeWrapper {
        DynamicTopAppBar(
            title = "SplitTrip",
            actions = {
                Row {
                    ProfileAvatarButton(
                        avatarUrl = null,
                        onClick = {},
                        isPro = false
                    )
                    ProfileAvatarButton(
                        avatarUrl = null,
                        onClick = {},
                        isPro = true
                    )
                }
            }
        )
    }
}
