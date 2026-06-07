package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.repository.GroupPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.GetGroupLastUsedPaymentMethodUseCaseImpl
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetGroupLastUsedPaymentMethodUseCaseTest {

    private val repository: GroupPreferenceRepository = mockk()
    private val useCase = GetGroupLastUsedPaymentMethodUseCaseImpl(repository)

    @Test
    fun `returns payment method list flow from repository`() = runTest {
        val groupId = "group-123"
        val expectedMethods = listOf("CREDIT_CARD", "CASH")
        every { repository.getGroupLastUsedPaymentMethod(groupId) } returns flowOf(expectedMethods)

        val result = useCase(groupId).first()

        assertEquals(expectedMethods, result)
    }

    @Test
    fun `returns empty list when no payment methods stored`() = runTest {
        val groupId = "group-456"
        every { repository.getGroupLastUsedPaymentMethod(groupId) } returns flowOf(emptyList())

        val result = useCase(groupId).first()

        assertTrue(result.isEmpty())
    }
}
