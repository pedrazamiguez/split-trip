package es.pedrazamiguez.splittrip.features.profile.presentation.mapper

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.profile.presentation.model.ProfileUiModel

interface ProfileUiMapper {
    fun toProfileUiModel(user: User, isAllowlistedPro: Boolean = false): ProfileUiModel
}
