package es.pedrazamiguez.splittrip.core.logging.impl

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import timber.log.Timber

@PublishedApi
internal class LoggingContinuation<T>(
    private val delegate: Continuation<T>,
    private val tag: String,
    private val methodName: String
) : Continuation<T> {
    override val context: CoroutineContext
        get() = delegate.context

    override fun resumeWith(result: kotlin.Result<T>) {
        result.fold(
            onSuccess = {
                Timber.tag(tag).d("Completed $methodName successfully")
            },
            onFailure = { e ->
                Timber.tag(tag).e(e, "Failed $methodName")
            }
        )
        delegate.resumeWith(result)
    }
}
