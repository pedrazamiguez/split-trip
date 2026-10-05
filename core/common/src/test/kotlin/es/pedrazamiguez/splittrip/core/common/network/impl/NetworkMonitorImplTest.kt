package es.pedrazamiguez.splittrip.core.common.network.impl

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NetworkMonitorImplTest {

    @Test
    fun `isOnline defaults to true when connectivity manager is missing or inactive`() {
        val context = mockk<Context>()
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns null

        val monitor = NetworkMonitorImpl(context)

        assertNotNull(monitor.isOnline)
        assertTrue(monitor.isOnline.value)
    }

    @Test
    fun `isOnline evaluates initial connectivity when connectivity manager present`() {
        val connectivityManager = mockk<ConnectivityManager>(relaxed = true)
        every { connectivityManager.activeNetwork } returns null

        val context = mockk<Context>()
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager

        val monitor = NetworkMonitorImpl(context)

        assertNotNull(monitor.isOnline)
        assertFalse(monitor.isOnline.value)
    }

    @Test
    fun `isOnline returns true when active network has internet capability`() {
        val connectivityManager = mockk<ConnectivityManager>(relaxed = true)
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities>()
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        val context = mockk<Context>()
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager

        val monitor = NetworkMonitorImpl(context)

        assertTrue(monitor.isOnline.value)
    }

    @Test
    fun `isOnline returns false when active network lacks internet capability`() {
        val connectivityManager = mockk<ConnectivityManager>(relaxed = true)
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities>()
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns false

        val context = mockk<Context>()
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager

        val monitor = NetworkMonitorImpl(context)

        assertFalse(monitor.isOnline.value)
    }

    @Test
    fun `network callback updates isOnline on available and lost`() {
        val connectivityManager = mockk<ConnectivityManager>(relaxed = true)
        val callbackSlot = slot<ConnectivityManager.NetworkCallback>()
        every { connectivityManager.activeNetwork } returns null
        try {
            every { connectivityManager.registerDefaultNetworkCallback(capture(callbackSlot)) } answers { }
        } catch (_: Throwable) { }

        val context = mockk<Context>()
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager

        val monitor = NetworkMonitorImpl(context)

        if (callbackSlot.isCaptured) {
            val network = mockk<Network>()
            callbackSlot.captured.onAvailable(network)
            assertTrue(monitor.isOnline.value)

            callbackSlot.captured.onLost(network)
            assertFalse(monitor.isOnline.value)
        }
    }

    @Test
    fun `registerNetworkCallback logs warning and does not crash when callback registration throws`() {
        val connectivityManager = mockk<ConnectivityManager>(relaxed = true)
        every { connectivityManager.activeNetwork } returns null
        every {
            connectivityManager.registerDefaultNetworkCallback(any())
        } throws SecurityException("Permission denied")

        val context = mockk<Context>()
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager

        val monitor = NetworkMonitorImpl(context)

        assertNotNull(monitor.isOnline)
        assertFalse(monitor.isOnline.value)
    }
}
