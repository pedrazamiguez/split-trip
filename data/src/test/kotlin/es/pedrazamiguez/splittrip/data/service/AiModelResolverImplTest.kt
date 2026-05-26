package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.data.local.datastore.UserPreferences
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiModelResolverImplTest {

    private lateinit var userPreferences: UserPreferences
    private lateinit var resolver: AiModelResolverImpl

    @BeforeEach
    fun setUp() {
        userPreferences = mockk()
        resolver = AiModelResolverImpl(
            userPreferences = userPreferences
        )
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
    }

    @Test
    fun `getActiveModel returns override value when configured`() = runTest {
        every { userPreferences.activeAiModel } returns flowOf(AiEngineType.LITE_RT_LM.name)

        val activeModel = resolver.getActiveModel().first()

        assertEquals(AiEngineType.LITE_RT_LM, activeModel)
    }

    @Test
    fun `getActiveModel falls back to AI_CORE_GEMMA_4 when override is null`() = runTest {
        every { userPreferences.activeAiModel } returns flowOf(null)

        val activeModel = resolver.getActiveModel().first()

        assertEquals(AiEngineType.AI_CORE_GEMMA_4, activeModel)
    }

    @Test
    fun `getDeveloperOverrideModel returns override value`() = runTest {
        every { userPreferences.activeAiModel } returns flowOf(AiEngineType.AI_CORE_GEMMA_4.name)

        val override = resolver.getDeveloperOverrideModel().first()

        assertEquals(AiEngineType.AI_CORE_GEMMA_4, override)
    }

    @Test
    fun `getDeveloperOverrideModel returns null when override is not set`() = runTest {
        every { userPreferences.activeAiModel } returns flowOf(null)

        val override = resolver.getDeveloperOverrideModel().first()

        assertNull(override)
    }

    @Test
    fun `setDeveloperOverrideModel sets value in preferences`() = runTest {
        coEvery { userPreferences.setActiveAiModel(any()) } returns Unit

        resolver.setDeveloperOverrideModel(AiEngineType.LITE_RT_LM)

        coVerify { userPreferences.setActiveAiModel(AiEngineType.LITE_RT_LM.name) }
    }

    @Test
    fun `setDeveloperOverrideModel clears value in preferences when passing null`() = runTest {
        coEvery { userPreferences.setActiveAiModel(any()) } returns Unit

        resolver.setDeveloperOverrideModel(null)

        coVerify { userPreferences.setActiveAiModel(null) }
    }
}
