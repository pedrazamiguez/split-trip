package es.pedrazamiguez.splittrip.core.logging.exit

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

class AndroidProcessExitInfoProvider(
    private val context: Context,
    private val sdkInt: Int = Build.VERSION.SDK_INT
) : ProcessExitInfoProvider {

    override fun getRecentAbnormalExits(maxResults: Int): List<ProcessExitRecord> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || sdkInt < Build.VERSION_CODES.R) {
            return emptyList()
        }
        return queryHistoricalExits(maxResults)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun queryHistoricalExits(maxResults: Int): List<ProcessExitRecord> {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return emptyList()

        val exits = runCatching {
            activityManager.getHistoricalProcessExitReasons(context.packageName, 0, maxResults)
        }.getOrDefault(emptyList())

        return exits.mapNotNull { info -> mapAbnormalExit(info) }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun mapAbnormalExit(info: ApplicationExitInfo): ProcessExitRecord? {
        val reason = when (info.reason) {
            ApplicationExitInfo.REASON_LOW_MEMORY -> AbnormalExitReason.LOW_MEMORY
            ApplicationExitInfo.REASON_CRASH_NATIVE -> AbnormalExitReason.CRASH_NATIVE
            ApplicationExitInfo.REASON_SIGNALED -> AbnormalExitReason.SIGNALED
            else -> return null
        }
        return ProcessExitRecord(
            reason = reason,
            timestampMillis = info.timestamp,
            pid = info.pid,
            status = info.status,
            importance = info.importance,
            pssKb = info.pss,
            rssKb = info.rss,
            processName = info.processName,
            description = info.description
        )
    }
}
