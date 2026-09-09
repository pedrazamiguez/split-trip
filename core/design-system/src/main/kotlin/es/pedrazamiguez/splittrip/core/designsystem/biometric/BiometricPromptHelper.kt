package es.pedrazamiguez.splittrip.core.designsystem.biometric

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.ProviderException
import java.util.concurrent.Executor
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

object BiometricPromptHelper {

    private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
    internal const val KEY_ALIAS = "split_trip_biometric_key_v3"
    internal const val LEGACY_KEY_ALIAS_V2 = "split_trip_biometric_key_v2"
    internal const val LEGACY_KEY_ALIAS_V1 = "split_trip_biometric_auth_key"
    internal const val CIPHER_TRANSFORMATION =
        "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}"
    private val AUTH_CHALLENGE = "split_trip_auth_challenge".toByteArray(StandardCharsets.UTF_8)

    internal var promptFactory: (
        FragmentActivity,
        Executor,
        BiometricPrompt.AuthenticationCallback
    ) -> BiometricPrompt = { activity, executor, callback ->
        BiometricPrompt(activity, executor, callback)
    }

    internal var executorProvider: (FragmentActivity) -> Executor = { activity ->
        ContextCompat.getMainExecutor(activity)
    }

    internal var cryptoObjectProvider: () -> BiometricPrompt.CryptoObject? = { createCryptoObject() }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String? = null,
        negativeButtonText: String,
        cryptoObject: BiometricPrompt.CryptoObject? = null,
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit = { _, _ -> },
        onFailed: () -> Unit = {}
    ) {
        val resolvedCryptoObject = cryptoObject ?: cryptoObjectProvider()
        if (resolvedCryptoObject == null) {
            onError(
                BiometricPrompt.ERROR_UNABLE_TO_PROCESS,
                "Failed to initialize biometric cryptography"
            )
            return
        }

        val executor = executorProvider(activity)
        val prompt = promptFactory(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val authenticatedCipher = result.cryptoObject?.cipher
                    if (authenticatedCipher != null) {
                        try {
                            authenticatedCipher.doFinal(AUTH_CHALLENGE)
                            onSuccess()
                        } catch (e: Exception) {
                            onError(
                                BiometricPrompt.ERROR_UNABLE_TO_PROCESS,
                                e.localizedMessage ?: "Cryptographic verification failed"
                            )
                        }
                    } else {
                        onError(
                            BiometricPrompt.ERROR_UNABLE_TO_PROCESS,
                            "Biometric authentication succeeded without crypto object"
                        )
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }
        )

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BIOMETRIC_STRONG)

        if (!subtitle.isNullOrBlank()) {
            promptInfoBuilder.setSubtitle(subtitle)
        }

        val promptInfo = promptInfoBuilder.build()
        prompt.authenticate(promptInfo, resolvedCryptoObject)
    }

    fun createCryptoObject(): BiometricPrompt.CryptoObject? {
        return try {
            val cipher = try {
                initCipher()
            } catch (_: KeyPermanentlyInvalidatedException) {
                deleteSecretKey()
                initCipher()
            } catch (_: GeneralSecurityException) {
                deleteSecretKey()
                initCipher()
            } catch (_: ProviderException) {
                deleteSecretKey()
                initCipher()
            } catch (_: Exception) {
                deleteSecretKey()
                initCipher()
            }
            BiometricPrompt.CryptoObject(cipher)
        } catch (_: Exception) {
            null
        }
    }

    fun purgeLegacyKey() {
        deleteKey(KEY_ALIAS)
        deleteKey(LEGACY_KEY_ALIAS_V2)
        deleteKey(LEGACY_KEY_ALIAS_V1)
    }

    internal fun resetDefaults() {
        promptFactory = { activity, executor, callback ->
            BiometricPrompt(activity, executor, callback)
        }
        executorProvider = { activity ->
            ContextCompat.getMainExecutor(activity)
        }
        cryptoObjectProvider = { createCryptoObject() }
    }

    private fun initCipher(): Cipher {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateSecretKey()
        }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    private fun generateSecretKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE_PROVIDER
        )
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        }

        keyGenerator.init(builder.build())
        keyGenerator.generateKey()
    }

    private fun deleteSecretKey() {
        deleteKey(KEY_ALIAS)
    }

    private fun deleteKey(alias: String) {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply { load(null) }
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias)
            }
        } catch (_: Exception) {
            // Ignore keystore deletion failure
        }
    }
}
