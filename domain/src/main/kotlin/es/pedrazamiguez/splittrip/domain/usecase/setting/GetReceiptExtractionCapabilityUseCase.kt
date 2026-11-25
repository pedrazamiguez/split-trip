package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.model.ExtractionCapability
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface GetReceiptExtractionCapabilityUseCase : UseCase {
    operator fun invoke(): ExtractionCapability
}
