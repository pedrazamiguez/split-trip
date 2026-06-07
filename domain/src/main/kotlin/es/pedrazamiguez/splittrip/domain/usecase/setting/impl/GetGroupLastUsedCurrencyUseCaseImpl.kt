package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.repository.GroupPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetGroupLastUsedCurrencyUseCase
import kotlinx.coroutines.flow.Flow

class GetGroupLastUsedCurrencyUseCaseImpl(
    private val preferenceRepository: GroupPreferenceRepository
) : GetGroupLastUsedCurrencyUseCase {

    override operator fun invoke(groupId: String): Flow<String?> = preferenceRepository.getGroupLastUsedCurrency(
        groupId
    )
}
