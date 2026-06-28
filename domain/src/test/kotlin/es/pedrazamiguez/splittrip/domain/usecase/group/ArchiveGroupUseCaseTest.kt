package es.pedrazamiguez.splittrip.domain.usecase.group

import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.ArchiveGroupUseCaseImpl
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ArchiveGroupUseCaseTest {

    private lateinit var groupRepository: GroupRepository
    private lateinit var useCase: ArchiveGroupUseCase

    private val sampleGroup = Group(
        id = "group-123",
        name = "Trip to Paris",
        currency = "EUR",
        members = listOf("user-1", "user-2"),
        status = GroupStatus.ACTIVE,
        createdBy = "user-1",
        createdAt = LocalDateTime.now(),
        lastUpdatedAt = LocalDateTime.now()
    )

    @BeforeEach
    fun setUp() {
        groupRepository = mockk()
        useCase = ArchiveGroupUseCaseImpl(groupRepository)
    }

    @Nested
    inner class Invocation {

        @Test
        fun `archives active group and calls repository updateGroup`() = runTest {
            // Given
            coEvery { groupRepository.getGroupById("group-123") } returns sampleGroup
            coEvery { groupRepository.updateGroup(any()) } just Runs

            // When
            val result = useCase("group-123")

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                groupRepository.updateGroup(
                    match {
                        it.id == "group-123" && it.status == GroupStatus.ARCHIVED
                    }
                )
            }
        }

        @Test
        fun `returns failure result when group does not exist`() = runTest {
            // Given
            coEvery { groupRepository.getGroupById("invalid-id") } returns null

            // When
            val result = useCase("invalid-id")

            // Then
            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is IllegalArgumentException)
            assertEquals("Group not found with id: invalid-id", exception?.message)
            coVerify(exactly = 0) { groupRepository.updateGroup(any()) }
        }

        @Test
        fun `returns failure result when repository updateGroup throws exception`() = runTest {
            // Given
            coEvery { groupRepository.getGroupById("group-123") } returns sampleGroup
            val repoException = RuntimeException("DB update error")
            coEvery { groupRepository.updateGroup(any()) } throws repoException

            // When
            val result = useCase("group-123")

            // Then
            assertTrue(result.isFailure)
            assertEquals(repoException, result.exceptionOrNull())
        }
    }
}
