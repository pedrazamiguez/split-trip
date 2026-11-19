package es.pedrazamiguez.splittrip.domain.usecase.update

interface RecordOptionalUpdateDismissalUseCase {
    suspend operator fun invoke(versionCode: Long)
}
