package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.ad

import com.google.android.gms.ads.LoadAdError
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AdaptiveBannerAdTest {

    @Test
    fun `onAdLoaded invokes onLoaded callback`() {
        var called = false
        val listener = createAdaptiveBannerAdListener(
            onLoaded = { called = true },
            onImpression = {},
            onFailedToLoad = {}
        )

        listener.onAdLoaded()

        assertTrue(called)
    }

    @Test
    fun `onAdImpression invokes onImpression callback`() {
        var called = false
        val listener = createAdaptiveBannerAdListener(
            onLoaded = {},
            onImpression = { called = true },
            onFailedToLoad = {}
        )

        listener.onAdImpression()

        assertTrue(called)
    }

    @Test
    fun `onAdFailedToLoad invokes onFailedToLoad callback with LoadAdError code`() {
        var receivedCode: Int? = null
        val loadAdError = mockk<LoadAdError> {
            every { code } returns 3
            every { message } returns "No fill"
        }
        val listener = createAdaptiveBannerAdListener(
            onLoaded = {},
            onImpression = {},
            onFailedToLoad = { receivedCode = it }
        )

        listener.onAdFailedToLoad(loadAdError)

        assertEquals(3, receivedCode)
    }
}
