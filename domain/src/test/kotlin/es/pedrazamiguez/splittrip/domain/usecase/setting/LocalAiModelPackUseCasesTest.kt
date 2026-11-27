package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.exception.LocalAiModelNotEntitledException
import es.pedrazamiguez.splittrip.domain.exception.LocalAiModelUnavailableException
import es.pedrazamiguez.splittrip.domain.service.LocalAiModelPackService
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.CancelLocalAiModelPackDownloadUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.DeleteLocalAiModelPackUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.DownloadLocalAiModelPackUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.SetLocalAiModelPackWifiOnlyUseCaseImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LocalAiModelPackUseCasesTest {

    private val service: LocalAiModelPackService = mockk()

    @Test
    fun `DownloadLocalAiModelPackUseCase delegates to requestDownload`() = runTest {
        coEvery { service.requestDownload(null) } returns Result.success(Unit)
        val useCase = DownloadLocalAiModelPackUseCaseImpl(service)

        val result = useCase()

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { service.requestDownload(null) }
    }

    @Test
    fun `DownloadLocalAiModelPackUseCase forwards optional packId to requestDownload`() = runTest {
        coEvery { service.requestDownload("qwen3-0.6b-litert-v1") } coAnswers {
            yield()
            Result.success(Unit)
        }
        val useCase = DownloadLocalAiModelPackUseCaseImpl(service)

        val result = useCase("qwen3-0.6b-litert-v1")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { service.requestDownload("qwen3-0.6b-litert-v1") }
    }

    @Test
    fun `CancelLocalAiModelPackDownloadUseCase delegates to cancelDownload`() = runTest {
        coEvery { service.cancelDownload(null) } returns Result.success(Unit)
        val useCase = CancelLocalAiModelPackDownloadUseCaseImpl(service)

        val result = useCase()

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { service.cancelDownload(null) }
    }

    @Test
    fun `CancelLocalAiModelPackDownloadUseCase forwards optional packId to cancelDownload`() = runTest {
        coEvery { service.cancelDownload("gemma4-e2b-litert-v1") } coAnswers {
            yield()
            Result.success(Unit)
        }
        val useCase = CancelLocalAiModelPackDownloadUseCaseImpl(service)

        val result = useCase("gemma4-e2b-litert-v1")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { service.cancelDownload("gemma4-e2b-litert-v1") }
    }

    @Test
    fun `DeleteLocalAiModelPackUseCase delegates to deletePack`() = runTest {
        coEvery { service.deletePack() } returns Result.success(Unit)
        val useCase = DeleteLocalAiModelPackUseCaseImpl(service)

        val result = useCase()

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { service.deletePack() }
    }

    @Test
    fun `SetLocalAiModelPackWifiOnlyUseCase delegates to setWifiOnlyPreference`() = runTest {
        coEvery { service.setWifiOnlyPreference(false) } just runs
        val useCase = SetLocalAiModelPackWifiOnlyUseCaseImpl(service)

        useCase(false)

        coVerify(exactly = 1) { service.setWifiOnlyPreference(false) }
    }

    @Test
    fun `exceptions expose expected default messages`() {
        val notEntitled = LocalAiModelNotEntitledException()
        val unavailable = LocalAiModelUnavailableException(cause = IllegalStateException("io"))

        assertEquals("Local AI model pack requires an active Pro subscription", notEntitled.message)
        assertEquals("Local AI model pack is not installed or failed to load", unavailable.message)
        assertTrue(unavailable.cause is IllegalStateException)
    }
}
