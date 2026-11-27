package es.pedrazamiguez.splittrip.data.litert

import android.content.Context
import com.google.android.play.core.splitcompat.SplitCompat
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener

internal class AndroidPlaySplitInstallClient(
    private val context: Context,
    private val splitInstallManager: SplitInstallManager
) : PlaySplitInstallClient {

    override fun getInstalledModules(): Set<String> = splitInstallManager.installedModules

    override fun registerListener(listener: (SplitInstallSessionSnapshot) -> Unit): () -> Unit {
        val playListener = SplitInstallStateUpdatedListener { state ->
            listener(
                SplitInstallSessionSnapshot(
                    sessionId = state.sessionId(),
                    status = state.status(),
                    errorCode = state.errorCode(),
                    bytesDownloaded = state.bytesDownloaded(),
                    totalBytesToDownload = state.totalBytesToDownload(),
                    moduleNames = state.moduleNames()
                )
            )
        }
        splitInstallManager.registerListener(playListener)
        return { splitInstallManager.unregisterListener(playListener) }
    }

    override fun startInstall(
        moduleName: String,
        onSuccess: (sessionId: Int) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        val request = SplitInstallRequest.newBuilder()
            .addModule(moduleName)
            .build()
        splitInstallManager.startInstall(request)
            .addOnSuccessListener(onSuccess)
            .addOnFailureListener(onFailure)
    }

    override fun cancelInstall(sessionId: Int) {
        runCatching { splitInstallManager.cancelInstall(sessionId) }
    }

    override fun deferredUninstall(moduleName: String) {
        runCatching { splitInstallManager.deferredUninstall(listOf(moduleName)) }
    }

    override fun installSplitCompat(): Boolean =
        runCatching { SplitCompat.install(context.applicationContext) }.getOrDefault(false)
}
