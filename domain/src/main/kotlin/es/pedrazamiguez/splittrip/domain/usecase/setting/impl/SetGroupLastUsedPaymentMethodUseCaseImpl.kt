package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.repository.GroupPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetGroupLastUsedPaymentMethodUseCase

class SetGroupLastUsedPaymentMethodUseCaseImpl(
    private val preferenceRepository: GroupPreferenceRepository
) : SetGroupLastUsedPaymentMethodUseCase {

    override suspend operator fun invoke(groupId: String, paymentMethodId: String) {
        preferenceRepository.setGroupLastUsedPaymentMethod(groupId, paymentMethodId)
    }
}
