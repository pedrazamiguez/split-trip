package es.pedrazamiguez.splittrip.features.group.presentation.model

data class GroupMemberUiModel(
    val userId: String = "",
    val displayName: String = "",
    val avatarUrl: String? = null,
    val isCreator: Boolean = false,
    val roleBadgeText: String = ""
)
