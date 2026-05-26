package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SetActiveAiEngineUseCaseTest {

    private val repository: UserPreferenceRepository = mockk(relaxed = true)
    private val useCase = SetActiveAiEngineUseCase(repository)

    @Test
    fun `delegates to repository with AI_CORE_GEMMA_4`() = runTest {
        val engineType = AiEngineType.AI_CORE_GEMMA_4

        useCase(engineType)

        coVerify { repository.setActiveAiEngine(engineType) }
    }

    @Test
    fun `delegates to repository with LITE_RT_LM`() = runTest {
        val engineType = AiEngineType.LITE_RT_LM

        useCase(engineType)

        coVerify { repository.setActiveAiEngine(engineType) }
    }
}
