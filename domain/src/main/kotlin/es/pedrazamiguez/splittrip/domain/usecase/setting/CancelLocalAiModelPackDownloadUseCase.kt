package es.pedrazamiguez.splittrip.domain.usecase.setting

interface CancelLocalAiModelPackDownloadUseCase {
    suspend operator fun invoke(): Result<Unit>
}
