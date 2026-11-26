package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.model.LocalAiModelPackState
import kotlinx.coroutines.flow.Flow

interface ObserveLocalAiModelPackStateUseCase {
    operator fun invoke(): Flow<LocalAiModelPackState>
}
