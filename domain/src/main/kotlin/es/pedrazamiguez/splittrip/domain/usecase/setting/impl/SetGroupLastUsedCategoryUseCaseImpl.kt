package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.repository.GroupPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetGroupLastUsedCategoryUseCase

class SetGroupLastUsedCategoryUseCaseImpl(
    private val preferenceRepository: GroupPreferenceRepository
) : SetGroupLastUsedCategoryUseCase {

    override suspend operator fun invoke(groupId: String, categoryId: String) {
        preferenceRepository.setGroupLastUsedCategory(groupId, categoryId)
    }
}
