package es.pedrazamiguez.splittrip.data.firebase.firestore.mapper
import com.google.firebase.firestore.DocumentReference
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.ExpenseSplitDocument
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import io.mockk.mockk
import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
class ExpenseSplitDocumentMapperTest {
    private val testUserId = "user-123"
    private val testCoveredByUserId = "user-456"
    private val testDocRef: DocumentReference = mockk(relaxed = true)

    @Nested
    inner class ToDomain {
        @Test
        fun `maps all fields correctly`() {
            val document = ExpenseSplitDocument(
                userId = testUserId,
                amountCents = 5000L,
                percentage = "33.33",
                isExcluded = false,
                isCoveredById = null,
                isCoveredByRef = null
            )
            val domain = document.toDomain()
            assertEquals(testUserId, domain.userId)
            assertEquals(5000L, domain.amountCents)
            assertEquals(0, BigDecimal("33.33").compareTo(domain.percentage))
            assertFalse(domain.isExcluded)
            assertNull(domain.isCoveredById)
        }

        @Test
        fun `maps isExcluded true correctly`() {
            val document = ExpenseSplitDocument(
                userId = testUserId,
                amountCents = 0L,
                isExcluded = true
            )
            val domain = document.toDomain()
            assertTrue(domain.isExcluded)
        }

        @Test
        fun `maps isCoveredById correctly`() {
            val document = ExpenseSplitDocument(
                userId = testUserId,
                amountCents = 3000L,
                isCoveredById = testCoveredByUserId,
                isCoveredByRef = testDocRef
            )
            val domain = document.toDomain()
            assertEquals(testCoveredByUserId, domain.isCoveredById)
        }

        @Test
        fun `defaults amountCents to 0 when null`() {
            val document = ExpenseSplitDocument(
                userId = testUserId,
                amountCents = null
            )
            val domain = document.toDomain()
            assertEquals(0L, domain.amountCents)
        }

        @Test
        fun `maps null percentage to null`() {
            val document = ExpenseSplitDocument(
                userId = testUserId,
                amountCents = 5000L,
                percentage = null
            )
            val domain = document.toDomain()
            assertNull(domain.percentage)
        }

        @Test
        fun `maps non-numeric percentage to null`() {
            val document = ExpenseSplitDocument(
                userId = testUserId,
                amountCents = 5000L,
                percentage = "not-a-number"
            )
            val domain = document.toDomain()
            assertNull(domain.percentage)
        }

        @Test
        fun `maps empty string percentage to null`() {
            val document = ExpenseSplitDocument(
                userId = testUserId,
                amountCents = 5000L,
                percentage = ""
            )
            val domain = document.toDomain()
            assertNull(domain.percentage)
        }

        @Test
        fun `EQUAL splitType maps correctly`() {
            val document = ExpenseSplitDocument(userId = testUserId, splitType = "EQUAL")
            assertEquals(SplitType.EQUAL, document.toDomain().splitType)
        }

        @Test
        fun `EXACT splitType maps correctly`() {
            val document = ExpenseSplitDocument(userId = testUserId, splitType = "EXACT")
            assertEquals(SplitType.EXACT, document.toDomain().splitType)
        }

        @Test
        fun `PERCENT splitType maps correctly`() {
            val document = ExpenseSplitDocument(userId = testUserId, splitType = "PERCENT")
            assertEquals(SplitType.PERCENT, document.toDomain().splitType)
        }

        @Test
        fun `null splitType maps to null`() {
            val document = ExpenseSplitDocument(userId = testUserId)
            assertNull(document.toDomain().splitType)
        }

        @Test
        fun `unknown splitType string maps to null`() {
            val document = ExpenseSplitDocument(userId = testUserId, splitType = "UNKNOWN")
            assertNull(document.toDomain().splitType)
        }
    }

    @Nested
    inner class ToDocument {
        @Test
        fun `maps all fields correctly`() {
            val domain = ExpenseSplit(
                userId = testUserId,
                amountCents = 5000L,
                percentage = BigDecimal("33.33"),
                isExcluded = false,
                isCoveredById = null
            )
            val document = domain.toDocument()
            assertEquals(testUserId, document.userId)
            assertEquals(5000L, document.amountCents)
            assertEquals("33.33", document.percentage)
            assertFalse(document.isExcluded)
            assertNull(document.isCoveredById)
        }

        @Test
        fun `maps isExcluded true correctly`() {
            val domain = ExpenseSplit(
                userId = testUserId,
                amountCents = 0L,
                isExcluded = true
            )
            val document = domain.toDocument()
            assertTrue(document.isExcluded)
        }

        @Test
        fun `maps isCoveredById correctly`() {
            val domain = ExpenseSplit(
                userId = testUserId,
                amountCents = 3000L,
                isCoveredById = testCoveredByUserId
            )
            val document = domain.toDocument()
            assertEquals(testCoveredByUserId, document.isCoveredById)
        }

        @Test
        fun `maps null percentage to null`() {
            val domain = ExpenseSplit(
                userId = testUserId,
                amountCents = 5000L,
                percentage = null
            )
            val document = domain.toDocument()
            assertNull(document.percentage)
        }

        @Test
        fun `EQUAL splitType serialises to name string`() {
            val domain = ExpenseSplit(userId = testUserId, amountCents = 0L, splitType = SplitType.EQUAL)
            assertEquals("EQUAL", domain.toDocument().splitType)
        }

        @Test
        fun `EXACT splitType serialises to name string`() {
            val domain = ExpenseSplit(userId = testUserId, amountCents = 0L, splitType = SplitType.EXACT)
            assertEquals("EXACT", domain.toDocument().splitType)
        }

        @Test
        fun `PERCENT splitType serialises to name string`() {
            val domain = ExpenseSplit(userId = testUserId, amountCents = 0L, splitType = SplitType.PERCENT)
            assertEquals("PERCENT", domain.toDocument().splitType)
        }

        @Test
        fun `null splitType serialises to null`() {
            val domain = ExpenseSplit(userId = testUserId, amountCents = 0L)
            assertNull(domain.toDocument().splitType)
        }
    }

    @Nested
    inner class ListMappers {
        @Test
        fun `toDomainSplits maps list of documents`() {
            val documents = listOf(
                ExpenseSplitDocument(userId = "user-1", amountCents = 3000L),
                ExpenseSplitDocument(userId = "user-2", amountCents = 2000L, isExcluded = true)
            )
            val domainList = documents.toDomainSplits()
            assertEquals(2, domainList.size)
            assertEquals("user-1", domainList[0].userId)
            assertEquals(3000L, domainList[0].amountCents)
            assertFalse(domainList[0].isExcluded)
            assertEquals("user-2", domainList[1].userId)
            assertEquals(2000L, domainList[1].amountCents)
            assertTrue(domainList[1].isExcluded)
        }

        @Test
        fun `toSplitDocuments maps list of domain objects`() {
            val domains = listOf(
                ExpenseSplit(userId = "user-1", amountCents = 3000L),
                ExpenseSplit(userId = "user-2", amountCents = 2000L, isExcluded = true)
            )
            val documentList = domains.toSplitDocuments()
            assertEquals(2, documentList.size)
            assertEquals("user-1", documentList[0].userId)
            assertEquals(3000L, documentList[0].amountCents)
            assertFalse(documentList[0].isExcluded)
            assertEquals("user-2", documentList[1].userId)
            assertEquals(2000L, documentList[1].amountCents)
            assertTrue(documentList[1].isExcluded)
        }

        @Test
        fun `empty list mappings return empty lists`() {
            assertEquals(0, emptyList<ExpenseSplitDocument>().toDomainSplits().size)
            assertEquals(0, emptyList<ExpenseSplit>().toSplitDocuments().size)
        }
    }
}
