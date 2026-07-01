package es.pedrazamiguez.splittrip.domain.usecase.group

import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.exception.GroupArchivedException
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.DeleteGroupUseCaseImpl
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DeleteGroupUseCaseTest {

    private lateinit var groupRepository: GroupRepository
    private lateinit var useCase: DeleteGroupUseCase

    @BeforeEach
    fun setUp() {
        groupRepository = mockk()
        useCase = DeleteGroupUseCaseImpl(groupRepository)
    }

    @Nested
    inner class Invocation {

        @Test
        fun `delegates to repository deleteGroup`() = runTest {
            // Given
            val groupId = "group-123"
            val activeGroup = Group(id = groupId, name = "Test", currency = "EUR")
            coEvery { groupRepository.getGroupById(groupId) } returns activeGroup
            coEvery { groupRepository.deleteGroup(groupId) } just Runs

            // When
            useCase(groupId)

            // Then
            coVerify(exactly = 1) { groupRepository.deleteGroup(groupId) }
        }

        @Test
        fun `passes correct groupId to repository`() = runTest {
            // Given
            val groupId = "specific-group-id-456"
            val activeGroup = Group(id = groupId, name = "Test", currency = "EUR")
            coEvery { groupRepository.getGroupById(groupId) } returns activeGroup
            coEvery { groupRepository.deleteGroup(any()) } just Runs

            // When
            useCase(groupId)

            // Then
            coVerify { groupRepository.deleteGroup(groupId) }
        }

        @Test
        fun `propagates exception from repository`() = runTest {
            // Given
            val groupId = "group-123"
            val activeGroup = Group(id = groupId, name = "Test", currency = "EUR")
            coEvery { groupRepository.getGroupById(groupId) } returns activeGroup
            val exception = RuntimeException("Delete failed")
            coEvery { groupRepository.deleteGroup(groupId) } throws exception

            // When/Then
            try {
                useCase(groupId)
                fail("Expected exception to be thrown")
            } catch (e: RuntimeException) {
                assertEquals("Delete failed", e.message)
            }
        }
    }

    @Nested
    inner class ArchiveGuard {

        @Test
        fun `throws GroupArchivedException when group is archived`() = runTest {
            // Given
            val groupId = "archived-group-id"
            val archivedGroup = Group(id = groupId, name = "Archived", currency = "EUR", status = GroupStatus.ARCHIVED)
            coEvery { groupRepository.getGroupById(groupId) } returns archivedGroup

            // When/Then
            try {
                useCase(groupId)
                fail("Expected GroupArchivedException to be thrown")
            } catch (e: GroupArchivedException) {
                assertEquals(groupId, e.groupId)
            }
        }

        @Suppress("SwallowedException")
        @Test
        fun `does not call deleteGroup when group is archived`() = runTest {
            // Given
            val groupId = "archived-group-id"
            val archivedGroup = Group(id = groupId, name = "Archived", currency = "EUR", status = GroupStatus.ARCHIVED)
            coEvery { groupRepository.getGroupById(groupId) } returns archivedGroup

            // When/Then
            try {
                useCase(groupId)
                fail("Expected GroupArchivedException to be thrown")
            } catch (e: GroupArchivedException) {
                coVerify(exactly = 0) { groupRepository.deleteGroup(any()) }
            }
        }

        @Test
        fun `throws IllegalArgumentException when group does not exist`() = runTest {
            // Given
            val groupId = "non-existent-id"
            coEvery { groupRepository.getGroupById(groupId) } returns null

            // When/Then
            try {
                useCase(groupId)
                fail("Expected IllegalArgumentException to be thrown")
            } catch (e: IllegalArgumentException) {
                assertEquals("Group not found with id: $groupId", e.message)
            }
        }
    }
}
