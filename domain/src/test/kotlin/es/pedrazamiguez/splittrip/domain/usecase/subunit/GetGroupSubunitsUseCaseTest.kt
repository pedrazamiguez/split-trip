package es.pedrazamiguez.splittrip.domain.usecase.subunit

import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetGroupSubunitsUseCaseTest {

    private val repository: SubunitRepository = mockk()
    private val useCase = GetGroupSubunitsUseCase(repository)

    @Test
    fun `invoke delegates to repository`() = runTest {
        val groupId = "group_123"
        val expectedList: List<Subunit> = emptyList()
        coEvery { repository.getGroupSubunits(groupId) } returns expectedList

        val result = useCase(groupId)

        assertEquals(expectedList, result)
        coVerify(exactly = 1) { repository.getGroupSubunits(groupId) }
    }
}
