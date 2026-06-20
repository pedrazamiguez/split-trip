package es.pedrazamiguez.splittrip.data.firebase.firestore.datasource.impl

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.UserDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.mapper.toLocalDateTimeUtc
import es.pedrazamiguez.splittrip.data.firebase.firestore.mapper.toTimestampUtc
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudUserDataSource
import es.pedrazamiguez.splittrip.domain.model.User
import java.util.Date
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class FirestoreUserDataSourceImpl(private val firestore: FirebaseFirestore) : CloudUserDataSource {

    override suspend fun saveUser(user: User) {
        val now = Timestamp(Date())
        val docRef = firestore.collection(UserDocument.COLLECTION_PATH)
            .document(user.userId)

        firestore.runTransaction { transaction ->
            val existingDoc = transaction.get(docRef)

            val data = mutableMapOf<String, Any>(
                "userId" to user.userId,
                "email" to user.email.trim().lowercase(),
                "lastUpdatedBy" to user.userId,
                "lastUpdatedAt" to now,
                "isPending" to user.isPending
            )

            val userCreatedAtTimestamp = user.createdAt.toTimestampUtc()

            if (!existingDoc.exists()) {
                // New user — populate user-editable fields from the auth provider
                user.displayName?.let { data["displayName"] = it }
                user.profileImagePath?.let { data["profileImagePath"] = it }
                user.bio?.let { data["bio"] = it }
                data["createdBy"] = user.userId
                data["createdAt"] = userCreatedAtTimestamp ?: now
            } else {
                // Existing user — if createdAt is missing in Firestore, populate it from local
                val existingCreatedAt = existingDoc.get("createdAt")
                if (existingCreatedAt == null && userCreatedAtTimestamp != null) {
                    data["createdAt"] = userCreatedAtTimestamp
                }
            }
            // Existing user — skip displayName and profileImagePath to preserve
            // any user-customised values. Only email and timestamps are synced.

            transaction.set(docRef, data, SetOptions.merge())
        }.await()
    }

    override suspend fun getUsersByIds(userIds: List<String>): List<User> {
        if (userIds.isEmpty()) return emptyList()

        return try {
            // Firestore whereIn has a max of 10 values per query,
            // so we split the IDs into chunks and merge the results.
            val maxInQuerySize = 10
            val distinctIds = userIds.distinct()

            val users = mutableListOf<User>()

            distinctIds.chunked(maxInQuerySize).forEach { chunk ->
                val snapshot = firestore
                    .collection(UserDocument.COLLECTION_PATH)
                    .whereIn("userId", chunk)
                    .get()
                    .await()

                snapshot.documents.mapNotNullTo(users) { doc ->
                    doc.toObject(UserDocument::class.java)?.let { userDoc ->
                        User(
                            userId = userDoc.userId,
                            email = userDoc.email,
                            displayName = userDoc.displayName,
                            profileImagePath = userDoc.profileImagePath,
                            bio = userDoc.bio,
                            createdAt = userDoc.createdAt.toLocalDateTimeUtc(),
                            isPending = userDoc.isPending
                        )
                    }
                }
            }

            users
        } catch (e: Exception) {
            Timber.e(e, "Error fetching users by IDs")
            emptyList()
        }
    }

    override suspend fun searchUsersByEmail(email: String, excludeUserId: String?): List<User> {
        if (email.isBlank()) return emptyList()

        return try {
            val snapshot = firestore
                .collection(UserDocument.COLLECTION_PATH)
                .whereEqualTo("email", email.trim().lowercase())
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(UserDocument::class.java)?.let { userDoc ->
                    if (excludeUserId != null && userDoc.userId == excludeUserId) {
                        null
                    } else {
                        User(
                            userId = userDoc.userId,
                            email = userDoc.email,
                            displayName = userDoc.displayName,
                            profileImagePath = userDoc.profileImagePath,
                            bio = userDoc.bio,
                            createdAt = userDoc.createdAt.toLocalDateTimeUtc(),
                            isPending = userDoc.isPending
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error searching users by email")
            emptyList()
        }
    }

    override suspend fun updateUserProfile(userId: String, displayName: String?, bio: String?, avatarUrl: String?) {
        val docRef = firestore.collection(UserDocument.COLLECTION_PATH).document(userId)
        val updates = mutableMapOf<String, Any?>(
            "displayName" to displayName,
            "bio" to bio,
            "profileImagePath" to avatarUrl,
            "lastUpdatedBy" to userId,
            "lastUpdatedAt" to Timestamp(Date())
        )
        docRef.update(updates).await()
    }

    override suspend fun deleteUser(userId: String) {
        firestore.collection(UserDocument.COLLECTION_PATH).document(userId).delete().await()
    }
}
