package es.pedrazamiguez.splittrip.domain.usecase.group

import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.UpdateGroupUseCaseImpl
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UpdateGroupUseCaseTest {

    private lateinit var groupRepository: GroupRepository
    private lateinit var useCase: UpdateGroupUseCase

    private val testGroup = Group(id = "group-123", name = "Trip to Japan", currency = "JPY")

    @BeforeEach
    fun setUp() {
        groupRepository = mockk()
        useCase = UpdateGroupUseCaseImpl(groupRepository = groupRepository)
    }

    @Nested
    inner class Invocation {

        @Test
        fun `returns Result success on successful update`() = runTest {
            // Given
            coEvery { groupRepository.updateGroup(testGroup) } just Runs

            // When
            val result = useCase(testGroup)

            // Then
            assertTrue(result.isSuccess)
        }

        @Test
        fun `wraps repository exception in Result failure`() = runTest {
            // Given
            coEvery { groupRepository.updateGroup(testGroup) } throws RuntimeException("Network error")

            // When
            val result = useCase(testGroup)

            // Then
            assertTrue(result.isFailure)
            assertEquals("Network error", result.exceptionOrNull()?.message)
        }

        @Test
        fun `delegates to repository with the provided group`() = runTest {
            // Given
            coEvery { groupRepository.updateGroup(any()) } just Runs

            // When
            useCase(testGroup)

            // Then
            coVerify(exactly = 1) { groupRepository.updateGroup(testGroup) }
        }
    }
}
