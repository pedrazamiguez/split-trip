package es.pedrazamiguez.splittrip.domain.usecase.group

import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.CreateGroupUseCaseImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CreateGroupUseCaseTest {

    private lateinit var groupRepository: GroupRepository
    private lateinit var useCase: CreateGroupUseCase

    private val testGroup = Group(id = "", name = "Trip to Japan", currency = "JPY")

    @BeforeEach
    fun setUp() {
        groupRepository = mockk()
        useCase = CreateGroupUseCaseImpl(groupRepository)
    }

    @Nested
    inner class Invocation {

        @Test
        fun `returns Result success with group id on successful creation`() = runTest {
            // Given
            coEvery { groupRepository.createGroup(testGroup) } returns "generated-id-123"

            // When
            val result = useCase(testGroup)

            // Then
            assertTrue(result.isSuccess)
            assertEquals("generated-id-123", result.getOrNull())
        }

        @Test
        fun `wraps repository exception in Result failure`() = runTest {
            // Given
            coEvery { groupRepository.createGroup(testGroup) } throws RuntimeException("Network error")

            // When
            val result = useCase(testGroup)

            // Then
            assertTrue(result.isFailure)
            assertEquals("Network error", result.exceptionOrNull()?.message)
        }

        @Test
        fun `delegates to repository with the provided group`() = runTest {
            // Given
            coEvery { groupRepository.createGroup(any()) } returns "id-1"

            // When
            useCase(testGroup)

            // Then
            coVerify(exactly = 1) { groupRepository.createGroup(testGroup) }
        }
    }
}
