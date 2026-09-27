package es.pedrazamiguez.splittrip.data.firebase.firestore.document

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class DeviceDocument(
    val deviceId: String = "",
    val token: String = "",
    val model: String = "",
    val androidVersion: String = "",
    val appVersionName: String = "",
    val appVersionCode: Long = 0,
    @get:PropertyName("emulator") @set:PropertyName("emulator")
    var isEmulator: Boolean = false,
    val lastUpdatedAt: Timestamp? = null
) {
    companion object {
        fun collectionPath(userId: String) = "users/$userId/devices"
        const val TOKEN_FIELD = "token"
        const val LAST_UPDATED_AT_FIELD = "lastUpdatedAt"
        const val STALE_THRESHOLD_DAYS = 90L
        const val MAX_DEVICES_PER_USER = 5
    }
}
