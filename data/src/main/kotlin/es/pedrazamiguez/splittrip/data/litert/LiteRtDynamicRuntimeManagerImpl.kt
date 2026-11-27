package es.pedrazamiguez.splittrip.data.litert

import com.google.android.play.core.splitinstall.SplitInstallException
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import es.pedrazamiguez.splittrip.domain.model.ModelPackErrorReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

internal class LiteRtDynamicRuntimeManagerImpl(
    private val splitInstallClient: PlaySplitInstallClient,
    private val factoryLoader: LiteRtEngineClientFactoryLoader
) : LiteRtDynamicRuntimeManager {

    private val stateLock = Any()

    @Volatile
    private var activeSessionId: Int = NO_SESSION_ID

    private val _state: MutableStateFlow<LiteRtDynamicRuntimeState> = MutableStateFlow(
        if (isRuntimeInstalled()) {
            LiteRtDynamicRuntimeState.Installed
        } else {
            LiteRtDynamicRuntimeState.NotInstalled
        }
    )

    private val unregisterListener: () -> Unit = splitInstallClient.registerListener(::onSessionUpdate)

    override fun observeState(): StateFlow<LiteRtDynamicRuntimeState> = _state.asStateFlow()

    override fun isRuntimeInstalled(): Boolean {
        if (factoryLoader.isFactoryAvailable()) return true
        if (LiteRtDynamicRuntimeManager.MODULE_NAME in splitInstallClient.getInstalledModules()) {
            splitInstallClient.installSplitCompat()
            return factoryLoader.isFactoryAvailable()
        }
        return false
    }

    override fun activateInstalledRuntime(): Boolean {
        if (!factoryLoader.isFactoryAvailable()) {
            splitInstallClient.installSplitCompat()
        }
        return factoryLoader.isFactoryAvailable()
    }

    override fun requestInstall() {
        synchronized(stateLock) {
            if (isRuntimeInstalled()) {
                _state.value = LiteRtDynamicRuntimeState.Installed
                return
            }
            if (_state.value.isActiveInstall()) {
                return
            }
            _state.value = LiteRtDynamicRuntimeState.Pending
        }
        splitInstallClient.startInstall(
            moduleName = LiteRtDynamicRuntimeManager.MODULE_NAME,
            onSuccess = { sessionId ->
                synchronized(stateLock) {
                    if (_state.value !is LiteRtDynamicRuntimeState.Installed) {
                        activeSessionId = sessionId
                    }
                }
            },
            onFailure = ::handleStartInstallFailure
        )
    }

    override fun cancelInstall() {
        val sessionToCancel = synchronized(stateLock) {
            val id = activeSessionId
            activeSessionId = NO_SESSION_ID
            if (_state.value !is LiteRtDynamicRuntimeState.Installed) {
                _state.value = LiteRtDynamicRuntimeState.NotInstalled
            }
            id
        }
        if (sessionToCancel != NO_SESSION_ID) {
            splitInstallClient.cancelInstall(sessionToCancel)
        }
    }

    override fun requestDeferredUninstall() {
        runCatching {
            splitInstallClient.deferredUninstall(LiteRtDynamicRuntimeManager.MODULE_NAME)
        }.onFailure { throwable ->
            Timber.w(throwable, "Deferred uninstall request failed for %s", LiteRtDynamicRuntimeManager.MODULE_NAME)
        }
    }

    override fun close() {
        unregisterListener()
    }

    private fun onSessionUpdate(snapshot: SplitInstallSessionSnapshot) {
        if (LiteRtDynamicRuntimeManager.MODULE_NAME !in snapshot.moduleNames) return

        when (snapshot.status) {
            SplitInstallSessionStatus.PENDING ->
                _state.value = LiteRtDynamicRuntimeState.Pending

            SplitInstallSessionStatus.DOWNLOADING ->
                _state.value = LiteRtDynamicRuntimeState.Downloading(
                    bytesDownloaded = snapshot.bytesDownloaded,
                    totalBytesToDownload = snapshot.totalBytesToDownload
                )

            SplitInstallSessionStatus.DOWNLOADED,
            SplitInstallSessionStatus.INSTALLING ->
                _state.value = LiteRtDynamicRuntimeState.Installing

            SplitInstallSessionStatus.INSTALLED -> handleInstalledSession(snapshot.errorCode)

            SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> {
                synchronized(stateLock) { activeSessionId = NO_SESSION_ID }
                Timber.w(
                    "Play Feature Delivery requires user confirmation for %s",
                    LiteRtDynamicRuntimeManager.MODULE_NAME
                )
                _state.value = LiteRtDynamicRuntimeState.Failed(
                    reason = ModelPackErrorReason.RUNTIME_UNAVAILABLE,
                    errorCode = snapshot.errorCode
                )
            }

            SplitInstallSessionStatus.CANCELED,
            SplitInstallSessionStatus.CANCELING -> synchronized(stateLock) {
                activeSessionId = NO_SESSION_ID
                if (_state.value !is LiteRtDynamicRuntimeState.Installed) {
                    _state.value = LiteRtDynamicRuntimeState.NotInstalled
                }
            }

            SplitInstallSessionStatus.FAILED -> {
                synchronized(stateLock) { activeSessionId = NO_SESSION_ID }
                _state.value = LiteRtDynamicRuntimeState.Failed(
                    reason = mapErrorCode(snapshot.errorCode),
                    errorCode = snapshot.errorCode
                )
            }
        }
    }

    private fun handleInstalledSession(errorCode: Int) {
        synchronized(stateLock) { activeSessionId = NO_SESSION_ID }
        splitInstallClient.installSplitCompat()
        if (factoryLoader.isFactoryAvailable()) {
            _state.value = LiteRtDynamicRuntimeState.Installed
        } else {
            Timber.e(
                "Split module %s reported INSTALLED but SPI factory could not be loaded",
                LiteRtDynamicRuntimeManager.MODULE_NAME
            )
            _state.value = LiteRtDynamicRuntimeState.Failed(
                reason = ModelPackErrorReason.RUNTIME_UNAVAILABLE,
                errorCode = errorCode
            )
        }
    }

    private fun handleStartInstallFailure(throwable: Throwable) {
        val errorCode = (throwable as? SplitInstallException)?.errorCode ?: SplitInstallErrorCode.INTERNAL_ERROR
        synchronized(stateLock) { activeSessionId = NO_SESSION_ID }
        Timber.w(
            throwable,
            "Failed to start Play Feature Delivery install for %s (code=%d)",
            LiteRtDynamicRuntimeManager.MODULE_NAME,
            errorCode
        )
        _state.value = LiteRtDynamicRuntimeState.Failed(
            reason = mapErrorCode(errorCode),
            errorCode = errorCode
        )
    }

    private fun mapErrorCode(errorCode: Int): ModelPackErrorReason = when (errorCode) {
        SplitInstallErrorCode.NETWORK_ERROR -> ModelPackErrorReason.NETWORK_ERROR
        SplitInstallErrorCode.INSUFFICIENT_STORAGE -> ModelPackErrorReason.INSUFFICIENT_STORAGE
        else -> ModelPackErrorReason.RUNTIME_UNAVAILABLE
    }

    private companion object {
        private const val NO_SESSION_ID = -1
    }
}
