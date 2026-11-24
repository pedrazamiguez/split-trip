package es.pedrazamiguez.splittrip.data.firebase.firestore.mapper

import com.google.firebase.Timestamp
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.GroupDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.GroupMemberDocument
import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.model.Group

fun Group.toDocument(groupId: String, userId: String) = GroupDocument(
    groupId = groupId,
    name = name,
    description = description,
    currency = currency,
    extraCurrencies = extraCurrencies,
    memberIds = members,
    mainImagePath = mainImagePath ?: "",
    createdBy = createdBy.ifBlank { userId },
    createdAt = createdAt?.toTimestampUtc(),
    lastUpdatedAt = lastUpdatedAt?.toTimestampUtc(),
    status = status.name,
    lastArchiveEventId = lastArchiveEventId,
    context = context.name
)

fun GroupDocument.toDomain(): Group? {
    if (deletionRequested) return null

    return Group(
        id = groupId,
        name = name,
        description = description,
        currency = currency,
        extraCurrencies = extraCurrencies,
        members = memberIds.sorted(),
        mainImagePath = mainImagePath.takeIf { it.isNotBlank() },
        createdAt = createdAt?.toLocalDateTimeUtc(),
        lastUpdatedAt = lastUpdatedAt?.toLocalDateTimeUtc(),
        status = GroupStatus.fromStringOrDefault(status),
        createdBy = createdBy,
        lastArchiveEventId = lastArchiveEventId,
        context = GroupContext.fromStringOrDefault(context)
    )
}

fun toAdminMemberDocument(groupId: String, userId: String, addedBy: String = userId) =
    GroupMemberDocument(
        memberId = userId,
        groupId = groupId,
        userId = userId,
        role = "ADMIN",
        addedBy = addedBy,
        joinedAt = Timestamp.now()
    )

fun toRegularMemberDocument(groupId: String, memberId: String, addedBy: String) = GroupMemberDocument(
    memberId = memberId,
    groupId = groupId,
    userId = memberId,
    role = "MEMBER",
    addedBy = addedBy,
    joinedAt = Timestamp.now()
)
