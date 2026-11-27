package es.pedrazamiguez.splittrip.data.litert

internal interface PlaySplitInstallClient {
    fun getInstalledModules(): Set<String>
    fun registerListener(listener: (SplitInstallSessionSnapshot) -> Unit): () -> Unit
    fun startInstall(
        moduleName: String,
        onSuccess: (sessionId: Int) -> Unit,
        onFailure: (Throwable) -> Unit
    )
    fun cancelInstall(sessionId: Int)
    fun deferredUninstall(moduleName: String)
    fun installSplitCompat(): Boolean
}

internal data class SplitInstallSessionSnapshot(
    val sessionId: Int,
    val status: Int,
    val errorCode: Int,
    val bytesDownloaded: Long,
    val totalBytesToDownload: Long,
    val moduleNames: List<String>
)
