package es.pedrazamiguez.splittrip.domain.usecase.group

import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.repository.GroupPreferenceRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.ObserveSelectedGroupUseCaseImpl
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ObserveSelectedGroupUseCaseImplTest {

    private val groupPreferenceRepository: GroupPreferenceRepository = mockk()
    private val groupRepository: GroupRepository = mockk()
    private val useCase = ObserveSelectedGroupUseCaseImpl(groupPreferenceRepository, groupRepository)

    @Test
    fun `returns null flow when no group is selected`() = runTest {
        every { groupPreferenceRepository.getSelectedGroupId() } returns flowOf(null)

        val result = useCase().firstOrNull()

        assertNull(result)
        verify(exactly = 1) { groupPreferenceRepository.getSelectedGroupId() }
        verify(exactly = 0) { groupRepository.getGroupByIdFlow(any()) }
    }

    @Test
    fun `returns group flow when group is selected`() = runTest {
        val groupId = "group-123"
        val expectedGroup = Group(id = groupId, name = "Test Group")
        every { groupPreferenceRepository.getSelectedGroupId() } returns flowOf(groupId)
        every { groupRepository.getGroupByIdFlow(groupId) } returns flowOf(expectedGroup)

        val result = useCase().firstOrNull()

        assertEquals(expectedGroup, result)
        verify(exactly = 1) { groupPreferenceRepository.getSelectedGroupId() }
        verify(exactly = 1) { groupRepository.getGroupByIdFlow(groupId) }
    }
}
