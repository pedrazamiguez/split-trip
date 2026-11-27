package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.service.LocalAiModelPackService
import es.pedrazamiguez.splittrip.domain.usecase.setting.CancelLocalAiModelPackDownloadUseCase

class CancelLocalAiModelPackDownloadUseCaseImpl(
    private val localAiModelPackService: LocalAiModelPackService
) : CancelLocalAiModelPackDownloadUseCase {

    override suspend operator fun invoke(packId: String?): Result<Unit> =
        localAiModelPackService.cancelDownload(packId)
}
