package es.pedrazamiguez.splittrip.domain.usecase.setting

interface DownloadLocalAiModelPackUseCase {
    suspend operator fun invoke(packId: String? = null): Result<Unit>
}
