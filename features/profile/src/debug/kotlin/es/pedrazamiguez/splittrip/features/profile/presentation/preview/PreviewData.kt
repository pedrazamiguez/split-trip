package es.pedrazamiguez.splittrip.features.profile.presentation.preview

import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.domain.model.User
import java.time.LocalDateTime

val PREVIEW_USER = User(
    userId = "user-1",
    email = "antonio@example.com",
    displayName = "Antonio García",
    profileImagePath = null,
    createdAt = LocalDateTime.of(2025, 6, 15, 10, 0)
)

val PREVIEW_USER_WITH_PHOTO = User(
    userId = "user-2",
    email = "maria@example.com",
    displayName = "Maria López",
    profileImagePath = "https://example.com/avatar.jpg",
    createdAt = LocalDateTime.of(2024, 1, 1, 0, 0)
)

val PREVIEW_USER_PRO = User(
    userId = "user-pro",
    email = "andres@example.com",
    displayName = "Andrés",
    profileImagePath = "https://example.com/avatar.jpg",
    tier = SubscriptionTier.PRO,
    createdAt = LocalDateTime.of(2026, 1, 1, 0, 0)
)

val PREVIEW_USER_NO_NAME = User(
    userId = "user-3",
    email = "test@example.com",
    displayName = null,
    profileImagePath = null,
    createdAt = LocalDateTime.of(2026, 1, 10, 8, 30)
)
