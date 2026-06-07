package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface SetAppThemeUseCase : UseCase {
    suspend operator fun invoke(themeCode: String)
}
