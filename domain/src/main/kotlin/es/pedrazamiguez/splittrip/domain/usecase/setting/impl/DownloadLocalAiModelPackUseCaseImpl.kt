package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.service.LocalAiModelPackService
import es.pedrazamiguez.splittrip.domain.usecase.setting.DownloadLocalAiModelPackUseCase

class DownloadLocalAiModelPackUseCaseImpl(
    private val localAiModelPackService: LocalAiModelPackService
) : DownloadLocalAiModelPackUseCase {

    override suspend operator fun invoke(packId: String?): Result<Unit> =
        localAiModelPackService.requestDownload(packId)
}
