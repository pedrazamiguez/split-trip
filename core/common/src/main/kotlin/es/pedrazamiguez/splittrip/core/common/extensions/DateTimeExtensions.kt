package es.pedrazamiguez.splittrip.core.common.extensions

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Converts epoch milliseconds to [LocalDateTime] in UTC.
 *
 * All entity mappers (Room ↔ Domain) must use UTC for consistent
 * `Long` ↔ `LocalDateTime` conversions, matching the cloud layer's
 * `FirebaseTimestampMapper`.
 */
fun Long.toLocalDateTimeUtc(): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneOffset.UTC)

/**
 * Converts [LocalDateTime] to epoch milliseconds assuming UTC.
 *
 * Counterpart to [toLocalDateTimeUtc] — the same UTC zone must be used
 * in both directions to guarantee round-trip fidelity.
 */
fun LocalDateTime.toEpochMillisUtc(): Long =
    this.toInstant(ZoneOffset.UTC).toEpochMilli()

/**
 * Converts a UTC [LocalDateTime] to a [LocalDateTime] in the specified [zoneId] (defaults to system default).
 *
 * Interprets `this` at [ZoneOffset.UTC] and converts to the target timezone's local date-time.
 */
fun LocalDateTime.utcToSystemLocalDateTime(zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime =
    atZone(ZoneOffset.UTC).withZoneSameInstant(zoneId).toLocalDateTime()

/**
 * Converts a UTC [LocalDateTime] to a [LocalDate] in the specified [zoneId] (defaults to system default).
 *
 * Interprets `this` at [ZoneOffset.UTC] and converts to the target timezone's calendar date.
 */
fun LocalDateTime.utcToSystemLocalDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
    atZone(ZoneOffset.UTC).withZoneSameInstant(zoneId).toLocalDate()
