package es.pedrazamiguez.splittrip.data.local.mapper
import es.pedrazamiguez.splittrip.data.local.entity.ExpenseSplitEntity
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
class ExpenseSplitEntityMapperTest {
    private val fullEntity = ExpenseSplitEntity(
        expenseId = "exp-1",
        userId = "user-1",
        amountCents = 5000L,
        percentage = "33.33",
        isExcluded = false,
        isCoveredById = null,
        subunitId = "sub-1"
    )

    @Nested
    inner class ToDomain {
        @Test
        fun `maps all fields correctly`() {
            val split = fullEntity.toDomain()
            assertEquals("user-1", split.userId)
            assertEquals(5000L, split.amountCents)
            assertEquals(0, BigDecimal("33.33").compareTo(split.percentage))
            assertFalse(split.isExcluded)
            assertNull(split.isCoveredById)
            assertEquals("sub-1", split.subunitId)
        }

        @Test
        fun `null percentage maps to null`() {
            val entity = fullEntity.copy(percentage = null)
            val split = entity.toDomain()
            assertNull(split.percentage)
        }

        @Test
        fun `null subunitId maps to null`() {
            val entity = fullEntity.copy(subunitId = null)
            val split = entity.toDomain()
            assertNull(split.subunitId)
        }

        @Test
        fun `isExcluded true maps correctly`() {
            val entity = fullEntity.copy(isExcluded = true)
            val split = entity.toDomain()
            assertTrue(split.isExcluded)
        }

        @Test
        fun `isCoveredById maps correctly`() {
            val entity = fullEntity.copy(isCoveredById = "other-user")
            val split = entity.toDomain()
            assertEquals("other-user", split.isCoveredById)
        }

        @Test
        fun `EQUAL splitType maps correctly`() {
            val entity = fullEntity.copy(splitType = "EQUAL")
            val split = entity.toDomain()
            assertEquals(SplitType.EQUAL, split.splitType)
        }

        @Test
        fun `EXACT splitType maps correctly`() {
            val entity = fullEntity.copy(splitType = "EXACT")
            val split = entity.toDomain()
            assertEquals(SplitType.EXACT, split.splitType)
        }

        @Test
        fun `PERCENT splitType maps correctly`() {
            val entity = fullEntity.copy(splitType = "PERCENT")
            val split = entity.toDomain()
            assertEquals(SplitType.PERCENT, split.splitType)
        }

        @Test
        fun `null splitType maps to null`() {
            val split = fullEntity.toDomain()
            assertNull(split.splitType)
        }

        @Test
        fun `unknown splitType string maps to null`() {
            val entity = fullEntity.copy(splitType = "UNKNOWN_TYPE")
            val split = entity.toDomain()
            assertNull(split.splitType)
        }
    }

    @Nested
    inner class ToEntity {
        private val fullSplit = ExpenseSplit(
            userId = "user-1",
            amountCents = 5000L,
            percentage = BigDecimal("33.33"),
            isExcluded = false,
            isCoveredById = null,
            subunitId = "sub-1"
        )

        @Test
        fun `maps all fields with expenseId`() {
            val entity = fullSplit.toEntity("exp-1")
            assertEquals("exp-1", entity.expenseId)
            assertEquals("user-1", entity.userId)
            assertEquals(5000L, entity.amountCents)
            assertEquals("33.33", entity.percentage)
            assertFalse(entity.isExcluded)
            assertEquals("sub-1", entity.subunitId)
        }

        @Test
        fun `null percentage maps to null string`() {
            val split = fullSplit.copy(percentage = null)
            val entity = split.toEntity("exp-1")
            assertNull(entity.percentage)
        }

        @Test
        fun `expenseId is propagated correctly`() {
            val entity = fullSplit.toEntity("different-exp")
            assertEquals("different-exp", entity.expenseId)
        }

        @Test
        fun `EQUAL splitType serialises to name string`() {
            val split = fullSplit.copy(splitType = SplitType.EQUAL)
            val entity = split.toEntity("exp-1")
            assertEquals("EQUAL", entity.splitType)
        }

        @Test
        fun `EXACT splitType serialises to name string`() {
            val split = fullSplit.copy(splitType = SplitType.EXACT)
            val entity = split.toEntity("exp-1")
            assertEquals("EXACT", entity.splitType)
        }

        @Test
        fun `PERCENT splitType serialises to name string`() {
            val split = fullSplit.copy(splitType = SplitType.PERCENT)
            val entity = split.toEntity("exp-1")
            assertEquals("PERCENT", entity.splitType)
        }

        @Test
        fun `null splitType serialises to null`() {
            val entity = fullSplit.toEntity("exp-1")
            assertNull(entity.splitType)
        }
    }

    @Nested
    inner class ListExtensions {
        @Test
        fun `toDomainSplits maps list of entities`() {
            val entities = listOf(fullEntity, fullEntity.copy(userId = "user-2"))
            val splits = entities.toDomainSplits()
            assertEquals(2, splits.size)
            assertEquals("user-2", splits[1].userId)
        }

        @Test
        fun `toSplitEntities maps list with expenseId`() {
            val split = fullEntity.toDomain()
            val entities = listOf(split, split.copy(userId = "user-2")).toSplitEntities("exp-1")
            assertEquals(2, entities.size)
            assertEquals("exp-1", entities[0].expenseId)
            assertEquals("exp-1", entities[1].expenseId)
        }
    }
}
