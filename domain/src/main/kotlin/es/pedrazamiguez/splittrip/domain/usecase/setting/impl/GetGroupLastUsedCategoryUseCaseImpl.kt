package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.repository.GroupPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetGroupLastUsedCategoryUseCase
import kotlinx.coroutines.flow.Flow

class GetGroupLastUsedCategoryUseCaseImpl(
    private val preferenceRepository: GroupPreferenceRepository
) : GetGroupLastUsedCategoryUseCase {

    override operator fun invoke(groupId: String): Flow<List<String>> = preferenceRepository.getGroupLastUsedCategory(
        groupId
    )
}
