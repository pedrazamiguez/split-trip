package es.pedrazamiguez.splittrip.domain.usecase.setting

interface DeleteLocalAiModelPackUseCase {
    suspend operator fun invoke(): Result<Unit>
}
