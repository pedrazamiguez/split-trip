package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.model.Settlement
import es.pedrazamiguez.splittrip.domain.model.SettlementPocketType

object EphemeralSettlementIdHelper {
    private const val PREFIX = "ephemeral|"
    private const val DELIMITER = "|"
    private const val PAYLOAD_PARTS_COUNT = 5
    private const val INDEX_FROM_USER_ID = 0
    private const val INDEX_TO_USER_ID = 1
    private const val INDEX_SOURCE_POCKET = 2
    private const val INDEX_CURRENCY = 3
    private const val INDEX_AMOUNT = 4

    fun createId(
        fromUserId: String,
        toUserId: String,
        sourcePocket: SettlementPocketType,
        currency: String,
        amount: Long
    ): String = buildString {
        append(PREFIX)
        append(fromUserId).append(DELIMITER)
        append(toUserId).append(DELIMITER)
        append(sourcePocket.name).append(DELIMITER)
        append(currency).append(DELIMITER)
        append(amount)
    }

    fun isEphemeral(id: String): Boolean = id.startsWith(PREFIX)

    fun parse(id: String): Settlement? {
        if (!isEphemeral(id)) return null
        val payload = id.removePrefix(PREFIX)
        val parts = payload.split(DELIMITER)
        if (parts.size != PAYLOAD_PARTS_COUNT) return null
        val fromUserId = parts[INDEX_FROM_USER_ID]
        val toUserId = parts[INDEX_TO_USER_ID]
        val sourcePocket = runCatching { SettlementPocketType.valueOf(parts[INDEX_SOURCE_POCKET]) }.getOrNull()
            ?: return null
        val currency = parts[INDEX_CURRENCY]
        val amount = parts[INDEX_AMOUNT].toLongOrNull() ?: return null
        return Settlement(
            fromUserId = fromUserId,
            toUserId = toUserId,
            amount = amount,
            currency = currency,
            sourcePocket = sourcePocket
        )
    }
}
