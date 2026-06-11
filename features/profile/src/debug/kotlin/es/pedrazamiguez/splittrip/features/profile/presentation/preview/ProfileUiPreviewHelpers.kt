package es.pedrazamiguez.splittrip.features.profile.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.MappedPreview
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.profile.presentation.mapper.impl.ProfileUiMapperImpl
import es.pedrazamiguez.splittrip.features.profile.presentation.model.ProfileUiModel

@Composable
fun ProfileUiPreviewHelper(
    domainUser: User = PREVIEW_USER,
    content: @Composable (ProfileUiModel) -> Unit
) {
    MappedPreview(
        domain = domainUser,
        mapper = { localeProvider, _ ->
            ProfileUiMapperImpl()
        },
        transform = { mapper, domain ->
            mapper.toProfileUiModel(domain)
        },
        content = content
    )
}
