package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetAppLanguageUseCaseTest {

    private val repository: UserPreferenceRepository = mockk()
    private val useCase = GetAppLanguageUseCase(repository)

    @Test
    fun `returns app language flow from repository`() = runTest {
        val expectedLanguage = "es"
        every { repository.getAppLanguage() } returns flowOf(expectedLanguage)

        val result = useCase().first()

        assertEquals(expectedLanguage, result)
    }
}
