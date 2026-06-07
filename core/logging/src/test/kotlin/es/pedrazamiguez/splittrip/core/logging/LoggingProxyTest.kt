package es.pedrazamiguez.splittrip.core.logging

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

interface SampleService {
    fun doSomething(arg: String): String
    suspend fun doSomethingSuspending(arg: String): String
}

class SampleServiceImpl : SampleService {
    override fun doSomething(arg: String): String = "Echo: $arg"
    override suspend fun doSomethingSuspending(arg: String): String = "Suspend Echo: $arg"
}

class LoggingProxyTest {

    @Test
    fun testLoggingProxyNormalMethod() {
        val original = SampleServiceImpl()
        val proxy = createLoggingProxy<SampleService>(original, "TestTag")
        assertEquals("Echo: Hello", proxy.doSomething("Hello"))
    }

    @Test
    fun testLoggingProxySuspendingMethod() = runTest {
        val original = SampleServiceImpl()
        val proxy = createLoggingProxy<SampleService>(original, "TestTag")
        assertEquals("Suspend Echo: Hello", proxy.doSomethingSuspending("Hello"))
    }
}
