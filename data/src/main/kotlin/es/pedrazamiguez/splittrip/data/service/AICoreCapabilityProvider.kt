package es.pedrazamiguez.splittrip.data.service

import android.content.Context
import android.os.Build
import java.util.Locale

/**
 * Capability checker for Android AICore / Gemini Nano.
 */
internal class AICoreCapabilityProvider(
    private val context: Context
) {
    /**
     * Checks if the device has support for AICore.
     * Checks SDK level, manufacturer, and AICore system package presence.
     */
    fun isSupported(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return false
        }
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
        val isSupportedManufacturer = manufacturer.contains("google") || manufacturer.contains("samsung")
        if (!isSupportedManufacturer) {
            return false
        }
        return try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(AICORE_PACKAGE_NAME, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val AICORE_PACKAGE_NAME = "com.google.android.aicore"
    }
}
