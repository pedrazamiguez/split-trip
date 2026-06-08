package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.repository.GroupPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.SetGroupLastUsedPaymentMethodUseCaseImpl
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SetGroupLastUsedPaymentMethodUseCaseTest {

    private val repository: GroupPreferenceRepository = mockk(relaxed = true)
    private val useCase = SetGroupLastUsedPaymentMethodUseCaseImpl(repository)

    @Test
    fun `delegates to repository with group id and payment method id`() = runTest {
        val groupId = "group-123"
        val paymentMethodId = "CREDIT_CARD"

        useCase(groupId, paymentMethodId)

        coVerify { repository.setGroupLastUsedPaymentMethod(groupId, paymentMethodId) }
    }
}
