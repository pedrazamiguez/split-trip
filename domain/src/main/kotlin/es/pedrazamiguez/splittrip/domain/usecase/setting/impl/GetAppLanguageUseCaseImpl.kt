package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetAppLanguageUseCase
import kotlinx.coroutines.flow.Flow

class GetAppLanguageUseCaseImpl(private val preferenceRepository: UserPreferenceRepository) : GetAppLanguageUseCase {

    override operator fun invoke(): Flow<String?> = preferenceRepository.getAppLanguage()
}
