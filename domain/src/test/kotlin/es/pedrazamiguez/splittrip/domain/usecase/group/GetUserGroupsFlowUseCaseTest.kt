package es.pedrazamiguez.splittrip.domain.usecase.group

import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.GetUserGroupsFlowUseCaseImpl
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetUserGroupsFlowUseCaseTest {

    private val repository: GroupRepository = mockk()
    private val useCase = GetUserGroupsFlowUseCaseImpl(repository)

    @Test
    fun `invoke delegates to repository`() {
        val expectedFlow = flowOf(emptyList<Group>())
        every { repository.getAllGroupsFlow() } returns expectedFlow

        val resultFlow = useCase()

        assertEquals(expectedFlow, resultFlow)
        verify(exactly = 1) { repository.getAllGroupsFlow() }
    }
}
