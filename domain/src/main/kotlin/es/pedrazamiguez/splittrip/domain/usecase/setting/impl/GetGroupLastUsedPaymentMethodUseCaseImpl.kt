package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.repository.GroupPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetGroupLastUsedPaymentMethodUseCase
import kotlinx.coroutines.flow.Flow

class GetGroupLastUsedPaymentMethodUseCaseImpl(
    private val preferenceRepository: GroupPreferenceRepository
) : GetGroupLastUsedPaymentMethodUseCase {

    override operator fun invoke(groupId: String): Flow<List<String>> =
        preferenceRepository.getGroupLastUsedPaymentMethod(groupId)
}
