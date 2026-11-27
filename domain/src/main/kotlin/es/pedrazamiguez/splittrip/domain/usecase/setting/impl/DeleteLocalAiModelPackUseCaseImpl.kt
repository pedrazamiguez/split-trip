package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.service.LocalAiModelPackService
import es.pedrazamiguez.splittrip.domain.usecase.setting.DeleteLocalAiModelPackUseCase

class DeleteLocalAiModelPackUseCaseImpl(
    private val localAiModelPackService: LocalAiModelPackService
) : DeleteLocalAiModelPackUseCase {

    override suspend operator fun invoke(): Result<Unit> = localAiModelPackService.deletePack()
}
