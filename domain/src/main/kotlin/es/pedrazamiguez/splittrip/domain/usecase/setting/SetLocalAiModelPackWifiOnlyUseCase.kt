package es.pedrazamiguez.splittrip.domain.usecase.setting

interface SetLocalAiModelPackWifiOnlyUseCase {
    suspend operator fun invoke(wifiOnly: Boolean)
}
