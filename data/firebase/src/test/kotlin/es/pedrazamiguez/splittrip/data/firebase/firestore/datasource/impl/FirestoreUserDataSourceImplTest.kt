package es.pedrazamiguez.splittrip.data.firebase.firestore.datasource.impl

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Transaction
import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.domain.model.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("FirestoreUserDataSourceImpl")
class FirestoreUserDataSourceImplTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var dataSource: FirestoreUserDataSourceImpl

    private val testUserId = "firebase-uid-123"
    private val testEmail = "user@example.com"
    private val testDisplayName = "Test User"
    private val testPhotoUrl = "https://example.com/photo.jpg"

    @BeforeEach
    fun setUp() {
        firestore = mockk(relaxed = true)
        dataSource = FirestoreUserDataSourceImpl(firestore)
    }

    // region Helper methods

    /**
     * Mocks [FirebaseFirestore.runTransaction] to capture and execute the transaction
     * function with a mocked [Transaction], then return a completed [Task].
     *
     * @param existingDoc Mocked snapshot returned by [Transaction.get] - controls
     *                    whether the user document "exists" inside the transaction.
     * @return The mocked [Transaction] for verification.
     */
    private fun mockTransaction(existingDoc: DocumentSnapshot): Transaction {
        val transaction = mockk<Transaction>(relaxed = true)

        every { transaction.get(any()) } returns existingDoc
        every { transaction.set(any(), any(), any<SetOptions>()) } returns transaction

        every { firestore.runTransaction<Void>(any()) } answers {
            val function = firstArg<Transaction.Function<Void>>()
            function.apply(transaction)
            Tasks.forResult(null)
        }

        return transaction
    }

    private fun mockDocRef(): DocumentReference {
        val collection = mockk<CollectionReference>(relaxed = true)
        val docRef = mockk<DocumentReference>(relaxed = true)
        every { firestore.collection("users") } returns collection
        every { collection.document(testUserId) } returns docRef
        return docRef
    }

    private fun mockExistingDoc(exists: Boolean): DocumentSnapshot {
        val snapshot = mockk<DocumentSnapshot>(relaxed = true)
        every { snapshot.exists() } returns exists
        return snapshot
    }

    // endregion

    @Nested
    @DisplayName("saveUser")
    inner class SaveUser {

        @Test
        fun `new user - writes displayName, profileImagePath, and creation metadata`() = runTest {
            // Given
            val docRef = mockDocRef()
            val existingDoc = mockExistingDoc(exists = false)
            val transaction = mockTransaction(existingDoc)

            val user = User(
                userId = testUserId,
                email = testEmail,
                displayName = testDisplayName,
                profileImagePath = testPhotoUrl
            )

            // When
            dataSource.saveUser(user)

            // Then - capture the data map written to Firestore
            val dataSlot = slot<Map<String, Any>>()
            verify(exactly = 1) {
                transaction.set(docRef, capture(dataSlot), any<SetOptions>())
            }

            val data = dataSlot.captured
            assertEquals(testUserId, data["userId"])
            assertEquals(testEmail, data["email"])
            assertEquals(testDisplayName, data["displayName"])
            assertEquals(testPhotoUrl, data["profileImagePath"])
            assertEquals(testUserId, data["createdBy"])
            assertTrue(data.containsKey("createdAt"))
            assertTrue(data.containsKey("lastUpdatedAt"))
        }

        @Test
        fun `existing user - does NOT overwrite displayName`() = runTest {
            // Given
            val docRef = mockDocRef()
            val existingDoc = mockExistingDoc(exists = true)
            val transaction = mockTransaction(existingDoc)

            val user = User(
                userId = testUserId,
                email = testEmail,
                displayName = testDisplayName,
                profileImagePath = testPhotoUrl
            )

            // When
            dataSource.saveUser(user)

            // Then
            val dataSlot = slot<Map<String, Any>>()
            verify(exactly = 1) {
                transaction.set(docRef, capture(dataSlot), any<SetOptions>())
            }

            val data = dataSlot.captured
            // User-editable field must NOT be present
            assertFalse(data.containsKey("displayName"))
            // Creation metadata must NOT be present
            assertFalse(data.containsKey("createdBy"))
            assertFalse(data.containsKey("createdAt"))
        }

        @Test
        fun `existing user - syncs email but preserves user-editable fields`() = runTest {
            // Given
            val docRef = mockDocRef()
            val existingDoc = mockExistingDoc(exists = true)
            val transaction = mockTransaction(existingDoc)

            val user = User(
                userId = testUserId,
                email = " NewEmail@Example.COM ",
                displayName = "Ignored Name",
                profileImagePath = testPhotoUrl
            )

            // When
            dataSource.saveUser(user)

            // Then
            val dataSlot = slot<Map<String, Any>>()
            verify(exactly = 1) {
                transaction.set(docRef, capture(dataSlot), any<SetOptions>())
            }

            val data = dataSlot.captured
            // Always-synced fields
            assertEquals(testUserId, data["userId"])
            assertEquals("newemail@example.com", data["email"]) // trimmed + lowercased
            assertTrue(data.containsKey("lastUpdatedAt"))
            assertEquals(testUserId, data["lastUpdatedBy"])
            // User-editable fields must NOT be overwritten
            assertFalse(data.containsKey("displayName"))
            assertFalse(data.containsKey("profileImagePath"))
        }

        @Test
        fun `new user with null displayName - does not include displayName key`() = runTest {
            // Given
            val docRef = mockDocRef()
            val existingDoc = mockExistingDoc(exists = false)
            val transaction = mockTransaction(existingDoc)

            val user = User(
                userId = testUserId,
                email = testEmail,
                displayName = null,
                profileImagePath = null
            )

            // When
            dataSource.saveUser(user)

            // Then
            val dataSlot = slot<Map<String, Any>>()
            verify(exactly = 1) {
                transaction.set(docRef, capture(dataSlot), any<SetOptions>())
            }

            val data = dataSlot.captured
            assertFalse(data.containsKey("displayName"))
            assertFalse(data.containsKey("profileImagePath"))
            // Creation metadata is still present for new users
            assertTrue(data.containsKey("createdBy"))
            assertTrue(data.containsKey("createdAt"))
        }

        @Test
        fun `existing user with null profileImagePath - does not include profileImagePath key`() = runTest {
            // Given
            val docRef = mockDocRef()
            val existingDoc = mockExistingDoc(exists = true)
            val transaction = mockTransaction(existingDoc)

            val user = User(
                userId = testUserId,
                email = testEmail,
                displayName = testDisplayName,
                profileImagePath = null
            )

            // When
            dataSource.saveUser(user)

            // Then
            val dataSlot = slot<Map<String, Any>>()
            verify(exactly = 1) {
                transaction.set(docRef, capture(dataSlot), any<SetOptions>())
            }

            val data = dataSlot.captured
            assertFalse(data.containsKey("displayName"))
            assertFalse(data.containsKey("profileImagePath"))
        }

        @Test
        fun `new user - writes tier`() = runTest {
            val docRef = mockDocRef()
            val existingDoc = mockExistingDoc(exists = false)
            val transaction = mockTransaction(existingDoc)

            val user = User(
                userId = testUserId,
                email = testEmail,
                tier = SubscriptionTier.PRO
            )

            dataSource.saveUser(user)

            val dataSlot = slot<Map<String, Any>>()
            verify(exactly = 1) {
                transaction.set(docRef, capture(dataSlot), any<SetOptions>())
            }

            val data = dataSlot.captured
            assertEquals("PRO", data["tier"])
        }

        @Test
        fun `existing user with missing tier - backfills tier`() = runTest {
            val docRef = mockDocRef()
            val existingDoc = mockExistingDoc(exists = true)
            every { existingDoc.getString("tier") } returns null
            val transaction = mockTransaction(existingDoc)

            val user = User(
                userId = testUserId,
                email = testEmail,
                tier = SubscriptionTier.PRO
            )

            dataSource.saveUser(user)

            val dataSlot = slot<Map<String, Any>>()
            verify(exactly = 1) {
                transaction.set(docRef, capture(dataSlot), any<SetOptions>())
            }

            val data = dataSlot.captured
            assertEquals("PRO", data["tier"])
        }

        @Test
        fun `existing user with existing tier - does NOT overwrite tier`() = runTest {
            val docRef = mockDocRef()
            val existingDoc = mockExistingDoc(exists = true)
            every { existingDoc.getString("tier") } returns "FREE"
            val transaction = mockTransaction(existingDoc)

            val user = User(
                userId = testUserId,
                email = testEmail,
                tier = SubscriptionTier.PRO
            )

            dataSource.saveUser(user)

            val dataSlot = slot<Map<String, Any>>()
            verify(exactly = 1) {
                transaction.set(docRef, capture(dataSlot), any<SetOptions>())
            }

            val data = dataSlot.captured
            assertFalse(data.containsKey("tier"))
        }

        @Test
        fun `existing user with blank email and non-blank Firestore email - does NOT overwrite email`() = runTest {
            val docRef = mockDocRef()
            val existingDoc = mockExistingDoc(exists = true)
            every { existingDoc.getString("email") } returns "saved@example.com"
            val transaction = mockTransaction(existingDoc)

            val user = User(
                userId = testUserId,
                email = ""
            )

            dataSource.saveUser(user)

            val dataSlot = slot<Map<String, Any>>()
            verify(exactly = 1) {
                transaction.set(docRef, capture(dataSlot), any<SetOptions>())
            }

            val data = dataSlot.captured
            assertFalse(data.containsKey("email"))
        }

        @Test
        fun `existing user with blank email and missing Firestore email - writes blank email`() = runTest {
            val docRef = mockDocRef()
            val existingDoc = mockExistingDoc(exists = true)
            every { existingDoc.getString("email") } returns null
            val transaction = mockTransaction(existingDoc)

            val user = User(
                userId = testUserId,
                email = ""
            )

            dataSource.saveUser(user)

            val dataSlot = slot<Map<String, Any>>()
            verify(exactly = 1) {
                transaction.set(docRef, capture(dataSlot), any<SetOptions>())
            }

            val data = dataSlot.captured
            assertEquals("", data["email"])
        }

        @Test
        fun `existing user with non-blank email and existing Firestore email - updates email`() = runTest {
            val docRef = mockDocRef()
            val existingDoc = mockExistingDoc(exists = true)
            every { existingDoc.getString("email") } returns "old@example.com"
            val transaction = mockTransaction(existingDoc)

            val user = User(
                userId = testUserId,
                email = "new@example.com"
            )

            dataSource.saveUser(user)

            val dataSlot = slot<Map<String, Any>>()
            verify(exactly = 1) {
                transaction.set(docRef, capture(dataSlot), any<SetOptions>())
            }

            val data = dataSlot.captured
            assertEquals("new@example.com", data["email"])
        }
    }

    @Nested
    @DisplayName("updateUserTier")
    inner class UpdateUserTier {

        @Test
        fun `merges tier and timestamp into user document`() = runTest {
            val docRef = mockDocRef()
            every { docRef.set(any(), any<SetOptions>()) } returns Tasks.forResult(null)

            dataSource.updateUserTier(testUserId, SubscriptionTier.PRO)

            val dataSlot = slot<Map<String, Any>>()
            verify(exactly = 1) {
                docRef.set(capture(dataSlot), SetOptions.merge())
            }

            val data = dataSlot.captured
            assertEquals("PRO", data["tier"])
            assertEquals(testUserId, data["lastUpdatedBy"])
            assertTrue(data.containsKey("lastUpdatedAt"))
        }
    }
}
