package es.pedrazamiguez.splittrip.features.settings.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.settings.presentation.model.DeveloperInfoUiState
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.DeveloperInfoScreen

@PreviewComplete
@Composable
private fun DeveloperInfoScreenPreview() {
    PreviewThemeWrapper {
        DeveloperInfoScreen(
            uiState = DeveloperInfoUiState(
                name = "Andrés Pedraza Míguez",
                role = "Senior Java & Kotlin Engineer",
                bio = "Senior Backend Engineer with over 14 years of experience designing scalable systems, " +
                    "now leveraging a strong hybrid skill set in Java (Backend) and Kotlin (Mobile/Android). " +
                    "Proven track record working as a Remote Contractor for UK-based companies, delivering " +
                    "high-quality solutions in English-speaking environments.\n\n" +
                    "Specialises in Java 21, Spring Boot, and Hexagonal Architecture, with recent hands-on " +
                    "leadership in Android Native (Jetpack Compose) projects. Known for stepping into complex, " +
                    "legacy environments to refactor code, mentor senior peers, and drive delivery. " +
                    "Passionate about software craftsmanship, automated testing, and solving critical " +
                    "business problems across the full stack.",
                avatarUrl = "",
                githubUrl = "https://github.com/pedrazamiguez",
                splitTripRepoUrl = "https://github.com/pedrazamiguez/split-trip",
                linkedinUrl = "https://www.linkedin.com/in/pedrazamiguez",
                portfolioUrl = "https://pedrazamiguez.github.io",
                credits = "SplitTrip is a modular Android application designed for travellers to manage shared " +
                    "expenses efficiently. It allows users to create expense groups, track spending in multiple " +
                    "currencies, calculate debts, and sync data across devices.\n\n" +
                    "Built with modern Android practices—including Jetpack Compose, Clean Architecture, and " +
                    "Offline-First principles—the app serves as a reference for scalable, multi-module Android " +
                    "development.",
                copyright = "© 2026 Andrés Pedraza Míguez.\nAll rights reserved."
            ),
            onLinkClick = {}
        )
    }
}
