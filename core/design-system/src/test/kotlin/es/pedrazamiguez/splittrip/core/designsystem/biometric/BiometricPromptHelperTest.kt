package es.pedrazamiguez.splittrip.core.designsystem.biometric

import android.text.TextUtils
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import java.util.concurrent.Executor
import javax.crypto.Cipher
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("BiometricPromptHelper")
class BiometricPromptHelperTest {

    private val activity = mockk<FragmentActivity>(relaxed = true)
    private val prompt = mockk<BiometricPrompt>(relaxed = true)
    private var capturedCallback: BiometricPrompt.AuthenticationCallback? = null
    private val testExecutor = Executor { it.run() }
    private val defaultMockCipher = mockk<Cipher>(relaxed = true)
    private val defaultMockCryptoObject = mockk<BiometricPrompt.CryptoObject>(relaxed = true)

    @BeforeEach
    fun setUp() {
        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } answers {
            val arg = firstArg<CharSequence?>()
            arg == null || arg.isEmpty()
        }

        capturedCallback = null
        BiometricPromptHelper.promptFactory = { _, _, callback ->
            capturedCallback = callback
            prompt
        }
        BiometricPromptHelper.executorProvider = { testExecutor }
        every { defaultMockCryptoObject.cipher } returns defaultMockCipher
        BiometricPromptHelper.cryptoObjectProvider = { defaultMockCryptoObject }
    }

    @AfterEach
    fun tearDown() {
        BiometricPromptHelper.resetDefaults()
        unmockkStatic(TextUtils::class)
    }

    @Test
    @DisplayName("authenticate launches prompt with crypto object when cryptoObject is provided explicitly")
    fun `authenticate launches prompt with crypto object when cryptoObject is provided explicitly`() {
        val explicitCryptoObject = mockk<BiometricPrompt.CryptoObject>(relaxed = true)

        BiometricPromptHelper.authenticate(
            activity = activity,
            title = "Unlock App",
            subtitle = "Verify Identity",
            negativeButtonText = "Cancel",
            cryptoObject = explicitCryptoObject,
            onSuccess = {}
        )

        val promptInfoSlot = slot<BiometricPrompt.PromptInfo>()
        verify(exactly = 1) {
            prompt.authenticate(capture(promptInfoSlot), eq(explicitCryptoObject))
        }
        assertEquals("Unlock App", promptInfoSlot.captured.title)
        assertEquals("Verify Identity", promptInfoSlot.captured.subtitle)
        assertEquals("Cancel", promptInfoSlot.captured.negativeButtonText)
    }

    @Test
    @DisplayName("authenticate launches prompt with resolved crypto object when cryptoObject is null")
    fun `authenticate launches prompt with resolved crypto object when cryptoObject is null`() {
        BiometricPromptHelper.authenticate(
            activity = activity,
            title = "Unlock App",
            subtitle = "Verify Identity",
            negativeButtonText = "Cancel",
            cryptoObject = null,
            onSuccess = {}
        )

        val promptInfoSlot = slot<BiometricPrompt.PromptInfo>()
        verify(exactly = 1) {
            prompt.authenticate(capture(promptInfoSlot), eq(defaultMockCryptoObject))
        }
        assertEquals("Unlock App", promptInfoSlot.captured.title)
        assertEquals("Verify Identity", promptInfoSlot.captured.subtitle)
        assertEquals("Cancel", promptInfoSlot.captured.negativeButtonText)
    }

    @Test
    @DisplayName("authenticate calls onError when cryptoObject resolution fails (returns null)")
    fun `authenticate calls onError when cryptoObject resolution fails (returns null)`() {
        BiometricPromptHelper.cryptoObjectProvider = { null }
        var capturedErrorCode: Int? = null
        var capturedErrorString: CharSequence? = null

        BiometricPromptHelper.authenticate(
            activity = activity,
            title = "Unlock App",
            negativeButtonText = "Cancel",
            cryptoObject = null,
            onSuccess = {},
            onError = { code, msg ->
                capturedErrorCode = code
                capturedErrorString = msg
            }
        )

        verify(exactly = 0) { prompt.authenticate(any()) }
        verify(exactly = 0) { prompt.authenticate(any(), any()) }
        assertEquals(BiometricPrompt.ERROR_UNABLE_TO_PROCESS, capturedErrorCode)
        assertEquals("Failed to initialize biometric cryptography", capturedErrorString)
    }

    @Test
    @DisplayName("onAuthenticationSucceeded executes cipher doFinal and invokes onSuccess")
    fun `onAuthenticationSucceeded executes cipher doFinal and invokes onSuccess`() {
        var successCalled = false
        val mockCipher = mockk<Cipher>(relaxed = true)
        val mockResultCryptoObject = mockk<BiometricPrompt.CryptoObject> {
            every { cipher } returns mockCipher
        }
        val authResult = mockk<BiometricPrompt.AuthenticationResult> {
            every { cryptoObject } returns mockResultCryptoObject
        }

        BiometricPromptHelper.authenticate(
            activity = activity,
            title = "Unlock App",
            negativeButtonText = "Cancel",
            onSuccess = { successCalled = true }
        )

        assertNotNull(capturedCallback)
        capturedCallback?.onAuthenticationSucceeded(authResult)

        verify(exactly = 1) { mockCipher.doFinal(any<ByteArray>()) }
        assertTrue(successCalled)
    }

    @Test
    @DisplayName("onAuthenticationSucceeded fails and calls onError when cipher doFinal throws exception")
    fun `onAuthenticationSucceeded fails and calls onError when cipher doFinal throws exception`() {
        var successCalled = false
        var capturedErrorCode: Int? = null
        var capturedErrorString: CharSequence? = null

        val mockCipher = mockk<Cipher> {
            every { doFinal(any<ByteArray>()) } throws IllegalStateException("Cipher failed")
        }
        val mockResultCryptoObject = mockk<BiometricPrompt.CryptoObject> {
            every { cipher } returns mockCipher
        }
        val authResult = mockk<BiometricPrompt.AuthenticationResult> {
            every { cryptoObject } returns mockResultCryptoObject
        }

        BiometricPromptHelper.authenticate(
            activity = activity,
            title = "Unlock App",
            negativeButtonText = "Cancel",
            onSuccess = { successCalled = true },
            onError = { code, msg ->
                capturedErrorCode = code
                capturedErrorString = msg
            }
        )

        assertNotNull(capturedCallback)
        capturedCallback?.onAuthenticationSucceeded(authResult)

        assertFalse(successCalled)
        assertEquals(BiometricPrompt.ERROR_UNABLE_TO_PROCESS, capturedErrorCode)
        assertEquals("Cipher failed", capturedErrorString)
    }

    @Test
    @DisplayName("onAuthenticationSucceeded fails and calls onError when result cryptoObject is null")
    fun `onAuthenticationSucceeded fails and calls onError when result cryptoObject is null`() {
        var successCalled = false
        var capturedErrorCode: Int? = null
        var capturedErrorString: CharSequence? = null

        val authResult = mockk<BiometricPrompt.AuthenticationResult> {
            every { cryptoObject } returns null
        }

        BiometricPromptHelper.authenticate(
            activity = activity,
            title = "Unlock App",
            negativeButtonText = "Cancel",
            onSuccess = { successCalled = true },
            onError = { code, msg ->
                capturedErrorCode = code
                capturedErrorString = msg
            }
        )

        assertNotNull(capturedCallback)
        capturedCallback?.onAuthenticationSucceeded(authResult)

        assertFalse(successCalled)
        assertEquals(BiometricPrompt.ERROR_UNABLE_TO_PROCESS, capturedErrorCode)
        assertEquals("Biometric authentication succeeded without crypto object", capturedErrorString)
    }

    @Test
    @DisplayName("onAuthenticationSucceeded fails and calls onError when cipher inside cryptoObject is null")
    fun `onAuthenticationSucceeded fails and calls onError when cipher inside cryptoObject is null`() {
        var successCalled = false
        var capturedErrorCode: Int? = null
        var capturedErrorString: CharSequence? = null

        val mockResultCryptoObject = mockk<BiometricPrompt.CryptoObject> {
            every { cipher } returns null
        }
        val authResult = mockk<BiometricPrompt.AuthenticationResult> {
            every { cryptoObject } returns mockResultCryptoObject
        }

        BiometricPromptHelper.authenticate(
            activity = activity,
            title = "Unlock App",
            negativeButtonText = "Cancel",
            onSuccess = { successCalled = true },
            onError = { code, msg ->
                capturedErrorCode = code
                capturedErrorString = msg
            }
        )

        assertNotNull(capturedCallback)
        capturedCallback?.onAuthenticationSucceeded(authResult)

        assertFalse(successCalled)
        assertEquals(BiometricPrompt.ERROR_UNABLE_TO_PROCESS, capturedErrorCode)
        assertEquals("Biometric authentication succeeded without crypto object", capturedErrorString)
    }

    @Test
    @DisplayName("onAuthenticationError callback invokes onError with code and error string")
    fun `onAuthenticationError callback invokes onError with code and error string`() {
        var capturedErrorCode: Int? = null
        var capturedErrorString: CharSequence? = null

        BiometricPromptHelper.authenticate(
            activity = activity,
            title = "Unlock App",
            negativeButtonText = "Cancel",
            onSuccess = {},
            onError = { code, msg ->
                capturedErrorCode = code
                capturedErrorString = msg
            }
        )

        assertNotNull(capturedCallback)
        capturedCallback?.onAuthenticationError(BiometricPrompt.ERROR_LOCKOUT, "Too many attempts")

        assertEquals(BiometricPrompt.ERROR_LOCKOUT, capturedErrorCode)
        assertEquals("Too many attempts", capturedErrorString)
    }

    @Test
    @DisplayName("onAuthenticationFailed callback invokes onFailed")
    fun `onAuthenticationFailed callback invokes onFailed`() {
        var failedCalled = false

        BiometricPromptHelper.authenticate(
            activity = activity,
            title = "Unlock App",
            negativeButtonText = "Cancel",
            onSuccess = {},
            onFailed = { failedCalled = true }
        )

        assertNotNull(capturedCallback)
        capturedCallback?.onAuthenticationFailed()

        assertTrue(failedCalled)
    }

    @Test
    @DisplayName("createCryptoObject catches keystore exceptions and returns null gracefully in JVM")
    fun `createCryptoObject catches keystore exceptions and returns null gracefully in JVM`() {
        val cryptoObject = BiometricPromptHelper.createCryptoObject()
        assertNull(cryptoObject)
    }

    @Test
    @DisplayName("purgeLegacyKey catches exceptions and cleans up key aliases safely")
    fun `purgeLegacyKey catches exceptions and cleans up key aliases safely`() {
        assertDoesNotThrow {
            BiometricPromptHelper.purgeLegacyKey()
        }
    }

    @Test
    @DisplayName("key alias and transformation constants match AES-GCM specification")
    fun `key alias and transformation constants match AES-GCM specification`() {
        assertEquals("split_trip_biometric_key_v3", BiometricPromptHelper.KEY_ALIAS)
        assertEquals("split_trip_biometric_key_v2", BiometricPromptHelper.LEGACY_KEY_ALIAS_V2)
        assertEquals("split_trip_biometric_auth_key", BiometricPromptHelper.LEGACY_KEY_ALIAS_V1)
        assertEquals("AES/GCM/NoPadding", BiometricPromptHelper.CIPHER_TRANSFORMATION)
    }
}
