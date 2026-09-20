package es.pedrazamiguez.splittrip.features.settings.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.settings.presentation.component.DeveloperCreditsCard
import es.pedrazamiguez.splittrip.features.settings.presentation.component.DeveloperHeroCard
import es.pedrazamiguez.splittrip.features.settings.presentation.component.DeveloperLinksCard
import es.pedrazamiguez.splittrip.features.settings.presentation.model.DeveloperInfoUiState

private val PREVIEW_DEVELOPER_INFO = DeveloperInfoUiState(
    name = "Andrés Pedraza Míguez",
    role = "Lead Developer & Designer",
    bio = "Passionate about building intuitive Android applications with modern architecture and Jetpack Compose.",
    avatarUrl = "",
    githubUrl = "https://github.com/pedrazamiguez",
    splitTripRepoUrl = "https://github.com/pedrazamiguez/split-trip",
    linkedinUrl = "https://linkedin.com/in/pedrazamiguez",
    portfolioUrl = "https://pedrazamiguez.es",
    credits = "Built with Kotlin, Jetpack Compose, Material 3, and Firebase.",
    copyright = "© 2026 SplitTrip"
)

@PreviewComplete
@Composable
fun DeveloperHeroCardPreview() {
    PreviewThemeWrapper {
        DeveloperHeroCard(
            uiState = PREVIEW_DEVELOPER_INFO
        )
    }
}

@PreviewComplete
@Composable
fun DeveloperLinksCardPreview() {
    PreviewThemeWrapper {
        DeveloperLinksCard(
            uiState = PREVIEW_DEVELOPER_INFO,
            onLinkClick = {}
        )
    }
}

@PreviewComplete
@Composable
fun DeveloperCreditsCardPreview() {
    PreviewThemeWrapper {
        DeveloperCreditsCard(
            credits = PREVIEW_DEVELOPER_INFO.credits
        )
    }
}
