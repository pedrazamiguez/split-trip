package es.pedrazamiguez.splittrip.data.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import es.pedrazamiguez.splittrip.domain.constant.BillingConstants
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

internal class BillingConnectionCoordinator(
    private val billingClientFactory: (PurchasesUpdatedListener) -> BillingClient,
    private val listener: PurchasesUpdatedListener,
    private val ioDispatcher: CoroutineDispatcher
) {
    private val connectionMutex = Mutex()

    @Volatile
    private var billingClient: BillingClient? = null
    private var activeConnectionDeferred: CompletableDeferred<Result<Unit>>? = null

    val isReady: Boolean
        get() = billingClient?.isReady == true

    fun getClient(): BillingClient {
        return billingClient ?: synchronized(this) {
            billingClient ?: billingClientFactory(listener).also { billingClient = it }
        }
    }

    fun reset() {
        try {
            billingClient?.endConnection()
        } catch (e: Exception) {
            Timber.w(e, "Error ending billing client connection")
        } finally {
            billingClient = null
        }
    }

    suspend fun ensureConnected(): Result<Unit> = withContext(ioDispatcher) {
        if (isReady) return@withContext Result.success(Unit)

        val (deferred, isInitiator) = connectionMutex.withLock {
            if (isReady) return@withContext Result.success(Unit)
            getOrCreateConnectionDeferred()
        }

        if (!isInitiator) {
            return@withContext deferred.await()
        }

        executeConnection(deferred)
    }

    private fun getOrCreateConnectionDeferred(): Pair<CompletableDeferred<Result<Unit>>, Boolean> {
        val existing = activeConnectionDeferred
        return if (existing != null && existing.isActive) {
            existing to false
        } else {
            val newDeferred = CompletableDeferred<Result<Unit>>()
            activeConnectionDeferred = newDeferred
            newDeferred to true
        }
    }

    private suspend fun executeConnection(deferred: CompletableDeferred<Result<Unit>>): Result<Unit> {
        return try {
            val result = connectBillingClient()
            deferred.complete(result)
            result
        } catch (e: CancellationException) {
            deferred.complete(Result.failure(e))
            throw e
        } catch (e: Exception) {
            val failure = Result.failure<Unit>(e)
            deferred.complete(failure)
            failure
        } finally {
            connectionMutex.withLock {
                if (activeConnectionDeferred === deferred) {
                    activeConnectionDeferred = null
                }
            }
        }
    }

    private suspend fun connectBillingClient(): Result<Unit> {
        val client = getClient()
        val timeoutMs = BillingConstants.BILLING_CONNECT_TIMEOUT_MS
        val connectResult = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation {
                    reset()
                }
                client.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        if (!continuation.isActive) return
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            continuation.resume(Result.success(Unit))
                        } else {
                            val code = billingResult.responseCode
                            val msg = billingResult.debugMessage
                            reset()
                            continuation.resume(
                                Result.failure(
                                    IllegalStateException("Billing setup failed: $msg (code: $code)")
                                )
                            )
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        Timber.w("Billing service disconnected")
                        reset()
                    }
                })
            }
        }

        return if (connectResult != null) {
            connectResult
        } else {
            Timber.w("Billing connection timed out after %d ms", timeoutMs)
            reset()
            Result.failure(
                IllegalStateException("Billing connection timed out after $timeoutMs ms")
            )
        }
    }
}
