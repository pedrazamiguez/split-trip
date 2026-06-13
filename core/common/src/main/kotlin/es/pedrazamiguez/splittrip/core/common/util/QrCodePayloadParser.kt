package es.pedrazamiguez.splittrip.core.common.util

import android.net.Uri

object QrCodePayloadParser {
    private const val SCHEME = "splittrip"
    private const val HOST = "user"
    private const val PATH = "/share"
    private const val PARAM_EMAIL = "email"
    private const val PARAM_USER_ID = "userId"

    data class SharePayload(
        val email: String,
        val userId: String
    )

    fun createSharePayload(email: String, userId: String): String {
        return Uri.Builder()
            .scheme(SCHEME)
            .authority(HOST)
            .path(PATH)
            .appendQueryParameter(PARAM_EMAIL, email)
            .appendQueryParameter(PARAM_USER_ID, userId)
            .build()
            .toString()
    }

    fun parseSharePayload(uriString: String): SharePayload? {
        return try {
            val uri = Uri.parse(uriString)
            val isValid = uri.scheme == SCHEME && uri.host == HOST && uri.path == PATH
            val email = uri.getQueryParameter(PARAM_EMAIL)
            val userId = uri.getQueryParameter(PARAM_USER_ID)
            if (isValid && !email.isNullOrBlank() && !userId.isNullOrBlank()) {
                SharePayload(email, userId)
            } else {
                null
            }
        } catch (ignored: Exception) {
            null
        }
    }
}
