package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface SetGroupLastUsedPaymentMethodUseCase : UseCase {
    suspend operator fun invoke(groupId: String, paymentMethodId: String)
}
