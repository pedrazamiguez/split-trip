package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetAppThemeUseCase
import kotlinx.coroutines.flow.Flow

class GetAppThemeUseCaseImpl(private val preferenceRepository: UserPreferenceRepository) : GetAppThemeUseCase {

    override operator fun invoke(): Flow<String?> = preferenceRepository.getAppTheme()
}
