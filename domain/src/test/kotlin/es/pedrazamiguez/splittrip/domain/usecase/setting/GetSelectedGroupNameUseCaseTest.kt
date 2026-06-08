package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.repository.GroupPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.GetSelectedGroupNameUseCaseImpl
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GetSelectedGroupNameUseCaseTest {

    private val repository: GroupPreferenceRepository = mockk()
    private val useCase = GetSelectedGroupNameUseCaseImpl(repository)

    @Test
    fun `returns group name flow from repository`() = runTest {
        val expectedName = "Summer Trip"
        every { repository.getSelectedGroupName() } returns flowOf(expectedName)

        val result = useCase().first()

        assertEquals(expectedName, result)
    }

    @Test
    fun `returns null when no group name stored`() = runTest {
        every { repository.getSelectedGroupName() } returns flowOf(null)

        val result = useCase().first()

        assertNull(result)
    }
}
