package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.repository.OnboardingPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.IsOnboardingCompleteUseCaseImpl
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IsOnboardingCompleteUseCaseTest {

    private val repository: OnboardingPreferenceRepository = mockk()
    private val useCase = IsOnboardingCompleteUseCaseImpl(repository)

    @Test
    fun `returns true when onboarding is complete`() = runTest {
        every { repository.isOnboardingComplete() } returns flowOf(true)

        val result = useCase().first()

        assertTrue(result)
    }

    @Test
    fun `returns false when onboarding is not complete`() = runTest {
        every { repository.isOnboardingComplete() } returns flowOf(false)

        val result = useCase().first()

        assertFalse(result)
    }
}
