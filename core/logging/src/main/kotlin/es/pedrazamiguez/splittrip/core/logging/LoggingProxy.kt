package es.pedrazamiguez.splittrip.core.logging

import es.pedrazamiguez.splittrip.core.logging.impl.LoggingContinuation
import es.pedrazamiguez.splittrip.core.logging.sanitizer.sanitizePii
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import timber.log.Timber

private const val MAX_STRING_ARG_LENGTH = 60
private const val MAX_OBJECT_ARG_LENGTH = 150

@Suppress("UNCHECKED_CAST", "SpreadOperator")
inline fun <reified T : Any> createLoggingProxy(target: T, tag: String): T {
    require(T::class.java.isInterface) {
        "createLoggingProxy requires an interface type, but got ${T::class.java.name}"
    }
    val interfaceName = T::class.java.simpleName
    return Proxy.newProxyInstance(
        T::class.java.classLoader,
        arrayOf(T::class.java),
        object : InvocationHandler {
            override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
                val methodName = method.name
                val continuation = args?.lastOrNull() as? Continuation<Any?>

                if (continuation != null) {
                    val newArgs = Array<Any?>(args.size) { args[it] }
                    newArgs[newArgs.lastIndex] = LoggingContinuation(continuation, tag, "$interfaceName.$methodName")
                    val argsInfo = formatArgsForLogging(interfaceName, args, isSuspending = true)
                    Timber.tag(tag).i("Executing $interfaceName.$methodName($argsInfo)")

                    try {
                        val result = method.invoke(target, *newArgs)
                        if (result != COROUTINE_SUSPENDED) {
                            Timber.tag(tag).d("Completed $interfaceName.$methodName successfully")
                        }
                        return result
                    } catch (e: Exception) {
                        val cause = e.cause ?: e
                        Timber.tag(tag).e(cause, "Failed $interfaceName.$methodName")
                        throw cause
                    }
                } else {
                    val argsInfo = formatArgsForLogging(interfaceName, args, isSuspending = false)
                    Timber.tag(tag).i("Executing $interfaceName.$methodName($argsInfo)")

                    try {
                        val result = method.invoke(target, *(args ?: emptyArray()))
                        Timber.tag(tag).d("Completed $interfaceName.$methodName successfully")
                        return result
                    } catch (e: Exception) {
                        val cause = e.cause ?: e
                        Timber.tag(tag).e(cause, "Failed $interfaceName.$methodName")
                        throw cause
                    }
                }
            }
        }
    ) as T
}

@PublishedApi
internal fun formatArgsForLogging(
    interfaceName: String,
    args: Array<out Any>?,
    isSuspending: Boolean
): String {
    if (args == null) return ""
    val actualArgs = if (isSuspending) args.take(args.size - 1) else args.toList()
    if (actualArgs.isEmpty()) return ""

    val isSensitive = isSensitiveInterface(interfaceName)
    val formatted = actualArgs.map { formatSingleArg(it, isSensitive) }
    return formatted.joinToString(", ")
}

private fun isSensitiveInterface(interfaceName: String): Boolean {
    val sensitiveKeywords = listOf("Password", "Login", "SignIn", "SignUp", "Register", "Credential", "Token", "Auth")
    return sensitiveKeywords.any { interfaceName.contains(it, ignoreCase = true) }
}

private fun formatSingleArg(arg: Any?, isSensitive: Boolean): String {
    if (arg == null) return "null"
    val rawString = arg.toString()

    return when {
        arg is String -> formatStringArg(rawString, isSensitive)
        arg is Number || arg is Boolean -> rawString
        else -> formatObjectArg(rawString, isSensitive)
    }
}

private fun formatStringArg(value: String, isSensitive: Boolean): String {
    return if (isSensitive) {
        if (value.contains("@")) value.sanitizePii() else "[PROTECTED]"
    } else {
        val sanitized = value.sanitizePii()
        if (sanitized.length > MAX_STRING_ARG_LENGTH) sanitized.take(MAX_STRING_ARG_LENGTH) + "..." else sanitized
    }
}

private fun formatObjectArg(value: String, isSensitive: Boolean): String {
    return if (isSensitive) {
        "[PROTECTED]"
    } else {
        val sanitized = value.sanitizePii()
        if (sanitized.length > MAX_OBJECT_ARG_LENGTH) sanitized.take(MAX_OBJECT_ARG_LENGTH) + "..." else sanitized
    }
}
