package es.pedrazamiguez.splittrip.domain.usecase.group

import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.ObserveGroupUseCaseImpl
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ObserveGroupUseCaseImplTest {

    private val repository: GroupRepository = mockk()
    private val useCase = ObserveGroupUseCaseImpl(repository)

    @Test
    fun `invoke delegates to repository getGroupByIdFlow`() = runTest {
        val groupId = "group_123"
        val expectedGroup = Group(id = groupId, name = "Test Group")
        every { repository.getGroupByIdFlow(groupId) } returns flowOf(expectedGroup)

        val result = useCase(groupId).firstOrNull()

        assertEquals(expectedGroup, result)
        verify(exactly = 1) { repository.getGroupByIdFlow(groupId) }
    }
}
