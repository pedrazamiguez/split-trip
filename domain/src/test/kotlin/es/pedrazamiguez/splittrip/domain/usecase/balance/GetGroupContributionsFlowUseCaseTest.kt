package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetGroupContributionsFlowUseCaseTest {

    private val repository: ContributionRepository = mockk()
    private val useCase = GetGroupContributionsFlowUseCase(repository)

    @Test
    fun `invoke delegates to repository`() {
        val groupId = "group_123"
        val expectedFlow = flowOf(emptyList<Contribution>())
        every { repository.getGroupContributionsFlow(groupId) } returns expectedFlow

        val resultFlow = useCase(groupId)

        assertEquals(expectedFlow, resultFlow)
        verify(exactly = 1) { repository.getGroupContributionsFlow(groupId) }
    }
}
