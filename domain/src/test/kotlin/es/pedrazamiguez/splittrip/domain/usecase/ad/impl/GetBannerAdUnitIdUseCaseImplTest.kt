package es.pedrazamiguez.splittrip.domain.usecase.ad.impl

import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetBannerAdUnitIdUseCaseImplTest {

    @Test
    fun `invoke returns admobBannerAdUnitId from repository`() {
        val bannerIdFlow = MutableStateFlow("test-banner-id")
        val appConfigRepository = mockk<AppConfigRepository> {
            every { admobBannerAdUnitId } returns bannerIdFlow
        }
        val useCase = GetBannerAdUnitIdUseCaseImpl(appConfigRepository)

        assertEquals("test-banner-id", useCase().value)
    }
}
