package es.pedrazamiguez.splittrip.domain.service.split

import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.model.EntitySplit
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.SubunitSplitOverride
import java.math.BigDecimal

interface SubunitAwareSplitService {
    fun calculateShares(
        totalAmountCents: Long,
        individualParticipantIds: List<String>,
        subunits: List<Subunit>,
        entitySplitType: SplitType,
        entitySplits: List<EntitySplit> = emptyList(),
        subunitSplitOverrides: Map<String, SubunitSplitOverride> = emptyMap()
    ): List<ExpenseSplit>
    fun distributeByMemberShares(
        memberIds: List<String>,
        totalCents: Long,
        memberShares: Map<String, BigDecimal>
    ): Map<String, Long>
}
