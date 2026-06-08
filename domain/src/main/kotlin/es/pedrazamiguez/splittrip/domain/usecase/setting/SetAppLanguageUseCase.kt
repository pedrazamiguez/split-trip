package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface SetAppLanguageUseCase : UseCase {
    suspend operator fun invoke(languageCode: String)
}
