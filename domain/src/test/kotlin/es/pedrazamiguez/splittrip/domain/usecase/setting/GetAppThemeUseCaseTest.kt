package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetAppThemeUseCaseTest {

    private val repository: UserPreferenceRepository = mockk()
    private val useCase = GetAppThemeUseCase(repository)

    @Test
    fun `returns app theme flow from repository`() = runTest {
        val expectedTheme = "DARK"
        every { repository.getAppTheme() } returns flowOf(expectedTheme)

        val result = useCase().first()

        assertEquals(expectedTheme, result)
    }
}
