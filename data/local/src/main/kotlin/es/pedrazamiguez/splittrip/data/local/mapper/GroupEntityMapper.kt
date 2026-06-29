package es.pedrazamiguez.splittrip.data.local.mapper

import es.pedrazamiguez.splittrip.core.common.extensions.toEpochMillisUtc
import es.pedrazamiguez.splittrip.core.common.extensions.toLocalDateTimeUtc
import es.pedrazamiguez.splittrip.data.local.entity.GroupEntity
import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.Group

/**
 * Maps GroupEntity (Room) to Group (Domain).
 */
fun GroupEntity.toDomain(): Group = Group(
    id = id,
    name = name,
    description = description ?: "",
    currency = currencyCode,
    extraCurrencies = extraCurrencies,
    members = memberIds.sorted(),
    mainImagePath = mainImagePath,
    createdAt = createdAtMillis?.toLocalDateTimeUtc(),
    lastUpdatedAt = lastUpdatedAtMillis?.toLocalDateTimeUtc(),
    syncStatus = SyncStatus.fromStringOrDefault(syncStatus),
    status = GroupStatus.fromStringOrDefault(status),
    createdBy = createdBy
)

/**
 * Maps Group (Domain) to GroupEntity (Room).
 */
fun Group.toEntity(): GroupEntity = GroupEntity(
    id = id,
    name = name,
    description = description.ifBlank { null },
    currencyCode = currency,
    extraCurrencies = extraCurrencies,
    memberIds = members,
    mainImagePath = mainImagePath,
    createdAtMillis = createdAt?.toEpochMillisUtc(),
    lastUpdatedAtMillis = lastUpdatedAt?.toEpochMillisUtc(),
    syncStatus = syncStatus.name,
    status = status.name,
    createdBy = createdBy
)

/**
 * Maps a list of GroupEntity to a list of Group.
 */
fun List<GroupEntity>.toDomain(): List<Group> = map { it.toDomain() }

/**
 * Maps a list of Group to a list of GroupEntity.
 */
fun List<Group>.toEntity(): List<GroupEntity> = map { it.toEntity() }
