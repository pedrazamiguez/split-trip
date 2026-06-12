package es.pedrazamiguez.splittrip.features.profile.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.features.profile.presentation.model.ProfileUiModel

@Composable
internal fun ProfileContent(
    profile: ProfileUiModel,
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
        ProfileHeader(
            displayName = profile.displayName,
            email = profile.email,
            profileImageUrl = profile.profileImageUrl
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Section))

        ProfileBio(
            bio = profile.bio
        )
    }
}
