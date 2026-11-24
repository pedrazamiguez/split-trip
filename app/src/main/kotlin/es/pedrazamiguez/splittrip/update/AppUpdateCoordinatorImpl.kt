package es.pedrazamiguez.splittrip.update

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.install.model.ActivityResult as PlayActivityResult
import es.pedrazamiguez.splittrip.core.common.provider.AppMetadataProvider
import es.pedrazamiguez.splittrip.core.logging.LogTag
import es.pedrazamiguez.splittrip.domain.model.AppUpdateDecision
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.usecase.update.RecordOptionalUpdateDismissalUseCase
import es.pedrazamiguez.splittrip.domain.usecase.update.ResolveAppUpdateDecisionUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

class AppUpdateCoordinatorImpl(
    private val playAppUpdateClient: PlayAppUpdateClient,
    private val resolveAppUpdateDecisionUseCase: ResolveAppUpdateDecisionUseCase,
    private val recordOptionalUpdateDismissalUseCase: RecordOptionalUpdateDismissalUseCase,
    private val appConfigService: AppConfigService,
    private val appMetadataProvider: AppMetadataProvider,
    private val isDebugBuild: Boolean,
    private val remoteConfigSettleTimeoutMs: Long = DEFAULT_REMOTE_CONFIG_SETTLE_TIMEOUT_MS
) : AppUpdateCoordinator {

    private val operationMutex = Mutex()

    private val _uiState = MutableStateFlow<AppUpdateUiState>(AppUpdateUiState.Idle)
    override val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()

    private val _actions = MutableSharedFlow<AppUpdateCoordinatorAction>(extraBufferCapacity = 1)
    override val actions: SharedFlow<AppUpdateCoordinatorAction> = _actions.asSharedFlow()

    @Volatile
    private var activeFlowType: PlayUpdateFlowType? = null
    private var activeAvailableVersionCode: Long? = null
    private var isForcedRetryInFlight: Boolean = false
    private var hasCompletedOptionalCheckThisSession: Boolean = false
    private var hasLoggedEnvironmentSkip: Boolean = false

    override fun registerInstallListener(): () -> Unit =
        playAppUpdateClient.registerInstallStatusListener { state ->
            if (activeFlowType == PlayUpdateFlowType.IMMEDIATE) {
                return@registerInstallStatusListener
            }
            _uiState.value = reduceUiStateForInstallState(_uiState.value, state)
        }

    override suspend fun checkForUpdates(
        allowOptionalPrompt: Boolean,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        if (shouldSkipUpdateChecks()) return
        if (allowOptionalPrompt && hasCompletedOptionalCheckThisSession) return

        // Wait outside mutex so onResume is never blocked on network config settle.
        withTimeoutOrNull(remoteConfigSettleTimeoutMs) {
            appConfigService.isInitialFetchSettled.first { it }
        }

        operationMutex.withLock {
            if (_uiState.value is AppUpdateUiState.ForcedUpdateBlocked || activeFlowType != null) {
                return@withLock
            }
            if (allowOptionalPrompt && hasCompletedOptionalCheckThisSession) {
                return@withLock
            }
            val snapshot = playAppUpdateClient.fetchUpdateSnapshot().getOrElse { error ->
                Timber.tag(LogTag.APP_UPDATE).d(error, "Skipping update check: Play unavailable")
                return@withLock
            }
            evaluateSnapshot(snapshot, allowOptionalPrompt, launcher)
        }
    }

    override suspend fun onResume(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        if (shouldSkipUpdateChecks()) return

        operationMutex.withLock {
            val snapshot = playAppUpdateClient.fetchUpdateSnapshot().getOrElse { error ->
                Timber.tag(LogTag.APP_UPDATE).d(error, "Skipping onResume check: Play unavailable")
                return@withLock
            }
            when {
                snapshot.availability == PlayUpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ->
                    launchImmediateFlow(
                        versionCode = snapshot.availableVersionCode,
                        launcher = launcher,
                        fallbackToPlayStoreOnError = false
                    )
                _uiState.value is AppUpdateUiState.ForcedUpdateBlocked ->
                    handleForcedGateOnResume(snapshot)
                else ->
                    _uiState.value = reduceUiStateForInstallState(_uiState.value, snapshot.toInstallState())
            }
        }
    }

    override suspend fun onActivityResult(resultCode: Int) {
        operationMutex.withLock {
            val flowType = activeFlowType
            val versionCode = activeAvailableVersionCode
            val wasForcedRetry = isForcedRetryInFlight
            activeFlowType = null
            activeAvailableVersionCode = null
            isForcedRetryInFlight = false

            when (flowType) {
                PlayUpdateFlowType.IMMEDIATE ->
                    handleImmediateResult(resultCode, versionCode, wasForcedRetry)
                PlayUpdateFlowType.FLEXIBLE ->
                    handleFlexibleActivityResult(resultCode, versionCode, recordOptionalUpdateDismissalUseCase)
                null -> Unit
            }
        }
    }

    override suspend fun retryForcedUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        if (shouldSkipUpdateChecks()) return

        operationMutex.withLock {
            val current = _uiState.value as? AppUpdateUiState.ForcedUpdateBlocked ?: return@withLock
            _uiState.value = current.copy(isRetrying = true)

            val snapshot = playAppUpdateClient.fetchUpdateSnapshot().getOrElse { error ->
                Timber.tag(LogTag.APP_UPDATE).d(error, "Forced update retry fetch failed")
                _uiState.value = current.copy(isRetrying = false)
                _actions.tryEmit(AppUpdateCoordinatorAction.OpenPlayStoreListing)
                return@withLock
            }

            retryFromFreshSnapshot(snapshot, current, launcher)
        }
    }

    override suspend fun completeFlexibleUpdate() {
        playAppUpdateClient.completeFlexibleUpdate().onFailure { error ->
            Timber.tag(LogTag.APP_UPDATE).d(error, "Failed to complete flexible update")
            if (_uiState.value is AppUpdateUiState.FlexibleUpdateReadyToInstall) {
                _uiState.value = AppUpdateUiState.Idle
            }
        }
    }

    private fun shouldSkipUpdateChecks(): Boolean {
        val shouldSkip = isDebugBuild || appMetadataProvider.isEmulator
        if (shouldSkip && !hasLoggedEnvironmentSkip) {
            hasLoggedEnvironmentSkip = true
            Timber.tag(LogTag.APP_UPDATE).d(
                "Skipping in-app update checks (debugBuild=%b, emulator=%b)",
                isDebugBuild,
                appMetadataProvider.isEmulator
            )
        }
        return shouldSkip
    }

    private suspend fun evaluateSnapshot(
        snapshot: PlayUpdateSnapshot,
        allowOptionalPrompt: Boolean,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        if (snapshot.availability == PlayUpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
            launchImmediateFlow(
                versionCode = snapshot.availableVersionCode,
                launcher = launcher,
                fallbackToPlayStoreOnError = false
            )
            return
        }
        if (snapshot.installStatus.isActiveFlexibleStatus()) {
            _uiState.value = reduceUiStateForInstallState(_uiState.value, snapshot.toInstallState())
            if (allowOptionalPrompt) hasCompletedOptionalCheckThisSession = true
            return
        }
        _uiState.value = reduceUiStateForInstallState(_uiState.value, snapshot.toInstallState())
        if (snapshot.availability != PlayUpdateAvailability.UPDATE_AVAILABLE) {
            if (allowOptionalPrompt) hasCompletedOptionalCheckThisSession = true
            return
        }

        val decision = resolveAppUpdateDecisionUseCase(
            installedVersionCode = appMetadataProvider.appVersionCode,
            availableVersionCode = snapshot.availableVersionCode
        )
        applyDecision(decision, snapshot, allowOptionalPrompt, launcher)
    }

    private fun applyDecision(
        decision: AppUpdateDecision,
        snapshot: PlayUpdateSnapshot,
        allowOptionalPrompt: Boolean,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        when (decision) {
            AppUpdateDecision.None -> {
                if (allowOptionalPrompt) hasCompletedOptionalCheckThisSession = true
            }
            is AppUpdateDecision.Forced -> {
                if (snapshot.isImmediateAllowed) {
                    launchImmediateFlow(
                        versionCode = decision.availableVersionCode,
                        launcher = launcher,
                        fallbackToPlayStoreOnError = false
                    )
                } else {
                    _uiState.value = AppUpdateUiState.ForcedUpdateBlocked(
                        availableVersionCode = decision.availableVersionCode
                    )
                }
            }
            is AppUpdateDecision.Optional -> {
                if (allowOptionalPrompt) {
                    hasCompletedOptionalCheckThisSession = true
                    launchOptionalFlow(
                        versionCode = decision.availableVersionCode,
                        isFlexibleAllowed = snapshot.isFlexibleAllowed,
                        launcher = launcher
                    )
                }
            }
        }
    }

    private fun launchOptionalFlow(
        versionCode: Long,
        isFlexibleAllowed: Boolean,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        if (!isFlexibleAllowed) return
        activeFlowType = PlayUpdateFlowType.FLEXIBLE
        activeAvailableVersionCode = versionCode
        val started = playAppUpdateClient.startUpdateFlow(PlayUpdateFlowType.FLEXIBLE, launcher)
        if (started.getOrNull() != true) {
            Timber.tag(LogTag.APP_UPDATE).d("Failed to start flexible update flow")
            activeFlowType = null
            activeAvailableVersionCode = null
        }
    }

    private suspend fun handleForcedGateOnResume(snapshot: PlayUpdateSnapshot) {
        if (snapshot.availability != PlayUpdateAvailability.UPDATE_AVAILABLE) {
            _uiState.value = AppUpdateUiState.Idle
            return
        }
        val decision = resolveAppUpdateDecisionUseCase(
            installedVersionCode = appMetadataProvider.appVersionCode,
            availableVersionCode = snapshot.availableVersionCode
        )
        if (decision !is AppUpdateDecision.Forced) {
            _uiState.value = AppUpdateUiState.Idle
        }
    }

    private suspend fun retryFromFreshSnapshot(
        snapshot: PlayUpdateSnapshot,
        current: AppUpdateUiState.ForcedUpdateBlocked,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        when (snapshot.availability) {
            PlayUpdateAvailability.UPDATE_AVAILABLE -> {
                val decision = resolveAppUpdateDecisionUseCase(
                    installedVersionCode = appMetadataProvider.appVersionCode,
                    availableVersionCode = snapshot.availableVersionCode
                )
                when {
                    decision !is AppUpdateDecision.Forced -> _uiState.value = AppUpdateUiState.Idle
                    snapshot.isImmediateAllowed -> launchImmediateFlow(
                        versionCode = snapshot.availableVersionCode,
                        launcher = launcher,
                        fallbackToPlayStoreOnError = true
                    )
                    else -> {
                        _uiState.value = current.copy(isRetrying = false)
                        _actions.tryEmit(AppUpdateCoordinatorAction.OpenPlayStoreListing)
                    }
                }
            }
            PlayUpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> launchImmediateFlow(
                versionCode = snapshot.availableVersionCode,
                launcher = launcher,
                fallbackToPlayStoreOnError = true
            )
            else -> _uiState.value = AppUpdateUiState.Idle
        }
    }

    private fun handleImmediateResult(
        resultCode: Int,
        versionCode: Long?,
        wasForcedRetry: Boolean
    ) {
        when (resultCode) {
            Activity.RESULT_OK -> _uiState.value = AppUpdateUiState.Idle
            Activity.RESULT_CANCELED -> {
                _uiState.value = AppUpdateUiState.ForcedUpdateBlocked(
                    availableVersionCode = versionCode ?: DEFAULT_VERSION_CODE,
                    isRetrying = false
                )
            }
            else -> {
                Timber.tag(LogTag.APP_UPDATE).d(
                    "Immediate update flow failed (resultCode=%d, PlayResultFailed=%d)",
                    resultCode,
                    PlayActivityResult.RESULT_IN_APP_UPDATE_FAILED
                )
                _uiState.value = AppUpdateUiState.ForcedUpdateBlocked(
                    availableVersionCode = versionCode ?: DEFAULT_VERSION_CODE,
                    isRetrying = false
                )
                if (wasForcedRetry) {
                    _actions.tryEmit(AppUpdateCoordinatorAction.OpenPlayStoreListing)
                }
            }
        }
    }

    private fun launchImmediateFlow(
        versionCode: Long,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        fallbackToPlayStoreOnError: Boolean
    ) {
        activeFlowType = PlayUpdateFlowType.IMMEDIATE
        activeAvailableVersionCode = versionCode
        isForcedRetryInFlight = fallbackToPlayStoreOnError

        val result = playAppUpdateClient.startUpdateFlow(PlayUpdateFlowType.IMMEDIATE, launcher)
        if (result.getOrNull() != true) {
            Timber.tag(LogTag.APP_UPDATE).d(result.exceptionOrNull(), "Failed to start immediate update flow")
            activeFlowType = null
            activeAvailableVersionCode = null
            isForcedRetryInFlight = false
            _uiState.value = AppUpdateUiState.ForcedUpdateBlocked(
                availableVersionCode = versionCode,
                isRetrying = false
            )
            if (fallbackToPlayStoreOnError) {
                _actions.tryEmit(AppUpdateCoordinatorAction.OpenPlayStoreListing)
            }
        }
    }

    private companion object {
        const val DEFAULT_REMOTE_CONFIG_SETTLE_TIMEOUT_MS = 3_000L
        const val DEFAULT_VERSION_CODE = 0L
    }
}

private suspend fun handleFlexibleActivityResult(
    resultCode: Int,
    versionCode: Long?,
    recordOptionalUpdateDismissalUseCase: RecordOptionalUpdateDismissalUseCase
) {
    if (resultCode == Activity.RESULT_CANCELED && versionCode != null) {
        try {
            recordOptionalUpdateDismissalUseCase(versionCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(LogTag.APP_UPDATE).d(e, "Failed to record optional update dismissal")
        }
    } else {
        Timber.tag(LogTag.APP_UPDATE).d(
            "Flexible update flow ended with resultCode=%d",
            resultCode
        )
    }
}

private fun PlayUpdateSnapshot.toInstallState(): PlayInstallState = PlayInstallState(
    status = installStatus,
    bytesDownloaded = bytesDownloaded,
    totalBytesToDownload = totalBytesToDownload
)

private fun PlayInstallStatus.isActiveFlexibleStatus(): Boolean =
    this == PlayInstallStatus.PENDING ||
        this == PlayInstallStatus.DOWNLOADING ||
        this == PlayInstallStatus.DOWNLOADED

private fun reduceUiStateForInstallState(
    currentState: AppUpdateUiState,
    state: PlayInstallState
): AppUpdateUiState {
    if (currentState is AppUpdateUiState.ForcedUpdateBlocked) {
        return currentState
    }
    return when (state.status) {
        PlayInstallStatus.PENDING -> AppUpdateUiState.FlexibleUpdateDownloading()
        PlayInstallStatus.DOWNLOADING -> AppUpdateUiState.FlexibleUpdateDownloading(
            bytesDownloaded = state.bytesDownloaded.coerceAtLeast(0L),
            totalBytesToDownload = state.totalBytesToDownload.coerceAtLeast(0L)
        )
        PlayInstallStatus.DOWNLOADED -> AppUpdateUiState.FlexibleUpdateReadyToInstall
        PlayInstallStatus.INSTALLED,
        PlayInstallStatus.FAILED,
        PlayInstallStatus.CANCELED -> if (currentState.isFlexibleOverlayState()) {
            AppUpdateUiState.Idle
        } else {
            currentState
        }
        PlayInstallStatus.INSTALLING,
        PlayInstallStatus.UNKNOWN -> currentState
    }
}

private fun AppUpdateUiState.isFlexibleOverlayState(): Boolean =
    this is AppUpdateUiState.FlexibleUpdateDownloading ||
        this is AppUpdateUiState.FlexibleUpdateReadyToInstall
