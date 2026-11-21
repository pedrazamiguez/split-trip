package es.pedrazamiguez.splittrip.features.settings.presentation.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewLocaleProvider
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.domain.service.featuregate.AiReceiptQuota
import es.pedrazamiguez.splittrip.domain.service.featuregate.MediaUploadQuota
import es.pedrazamiguez.splittrip.domain.service.featuregate.PlanAllowances
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.impl.PlanAllowancesUiMapperImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.PlanAllowancesScreen

@PreviewComplete
@Composable
private fun PlanAllowancesFreeHealthyPreview() {
    val context = LocalContext.current
    val mapper = PlanAllowancesUiMapperImpl(PreviewLocaleProvider(context))
    val uiState = mapper.map(
        PlanAllowances(
            tier = SubscriptionTier.FREE,
            isGatingEnabled = true,
            aiReceiptQuota = AiReceiptQuota(
                usedScans = 1,
                monthlyLimit = 3,
                isProTier = false,
                isGatingEnabled = true
            ),
            mediaUploadQuota = MediaUploadQuota(
                usedUploads = 2,
                monthlyFreeLimit = 5,
                isProTier = false,
                isGatingEnabled = true
            ),
            ownedGroupsUsed = 0,
            maxOwnedGroups = 1,
            maxMembersPerGroup = 4,
            maxSubunitsPerGroup = 1,
            isAdFreeEnabled = false
        )
    )

    PreviewThemeWrapper {
        PlanAllowancesScreen(uiState = uiState, onEvent = {})
    }
}

@PreviewComplete
@Composable
private fun PlanAllowancesFreeExhaustedPreview() {
    val context = LocalContext.current
    val mapper = PlanAllowancesUiMapperImpl(PreviewLocaleProvider(context))
    val uiState = mapper.map(
        PlanAllowances(
            tier = SubscriptionTier.FREE,
            isGatingEnabled = true,
            aiReceiptQuota = AiReceiptQuota(
                usedScans = 3,
                monthlyLimit = 3,
                isProTier = false,
                isGatingEnabled = true
            ),
            mediaUploadQuota = MediaUploadQuota(
                usedUploads = 5,
                monthlyFreeLimit = 5,
                isProTier = false,
                isGatingEnabled = true
            ),
            ownedGroupsUsed = 1,
            maxOwnedGroups = 1,
            maxMembersPerGroup = 4,
            maxSubunitsPerGroup = 1,
            isAdFreeEnabled = false
        )
    )

    PreviewThemeWrapper {
        PlanAllowancesScreen(uiState = uiState, onEvent = {})
    }
}

@PreviewComplete
@Composable
private fun PlanAllowancesProPreview() {
    val context = LocalContext.current
    val mapper = PlanAllowancesUiMapperImpl(PreviewLocaleProvider(context))
    val uiState = mapper.map(
        PlanAllowances(
            tier = SubscriptionTier.PRO,
            isGatingEnabled = true,
            aiReceiptQuota = AiReceiptQuota(
                usedScans = 12,
                monthlyLimit = 100,
                isProTier = true,
                isGatingEnabled = true
            ),
            mediaUploadQuota = MediaUploadQuota(
                usedUploads = 18,
                monthlyFreeLimit = 5,
                isProTier = true,
                isGatingEnabled = true
            ),
            ownedGroupsUsed = 4,
            maxOwnedGroups = 100,
            maxMembersPerGroup = 20,
            maxSubunitsPerGroup = null,
            isAdFreeEnabled = true
        )
    )

    PreviewThemeWrapper {
        PlanAllowancesScreen(uiState = uiState, onEvent = {})
    }
}

@PreviewComplete
@Composable
private fun PlanAllowancesUngatedPreview() {
    val context = LocalContext.current
    val mapper = PlanAllowancesUiMapperImpl(PreviewLocaleProvider(context))
    val uiState = mapper.map(
        PlanAllowances(
            tier = SubscriptionTier.FREE,
            isGatingEnabled = false,
            aiReceiptQuota = AiReceiptQuota(
                usedScans = 2,
                monthlyLimit = 3,
                isProTier = false,
                isGatingEnabled = false
            ),
            mediaUploadQuota = MediaUploadQuota(
                usedUploads = 3,
                monthlyFreeLimit = 5,
                isProTier = false,
                isGatingEnabled = false
            ),
            ownedGroupsUsed = 2,
            maxOwnedGroups = 1,
            maxMembersPerGroup = 4,
            maxSubunitsPerGroup = null,
            isAdFreeEnabled = true
        )
    )

    PreviewThemeWrapper {
        PlanAllowancesScreen(uiState = uiState, onEvent = {})
    }
}
