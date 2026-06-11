package es.pedrazamiguez.splittrip.features.profile.presentation.mapper.impl

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.profile.presentation.mapper.ProfileUiMapper
import es.pedrazamiguez.splittrip.features.profile.presentation.model.ProfileUiModel

class ProfileUiMapperImpl : ProfileUiMapper {

    override fun toProfileUiModel(user: User): ProfileUiModel = with(user) {
        ProfileUiModel(
            displayName = displayName ?: email,
            email = email,
            profileImageUrl = profileImagePath,
            bio = ""
        )
    }
}
