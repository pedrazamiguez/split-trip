package es.pedrazamiguez.splittrip.domain.usecase.group

import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetGroupByIdUseCaseTest {

    private val repository: GroupRepository = mockk()
    private val useCase = GetGroupByIdUseCase(repository)

    @Test
    fun `invoke delegates to repository`() = runTest {
        val groupId = "group_123"
        val expectedGroup = Group(id = groupId, name = "Test Group")
        coEvery { repository.getGroupById(groupId) } returns expectedGroup

        val result = useCase(groupId)

        assertEquals(expectedGroup, result)
        coVerify(exactly = 1) { repository.getGroupById(groupId) }
    }
}
