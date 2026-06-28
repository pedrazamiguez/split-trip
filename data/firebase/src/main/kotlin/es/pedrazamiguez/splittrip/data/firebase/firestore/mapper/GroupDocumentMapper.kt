package es.pedrazamiguez.splittrip.data.firebase.firestore.mapper

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.GroupDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.GroupMemberDocument
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
    status = status.name
)

fun GroupDocument.toDomain() = Group(
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
    createdBy = createdBy
)

fun toAdminMemberDocument(groupDocRef: DocumentReference, userId: String, addedBy: String = userId) =
    GroupMemberDocument(
        memberId = userId,
        groupId = groupDocRef.id,
        groupRef = groupDocRef,
        userId = userId,
        role = "ADMIN",
        addedBy = addedBy,
        joinedAt = Timestamp.now()
    )

fun toRegularMemberDocument(groupDocRef: DocumentReference, memberId: String, addedBy: String) = GroupMemberDocument(
    memberId = memberId,
    groupId = groupDocRef.id,
    groupRef = groupDocRef,
    userId = memberId,
    role = "MEMBER",
    addedBy = addedBy,
    joinedAt = Timestamp.now()
)
