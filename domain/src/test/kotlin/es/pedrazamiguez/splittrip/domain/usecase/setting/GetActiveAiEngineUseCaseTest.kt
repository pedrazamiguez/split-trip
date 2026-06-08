package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.GetActiveAiEngineUseCaseImpl
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetActiveAiEngineUseCaseTest {

    private val repository: UserPreferenceRepository = mockk()
    private val useCase = GetActiveAiEngineUseCaseImpl(repository)

    @Test
    fun `returns active AI engine flow from repository`() = runTest {
        val expectedEngine = AiEngineType.AI_CORE_GEMMA_4
        every { repository.getActiveAiEngine() } returns flowOf(expectedEngine)

        val result = useCase().first()

        assertEquals(expectedEngine, result)
    }

    @Test
    fun `returns updated active AI engine when preference changes`() = runTest {
        val expectedEngine = AiEngineType.LITE_RT_LM
        every { repository.getActiveAiEngine() } returns flowOf(expectedEngine)

        val result = useCase().first()

        assertEquals(expectedEngine, result)
    }
}
