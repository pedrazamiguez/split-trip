package es.pedrazamiguez.splittrip.update

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AppUpdateUiStateTest {

    @Test
    fun `returns null progressFraction and progressPercent when totalBytesToDownload is zero or negative`() {
        val zeroTotal = AppUpdateUiState.FlexibleUpdateDownloading(
            bytesDownloaded = 500L,
            totalBytesToDownload = 0L
        )
        val negativeTotal = AppUpdateUiState.FlexibleUpdateDownloading(
            bytesDownloaded = 500L,
            totalBytesToDownload = -10L
        )

        assertNull(zeroTotal.progressFraction)
        assertNull(zeroTotal.progressPercent)
        assertNull(negativeTotal.progressFraction)
        assertNull(negativeTotal.progressPercent)
    }

    @Test
    fun `computes exact BigDecimal progressFraction and HALF_UP progressPercent for valid byte counts`() {
        val zero = AppUpdateUiState.FlexibleUpdateDownloading(0L, 1_000L)
        assertEquals(0f, zero.progressFraction)
        assertEquals(0, zero.progressPercent)

        val half = AppUpdateUiState.FlexibleUpdateDownloading(500L, 1_000L)
        assertEquals(0.5f, half.progressFraction)
        assertEquals(50, half.progressPercent)

        val oneThird = AppUpdateUiState.FlexibleUpdateDownloading(1L, 3L)
        assertEquals(0.3333f, oneThird.progressFraction)
        assertEquals(33, oneThird.progressPercent)

        val twoThirds = AppUpdateUiState.FlexibleUpdateDownloading(2L, 3L)
        assertEquals(0.6667f, twoThirds.progressFraction)
        assertEquals(67, twoThirds.progressPercent)

        val full = AppUpdateUiState.FlexibleUpdateDownloading(1_000L, 1_000L)
        assertEquals(1f, full.progressFraction)
        assertEquals(100, full.progressPercent)
    }

    @Test
    fun `clamps negative and overflowing bytesDownloaded to valid 0 to 1 and 0 to 100 bounds`() {
        val negative = AppUpdateUiState.FlexibleUpdateDownloading(-250L, 1_000L)
        assertEquals(0f, negative.progressFraction)
        assertEquals(0, negative.progressPercent)

        val overflowing = AppUpdateUiState.FlexibleUpdateDownloading(1_500L, 1_000L)
        assertEquals(1f, overflowing.progressFraction)
        assertEquals(100, overflowing.progressPercent)
    }

    @Test
    fun `handles large Long byte counts without integer overflow`() {
        val total = 10_000_000_000L
        val downloaded = 7_500_000_000L
        val state = AppUpdateUiState.FlexibleUpdateDownloading(downloaded, total)

        assertEquals(0.75f, state.progressFraction)
        assertEquals(75, state.progressPercent)
    }
}
