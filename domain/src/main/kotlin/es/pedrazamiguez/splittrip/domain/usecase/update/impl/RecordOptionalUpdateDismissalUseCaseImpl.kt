package es.pedrazamiguez.splittrip.domain.usecase.update.impl

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.update.RecordOptionalUpdateDismissalUseCase

class RecordOptionalUpdateDismissalUseCaseImpl(
    private val userPreferenceRepository: UserPreferenceRepository,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) : RecordOptionalUpdateDismissalUseCase {

    override suspend fun invoke(versionCode: Long) {
        userPreferenceRepository.recordOptionalUpdateDismissal(
            versionCode = versionCode,
            timestampMs = currentTimeMillis()
        )
    }
}
