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
import es.pedrazamiguez.splittrip.features.profile.presentation.model.ProfileUiModel

@Composable
internal fun ProfileContent(
    profile: ProfileUiModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(MaterialTheme.spacing.ExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        ProfileHeader(
            displayName = profile.displayName,
            email = profile.email,
            profileImageUrl = profile.profileImageUrl,
            isPro = profile.isPro
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Section))

        ProfileBio(bio = profile.bio)

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Section))

        ProfileQrShareCard(
            email = profile.email,
            userId = profile.userId
        )
    }
}
