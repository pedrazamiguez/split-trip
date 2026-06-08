package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.GetShouldShowLanguagePillUseCaseImpl
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetShouldShowLanguagePillUseCaseTest {

    private val repository: UserPreferenceRepository = mockk()
    private val useCase = GetShouldShowLanguagePillUseCaseImpl(repository)

    @Test
    fun `returns show pill flow from repository`() = runTest {
        every { repository.getShouldShowLanguagePill() } returns flowOf(true)

        val result = useCase().first()

        assertTrue(result)
    }
}
