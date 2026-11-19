package es.pedrazamiguez.splittrip.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import es.pedrazamiguez.splittrip.core.logging.LogTag
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.user.CheckPendingReconciliationUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

private const val DEFAULT_RECONCILIATION_TIMEOUT_MS = 6_000L

@Composable
internal fun StartupReconciliationEffect(
    isUserLoggedIn: Boolean?,
    isReconciled: Boolean?,
    stableStartDestination: String?,
    dependencies: StartupReconciliationDependencies,
    onResult: (isChecked: Boolean, hasPending: Boolean?) -> Unit,
    reconciliationTimeoutMs: Long = DEFAULT_RECONCILIATION_TIMEOUT_MS
) {
    val currentOnResult by rememberUpdatedState(onResult)

    LaunchedEffect(isUserLoggedIn, isReconciled) {
        if (stableStartDestination != null) {
            return@LaunchedEffect
        }
        if (isUserLoggedIn != true || isReconciled != false) {
            currentOnResult(false, null)
            return@LaunchedEffect
        }
        currentOnResult(false, null)
        val email = dependencies.authenticationService.currentUserEmail()?.takeIf(String::isNotBlank)
        if (email == null) {
            Timber.tag(LogTag.NAVIGATION).i("No email for active session; marking reconciled")
            markSessionReconciled(dependencies.userPreferenceRepository)
            currentOnResult(true, false)
        } else {
            checkEmailReconciliation(
                email = email,
                reconciliationTimeoutMs = reconciliationTimeoutMs,
                userPreferenceRepository = dependencies.userPreferenceRepository,
                checkPendingReconciliationUseCase = dependencies.checkPendingReconciliationUseCase,
                onResult = currentOnResult
            )
        }
    }
}

private suspend fun checkEmailReconciliation(
    email: String,
    reconciliationTimeoutMs: Long,
    userPreferenceRepository: UserPreferenceRepository,
    checkPendingReconciliationUseCase: CheckPendingReconciliationUseCase,
    onResult: (isChecked: Boolean, hasPending: Boolean?) -> Unit
) {
    val result = withTimeoutOrNull(reconciliationTimeoutMs) {
        checkPendingReconciliationUseCase(email)
    }
    if (result == null) {
        Timber.tag(LogTag.NAVIGATION).w(
            "Pending reconciliation timed out after %d ms; failing open",
            reconciliationTimeoutMs
        )
        onResult(true, false)
        return
    }
    result
        .onSuccess { hasPending ->
            if (hasPending) {
                Timber.tag(LogTag.NAVIGATION).i("Pending reconciliation found; routing to reconciliation")
                onResult(true, true)
            } else {
                markSessionReconciled(userPreferenceRepository)
                onResult(true, false)
            }
        }
        .onFailure { throwable ->
            Timber.tag(LogTag.NAVIGATION).w(
                throwable,
                "Pending reconciliation failed; failing open"
            )
            onResult(true, false)
        }
}

@Suppress("TooGenericExceptionCaught")
private suspend fun markSessionReconciled(userPreferenceRepository: UserPreferenceRepository) {
    try {
        userPreferenceRepository.setIsReconciled(true)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.tag(LogTag.NAVIGATION).w(e, "Failed to persist reconciled preference")
    }
}
