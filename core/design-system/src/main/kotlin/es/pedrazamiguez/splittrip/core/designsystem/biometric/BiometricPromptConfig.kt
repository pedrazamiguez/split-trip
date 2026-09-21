package es.pedrazamiguez.splittrip.core.designsystem.biometric

import androidx.biometric.BiometricPrompt

/**
 * Configuration parameters for biometric prompt display.
 *
 * @param title Title displayed in the biometric prompt.
 * @param subtitle Optional subtitle displayed in the biometric prompt.
 * @param negativeButtonText Text for the cancel/negative button.
 * @param cryptoObject Optional custom [BiometricPrompt.CryptoObject] to use.
 */
data class BiometricPromptConfig(
    val title: String,
    val subtitle: String? = null,
    val negativeButtonText: String,
    val cryptoObject: BiometricPrompt.CryptoObject? = null
)
