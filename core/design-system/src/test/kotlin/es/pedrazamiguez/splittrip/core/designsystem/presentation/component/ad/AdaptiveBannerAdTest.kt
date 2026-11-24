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

    @Test
    fun `calculateAdaptiveBannerWidthDp returns full dp width when horizontal insets are zero`() {
        val widthDp = calculateAdaptiveBannerWidthDp(
            containerWidthPx = 1080,
            horizontalInsetPx = 0,
            density = 3.0f
        )

        assertEquals(360, widthDp)
    }

    @Test
    fun `calculateAdaptiveBannerWidthDp subtracts horizontal cutout and nav bar insets in landscape`() {
        val widthDp = calculateAdaptiveBannerWidthDp(
            containerWidthPx = 2400,
            horizontalInsetPx = 240,
            density = 3.0f
        )

        assertEquals(720, widthDp)
    }

    @Test
    fun `calculateAdaptiveBannerWidthDp clamps to 1 when insets exceed container width`() {
        val widthDp = calculateAdaptiveBannerWidthDp(
            containerWidthPx = 100,
            horizontalInsetPx = 200,
            density = 2.0f
        )

        assertEquals(1, widthDp)
    }

    @Test
    fun `calculateAdaptiveBannerWidthDp clamps to 1 when density is zero or negative`() {
        val zeroDensityWidth = calculateAdaptiveBannerWidthDp(
            containerWidthPx = 1080,
            horizontalInsetPx = 0,
            density = 0f
        )
        val negativeDensityWidth = calculateAdaptiveBannerWidthDp(
            containerWidthPx = 1080,
            horizontalInsetPx = 0,
            density = -2.0f
        )

        assertEquals(1, zeroDensityWidth)
        assertEquals(1, negativeDensityWidth)
    }
}
