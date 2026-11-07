package es.pedrazamiguez.splittrip.domain.usecase.group.impl

import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("GetGroupByIdLocalUseCaseImpl")
class GetGroupByIdLocalUseCaseImplTest {

    private val groupRepository = mockk<GroupRepository>()
    private lateinit var useCase: GetGroupByIdLocalUseCaseImpl

    @BeforeEach
    fun setUp() {
        useCase = GetGroupByIdLocalUseCaseImpl(groupRepository)
    }

    @Test
    fun `invoke delegates to groupRepository getGroupByIdLocal`() = runTest {
        val group = Group(id = "group-1", name = "Test Group")
        coEvery { groupRepository.getGroupByIdLocal("group-1") } returns group

        val result = useCase("group-1")

        assertEquals(group, result)
        coVerify(exactly = 1) { groupRepository.getGroupByIdLocal("group-1") }
    }

    @Test
    fun `invoke returns null when group not found locally`() = runTest {
        coEvery { groupRepository.getGroupByIdLocal("group-missing") } returns null

        val result = useCase("group-missing")

        assertNull(result)
        coVerify(exactly = 1) { groupRepository.getGroupByIdLocal("group-missing") }
    }
}
