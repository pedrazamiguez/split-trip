package es.pedrazamiguez.splittrip.data.service

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AICoreCapabilityProviderTest {

    private lateinit var context: Context
    private lateinit var packageManager: PackageManager

    @BeforeEach
    fun setUp() {
        context = mockk()
        packageManager = mockk()
        every { context.packageManager } returns packageManager
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
    }

    @Test
    fun `isSupported returns false when SDK is below UpsideDownCake`() {
        val provider = AICoreCapabilityProvider(
            context = context,
            sdkInt = Build.VERSION_CODES.M,
            manufacturer = "Google"
        )
        assertFalse(provider.isSupported())
    }

    @Test
    fun `isSupported returns false when manufacturer is not Google or Samsung`() {
        val provider = AICoreCapabilityProvider(
            context = context,
            sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
            manufacturer = "Xiaomi"
        )
        assertFalse(provider.isSupported())
    }

    @Test
    fun `isSupported returns false when package is not installed`() {
        val provider = AICoreCapabilityProvider(
            context = context,
            sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
            manufacturer = "Samsung"
        )
        every { packageManager.getPackageInfo("com.google.android.aicore", 0) } throws
            PackageManager.NameNotFoundException()

        assertFalse(provider.isSupported())
    }

    @Test
    fun `isSupported returns true when SDK, manufacturer and package are all valid`() {
        val provider = AICoreCapabilityProvider(
            context = context,
            sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
            manufacturer = "Google"
        )
        every { packageManager.getPackageInfo("com.google.android.aicore", 0) } returns mockk<PackageInfo>()

        assertTrue(provider.isSupported())
    }

    @Test
    fun `isSupported caches the result after first call`() {
        val provider = AICoreCapabilityProvider(
            context = context,
            sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
            manufacturer = "Google"
        )
        every { packageManager.getPackageInfo("com.google.android.aicore", 0) } returns mockk<PackageInfo>()

        assertTrue(provider.isSupported())
        assertTrue(provider.isSupported())

        // Verify packageInfo was only checked once
        verify(exactly = 1) { packageManager.getPackageInfo("com.google.android.aicore", 0) }
    }
}
