package es.pedrazamiguez.splittrip.data.firebase.firestore.datasource.impl

import com.google.firebase.Timestamp
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.CashWithdrawalDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.ContributionDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.ExpenseDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.ExpenseSplitDocument
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ReconciliationMappersTest {

    private val pendingUserId = "pending-user-123"
    private val activeUserId = "active-user-456"
    private val otherUserId = "other-user-789"

    @Nested
    inner class ExpenseDocumentReconciliation {

        @Test
        fun `updates payerId when matching pending user`() {
            val doc = ExpenseDocument(
                expenseId = "exp-1",
                payerId = pendingUserId,
                createdBy = otherUserId,
                serverUpdatedAt = Timestamp(100L, 0)
            )

            val updated = doc.getUpdatedIfNeedsUpdate(pendingUserId, activeUserId)

            assertNotNull(updated)
            assertEquals(activeUserId, updated?.payerId)
            assertEquals(otherUserId, updated?.createdBy)
            assertNull(updated?.serverUpdatedAt)
        }

        @Test
        fun `updates createdBy when matching pending user`() {
            val doc = ExpenseDocument(
                expenseId = "exp-1",
                payerId = otherUserId,
                createdBy = pendingUserId
            )

            val updated = doc.getUpdatedIfNeedsUpdate(pendingUserId, activeUserId)

            assertNotNull(updated)
            assertEquals(otherUserId, updated?.payerId)
            assertEquals(activeUserId, updated?.createdBy)
        }

        @Test
        fun `updates nested splits when split matches pending user`() {
            val doc = ExpenseDocument(
                expenseId = "exp-1",
                payerId = otherUserId,
                createdBy = otherUserId,
                splits = listOf(
                    ExpenseSplitDocument(userId = pendingUserId),
                    ExpenseSplitDocument(userId = otherUserId)
                )
            )

            val updated = doc.getUpdatedIfNeedsUpdate(pendingUserId, activeUserId)

            assertNotNull(updated)
            assertEquals(activeUserId, updated?.splits?.get(0)?.userId)
            assertEquals(otherUserId, updated?.splits?.get(1)?.userId)
        }

        @Test
        fun `returns null when no fields match pending user`() {
            val doc = ExpenseDocument(
                expenseId = "exp-1",
                payerId = otherUserId,
                createdBy = otherUserId,
                splits = listOf(
                    ExpenseSplitDocument(userId = otherUserId)
                )
            )

            val updated = doc.getUpdatedIfNeedsUpdate(pendingUserId, activeUserId)

            assertNull(updated)
        }
    }

    @Nested
    inner class ExpenseSplitDocumentReconciliation {

        @Test
        fun `updates userId when matching pending user`() {
            val split = ExpenseSplitDocument(
                userId = pendingUserId,
                isCoveredById = otherUserId
            )

            val updated = split.getUpdatedIfNeedsUpdate(pendingUserId, activeUserId)

            assertNotNull(updated)
            assertEquals(activeUserId, updated?.userId)
            assertEquals(otherUserId, updated?.isCoveredById)
        }

        @Test
        fun `updates isCoveredById when matching pending user`() {
            val split = ExpenseSplitDocument(
                userId = otherUserId,
                isCoveredById = pendingUserId
            )

            val updated = split.getUpdatedIfNeedsUpdate(pendingUserId, activeUserId)

            assertNotNull(updated)
            assertEquals(otherUserId, updated?.userId)
            assertEquals(activeUserId, updated?.isCoveredById)
        }

        @Test
        fun `returns null when split does not match pending user`() {
            val split = ExpenseSplitDocument(
                userId = otherUserId,
                isCoveredById = otherUserId
            )

            val updated = split.getUpdatedIfNeedsUpdate(pendingUserId, activeUserId)

            assertNull(updated)
        }
    }

    @Nested
    inner class ContributionDocumentReconciliation {

        @Test
        fun `updates userId when matching pending user`() {
            val doc = ContributionDocument(
                contributionId = "c-1",
                userId = pendingUserId,
                createdBy = otherUserId,
                serverUpdatedAt = Timestamp(100L, 0)
            )

            val updated = doc.getUpdatedIfNeedsUpdate(pendingUserId, activeUserId)

            assertNotNull(updated)
            assertEquals(activeUserId, updated?.userId)
            assertEquals(otherUserId, updated?.createdBy)
            assertNull(updated?.serverUpdatedAt)
        }

        @Test
        fun `updates createdBy when matching pending user`() {
            val doc = ContributionDocument(
                contributionId = "c-1",
                userId = otherUserId,
                createdBy = pendingUserId
            )

            val updated = doc.getUpdatedIfNeedsUpdate(pendingUserId, activeUserId)

            assertNotNull(updated)
            assertEquals(otherUserId, updated?.userId)
            assertEquals(activeUserId, updated?.createdBy)
        }

        @Test
        fun `returns null when no fields match pending user`() {
            val doc = ContributionDocument(
                contributionId = "c-1",
                userId = otherUserId,
                createdBy = otherUserId
            )

            val updated = doc.getUpdatedIfNeedsUpdate(pendingUserId, activeUserId)

            assertNull(updated)
        }
    }

    @Nested
    inner class CashWithdrawalDocumentReconciliation {

        @Test
        fun `updates withdrawnBy when matching pending user`() {
            val doc = CashWithdrawalDocument(
                withdrawalId = "w-1",
                withdrawnBy = pendingUserId,
                createdBy = otherUserId,
                serverUpdatedAt = Timestamp(100L, 0)
            )

            val updated = doc.getUpdatedIfNeedsUpdate(pendingUserId, activeUserId)

            assertNotNull(updated)
            assertEquals(activeUserId, updated?.withdrawnBy)
            assertEquals(otherUserId, updated?.createdBy)
            assertNull(updated?.serverUpdatedAt)
        }

        @Test
        fun `updates createdBy when matching pending user`() {
            val doc = CashWithdrawalDocument(
                withdrawalId = "w-1",
                withdrawnBy = otherUserId,
                createdBy = pendingUserId
            )

            val updated = doc.getUpdatedIfNeedsUpdate(pendingUserId, activeUserId)

            assertNotNull(updated)
            assertEquals(otherUserId, updated?.withdrawnBy)
            assertEquals(activeUserId, updated?.createdBy)
        }

        @Test
        fun `returns null when no fields match pending user`() {
            val doc = CashWithdrawalDocument(
                withdrawalId = "w-1",
                withdrawnBy = otherUserId,
                createdBy = otherUserId
            )

            val updated = doc.getUpdatedIfNeedsUpdate(pendingUserId, activeUserId)

            assertNull(updated)
        }
    }
}
