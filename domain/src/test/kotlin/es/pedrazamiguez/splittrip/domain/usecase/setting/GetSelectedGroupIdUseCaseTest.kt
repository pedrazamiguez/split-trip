package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.repository.GroupPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.GetSelectedGroupIdUseCaseImpl
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GetSelectedGroupIdUseCaseTest {

    private val repository: GroupPreferenceRepository = mockk()
    private val useCase = GetSelectedGroupIdUseCaseImpl(repository)

    @Test
    fun `returns group id flow from repository`() = runTest {
        val expectedGroupId = "group-123"
        every { repository.getSelectedGroupId() } returns flowOf(expectedGroupId)

        val result = useCase().first()

        assertEquals(expectedGroupId, result)
    }

    @Test
    fun `returns null when no group selected`() = runTest {
        every { repository.getSelectedGroupId() } returns flowOf(null)

        val result = useCase().first()

        assertNull(result)
    }
}
