package es.pedrazamiguez.splittrip.features.profile.presentation.model

data class ProfileUiModel(
    val displayName: String = "",
    val email: String = "",
    val profileImageUrl: String? = null,
    val bio: String = ""
)
