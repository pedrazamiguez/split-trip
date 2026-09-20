package es.pedrazamiguez.splittrip.features.subunit.presentation.preview

import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.features.subunit.presentation.model.MemberShareUiModel
import es.pedrazamiguez.splittrip.features.subunit.presentation.model.MemberUiModel
import es.pedrazamiguez.splittrip.features.subunit.presentation.model.SubunitUiModel
import java.math.BigDecimal
import kotlinx.collections.immutable.persistentListOf

val PREVIEW_GROUP_MEMBERS = persistentListOf(
    MemberUiModel(
        userId = "user-1",
        displayName = "Antonio García",
        isAssigned = false,
        assignedSubunitName = ""
    ),
    MemberUiModel(
        userId = "user-2",
        displayName = "María López",
        isAssigned = false,
        assignedSubunitName = ""
    ),
    MemberUiModel(
        userId = "user-3",
        displayName = "Andrés Pedraza",
        isAssigned = true,
        assignedSubunitName = "Cantalobos"
    )
)

val PREVIEW_MEMBER_SHARES = mapOf(
    "user-1" to "50",
    "user-2" to "50"
)

val PREVIEW_SUBUNIT_1 = Subunit(
    id = "sub-1",
    groupId = "group-1",
    name = "Cantalobos",
    memberIds = listOf("user-1", "user-2"),
    memberShares = mapOf("user-1" to BigDecimal("50"), "user-2" to BigDecimal("50"))
)

val PREVIEW_SUBUNIT_2 = Subunit(
    id = "sub-2",
    groupId = "group-1",
    name = "Beach House",
    memberIds = listOf("user-1", "user-2", "user-3"),
    memberShares = mapOf(
        "user-1" to BigDecimal("40"),
        "user-2" to BigDecimal("30"),
        "user-3" to BigDecimal("30")
    )
)

val PREVIEW_SUBUNIT_UI_MODELS = persistentListOf(
    SubunitUiModel(
        id = "sub-1",
        name = "Cantalobos",
        memberCount = "2 members",
        memberShares = persistentListOf(
            MemberShareUiModel(displayName = "Antonio García", shareText = "50 %"),
            MemberShareUiModel(displayName = "María López", shareText = "50 %")
        )
    ),
    SubunitUiModel(
        id = "sub-2",
        name = "Beach House",
        memberCount = "3 members",
        memberShares = persistentListOf(
            MemberShareUiModel(displayName = "Antonio García", shareText = "40 %"),
            MemberShareUiModel(displayName = "María López", shareText = "30 %"),
            MemberShareUiModel(displayName = "Andrés Pedraza", shareText = "30 %")
        )
    )
)
