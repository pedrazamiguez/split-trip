package es.pedrazamiguez.splittrip.data.litert

import kotlinx.coroutines.flow.StateFlow

internal interface LiteRtDynamicRuntimeManager : AutoCloseable {
    fun observeState(): StateFlow<LiteRtDynamicRuntimeState>
    fun isRuntimeInstalled(): Boolean
    fun activateInstalledRuntime(): Boolean
    fun requestInstall()
    fun cancelInstall()
    fun requestDeferredUninstall()

    companion object {
        const val MODULE_NAME = "dynamic_litertlm"
    }
}
