package es.pedrazamiguez.splittrip.core.logging

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import timber.log.Timber

@Suppress("UNCHECKED_CAST", "SpreadOperator")
inline fun <reified T : Any> createLoggingProxy(target: T, tag: String): T {
    require(T::class.java.isInterface) {
        "createLoggingProxy requires an interface type, but got ${T::class.java.name}"
    }
    return Proxy.newProxyInstance(
        T::class.java.classLoader,
        arrayOf(T::class.java),
        object : InvocationHandler {
            override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
                val methodName = method.name
                val continuation = args?.lastOrNull() as? Continuation<Any?>

                if (continuation != null) {
                    val newArgs = Array<Any?>(args.size) { args[it] }
                    newArgs[newArgs.lastIndex] = LoggingContinuation(continuation, tag, methodName)
                    val argsInfo = formatArgsForLogging(args, isSuspending = true)
                    Timber.tag(tag).i("Executing $methodName | $argsInfo")

                    try {
                        val result = method.invoke(target, *newArgs)
                        if (result != kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED) {
                            Timber.tag(tag).d("Completed $methodName successfully")
                        }
                        return result
                    } catch (e: Exception) {
                        val cause = e.cause ?: e
                        Timber.tag(tag).e(cause, "Failed $methodName")
                        throw cause
                    }
                } else {
                    val argsInfo = formatArgsForLogging(args, isSuspending = false)
                    Timber.tag(tag).i("Executing $methodName | $argsInfo")

                    try {
                        val result = method.invoke(target, *(args ?: emptyArray()))
                        Timber.tag(tag).d("Completed $methodName successfully")
                        return result
                    } catch (e: Exception) {
                        val cause = e.cause ?: e
                        Timber.tag(tag).e(cause, "Failed $methodName")
                        throw cause
                    }
                }
            }
        }
    ) as T
}

@PublishedApi
internal fun formatArgsForLogging(args: Array<out Any>?, isSuspending: Boolean): String {
    if (args == null) return "Args count: 0"
    val actualArgs = if (isSuspending) args.take(args.size - 1) else args.toList()
    val count = actualArgs.size
    val types = actualArgs.map { it?.javaClass?.simpleName ?: "null" }.joinToString()
    return "Args count: $count | Types: [$types]"
}

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
