package es.pedrazamiguez.splittrip.features.settings.presentation.mapper

import java.time.LocalDateTime

interface AccountStatusUiMapper {
    fun formatJoinDate(createdAt: LocalDateTime?): String
}
