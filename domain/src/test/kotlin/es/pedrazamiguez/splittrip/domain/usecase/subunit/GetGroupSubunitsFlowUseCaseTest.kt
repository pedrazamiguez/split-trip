package es.pedrazamiguez.splittrip.domain.usecase.subunit

import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.usecase.subunit.impl.GetGroupSubunitsFlowUseCaseImpl
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("GetGroupSubunitsFlowUseCase")
class GetGroupSubunitsFlowUseCaseTest {

    private lateinit var subunitRepository: SubunitRepository
    private lateinit var useCase: GetGroupSubunitsFlowUseCase

    private val groupId = "group-123"

    @BeforeEach
    fun setUp() {
        subunitRepository = mockk()
        useCase = GetGroupSubunitsFlowUseCaseImpl(
            subunitRepository = subunitRepository
        )
    }

    @Test
    fun `delegates to repository getGroupSubunitsFlow`() {
        // Given
        val subunits = listOf(
            Subunit(id = "sub-1", name = "Couple", memberIds = listOf("user-1", "user-2"))
        )
        every { subunitRepository.getGroupSubunitsFlow(groupId) } returns flowOf(subunits)

        // When
        useCase(groupId)

        // Then
        verify(exactly = 1) { subunitRepository.getGroupSubunitsFlow(groupId) }
    }

    @Test
    fun `passes correct groupId to repository`() {
        // Given
        val specificGroupId = "specific-group-456"
        every { subunitRepository.getGroupSubunitsFlow(specificGroupId) } returns flowOf(emptyList())

        // When
        useCase(specificGroupId)

        // Then
        verify { subunitRepository.getGroupSubunitsFlow(specificGroupId) }
    }
}
