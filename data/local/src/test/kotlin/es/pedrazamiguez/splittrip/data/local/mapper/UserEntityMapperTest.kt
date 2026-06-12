package es.pedrazamiguez.splittrip.data.local.mapper

import es.pedrazamiguez.splittrip.data.local.entity.UserEntity
import es.pedrazamiguez.splittrip.domain.model.User
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UserEntityMapperTest {

    private val testTimestamp = LocalDateTime.of(2026, 3, 15, 10, 30, 0)
    private val testTimestampMillis = testTimestamp
        .toInstant(ZoneOffset.UTC)
        .toEpochMilli()

    private val fullEntity = UserEntity(
        userId = "user-1",
        email = "alice@example.com",
        displayName = "Alice",
        profileImagePath = "images/alice.jpg",
        createdAtMillis = testTimestampMillis,
        lastUpdatedAtMillis = testTimestampMillis,
        bio = "Hello",
        syncStatus = "PENDING_SYNC"
    )

    @Nested
    inner class ToDomain {

        @Test
        fun `maps all fields correctly`() {
            val user = fullEntity.toDomain()

            assertEquals("user-1", user.userId)
            assertEquals("alice@example.com", user.email)
            assertEquals("Alice", user.displayName)
            assertEquals("images/alice.jpg", user.profileImagePath)
            assertEquals(testTimestamp, user.createdAt)
            assertEquals("Hello", user.bio)
            assertEquals(es.pedrazamiguez.splittrip.domain.enums.SyncStatus.PENDING_SYNC, user.syncStatus)
        }

        @Test
        fun `null createdAtMillis maps to null createdAt`() {
            val entity = fullEntity.copy(createdAtMillis = null)
            val user = entity.toDomain()
            assertNull(user.createdAt)
        }

        @Test
        fun `null profileImagePath maps to null`() {
            val entity = fullEntity.copy(profileImagePath = null)
            val user = entity.toDomain()
            assertNull(user.profileImagePath)
        }
    }

    @Nested
    inner class ToEntity {

        private val fullUser = User(
            userId = "user-1",
            email = "alice@example.com",
            displayName = "Alice",
            profileImagePath = "images/alice.jpg",
            createdAt = testTimestamp,
            bio = "Hello",
            syncStatus = es.pedrazamiguez.splittrip.domain.enums.SyncStatus.PENDING_SYNC
        )

        @Test
        fun `maps all fields correctly`() {
            val entity = fullUser.toEntity()

            assertEquals("user-1", entity.userId)
            assertEquals("alice@example.com", entity.email)
            assertEquals("Alice", entity.displayName)
            assertEquals("images/alice.jpg", entity.profileImagePath)
            assertEquals(testTimestampMillis, entity.createdAtMillis)
            assertEquals("Hello", entity.bio)
            assertEquals("PENDING_SYNC", entity.syncStatus)
        }

        @Test
        fun `lastUpdatedAtMillis is set to current time`() {
            val entity = fullUser.toEntity()
            assertNotNull(entity.lastUpdatedAtMillis)
            assertTrue(entity.lastUpdatedAtMillis!! > 0)
        }

        @Test
        fun `null createdAt maps to null createdAtMillis`() {
            val user = fullUser.copy(createdAt = null)
            val entity = user.toEntity()
            assertNull(entity.createdAtMillis)
        }
    }

    @Nested
    inner class ListExtensions {

        @Test
        fun `toDomain maps list of entities`() {
            val entities = listOf(fullEntity, fullEntity.copy(userId = "user-2"))
            val users = entities.toDomain()
            assertEquals(2, users.size)
            assertEquals("user-2", users[1].userId)
        }

        @Test
        fun `toEntities maps list of domain objects`() {
            val user = fullEntity.toDomain()
            val entities = listOf(user, user.copy(userId = "user-2")).toEntities()
            assertEquals(2, entities.size)
            assertEquals("user-2", entities[1].userId)
        }
    }
}
