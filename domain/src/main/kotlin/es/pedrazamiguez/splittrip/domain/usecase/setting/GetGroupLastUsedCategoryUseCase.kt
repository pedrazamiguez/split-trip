package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.usecase.UseCase
import kotlinx.coroutines.flow.Flow

interface GetGroupLastUsedCategoryUseCase : UseCase {
    operator fun invoke(groupId: String): Flow<List<String>>
}
