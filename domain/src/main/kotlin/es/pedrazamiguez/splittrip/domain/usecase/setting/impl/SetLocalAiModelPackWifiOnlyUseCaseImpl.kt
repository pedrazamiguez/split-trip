package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.service.LocalAiModelPackService
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetLocalAiModelPackWifiOnlyUseCase

class SetLocalAiModelPackWifiOnlyUseCaseImpl(
    private val localAiModelPackService: LocalAiModelPackService
) : SetLocalAiModelPackWifiOnlyUseCase {

    override suspend operator fun invoke(wifiOnly: Boolean) {
        localAiModelPackService.setWifiOnlyPreference(wifiOnly)
    }
}
