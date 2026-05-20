package es.pedrazamiguez.splittrip.data.service

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.util.Locale

/**
 * Capability checker for Android AICore / Gemini Nano.
 *
 * The support check is memoized after the first call because `getPackageInfo` performs
 * disk I/O and the installed package set does not change within a process lifetime.
 */
internal class AICoreCapabilityProvider(
    private val context: Context
) {
    @Volatile private var cachedIsSupported: Boolean? = null

    fun isSupported(): Boolean = cachedIsSupported ?: synchronized(this) {
        cachedIsSupported ?: computeIsSupported().also { cachedIsSupported = it }
    }

    private fun computeIsSupported(): Boolean {
        val sdkSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
        val manufacturerSupported = manufacturer.contains("google") || manufacturer.contains("samsung")
        if (!sdkSupported || !manufacturerSupported) return false
        return try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(AICORE_PACKAGE_NAME, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    companion object {
        private const val AICORE_PACKAGE_NAME = "com.google.android.aicore"
    }
}
