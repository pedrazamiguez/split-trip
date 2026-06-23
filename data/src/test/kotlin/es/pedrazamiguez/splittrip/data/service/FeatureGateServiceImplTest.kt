package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedFeature
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedLimit
import es.pedrazamiguez.splittrip.domain.service.featuregate.LimitResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FeatureGateServiceImplTest {

    private lateinit var authenticationService: AuthenticationService
    private lateinit var appConfigRepository: AppConfigRepository
    private lateinit var service: FeatureGateServiceImpl

    @BeforeEach
    fun setUp() {
        authenticationService = mockk()
        appConfigRepository = mockk()
        every { appConfigRepository.maxMembersPerGroup } returns MutableStateFlow(20)
        service = FeatureGateServiceImpl(authenticationService, appConfigRepository)
    }

    @Test
    fun `isFeatureEnabled when anonymous returns false for all gated features`() = runTest {
        // Given
        coEvery { authenticationService.isAnonymous() } returns true

        // Then
        assertFalse(service.isFeatureEnabled(GatedFeature.GROUP_COVER_UPLOAD).first())
        assertFalse(service.isFeatureEnabled(GatedFeature.SUBUNIT_CREATION).first())
        assertFalse(service.isFeatureEnabled(GatedFeature.AI_RECEIPT_SCANNING).first())
    }

    @Test
    fun `isFeatureEnabled when registered returns true for all gated features`() = runTest {
        // Given
        coEvery { authenticationService.isAnonymous() } returns false

        // Then
        assertTrue(service.isFeatureEnabled(GatedFeature.GROUP_COVER_UPLOAD).first())
        assertTrue(service.isFeatureEnabled(GatedFeature.SUBUNIT_CREATION).first())
        assertTrue(service.isFeatureEnabled(GatedFeature.AI_RECEIPT_SCANNING).first())
    }

    @Test
    fun `checkLimit for MAX_GROUPS_COUNT when anonymous blocks after 1 group`() = runTest {
        // Given
        coEvery { authenticationService.isAnonymous() } returns true

        // Then
        assertEquals(LimitResult.Allowed, service.checkLimit(GatedLimit.MAX_GROUPS_COUNT, 0).first())
        assertEquals(
            LimitResult.Blocked(GatedLimit.MAX_GROUPS_COUNT, upgradeRequired = true),
            service.checkLimit(GatedLimit.MAX_GROUPS_COUNT, 1).first()
        )
    }

    @Test
    fun `checkLimit for MAX_GROUPS_COUNT when registered allows infinite groups`() = runTest {
        // Given
        coEvery { authenticationService.isAnonymous() } returns false

        // Then
        assertEquals(LimitResult.Allowed, service.checkLimit(GatedLimit.MAX_GROUPS_COUNT, 0).first())
        assertEquals(LimitResult.Allowed, service.checkLimit(GatedLimit.MAX_GROUPS_COUNT, 1).first())
        assertEquals(LimitResult.Allowed, service.checkLimit(GatedLimit.MAX_GROUPS_COUNT, 100).first())
    }

    @Test
    fun `checkLimit for MAX_MEMBERS_PER_GROUP when anonymous blocks after 3 members`() = runTest {
        // Given
        coEvery { authenticationService.isAnonymous() } returns true

        // Then
        assertEquals(LimitResult.Allowed, service.checkLimit(GatedLimit.MAX_MEMBERS_PER_GROUP, 2).first())
        assertEquals(
            LimitResult.Blocked(GatedLimit.MAX_MEMBERS_PER_GROUP, upgradeRequired = true),
            service.checkLimit(GatedLimit.MAX_MEMBERS_PER_GROUP, 3).first()
        )
    }

    @Test
    fun `checkLimit for MAX_MEMBERS_PER_GROUP when registered blocks after maxMembersPerGroup limit`() = runTest {
        // Given
        coEvery { authenticationService.isAnonymous() } returns false

        // Then (default maxMembersPerGroup is 20)
        assertEquals(LimitResult.Allowed, service.checkLimit(GatedLimit.MAX_MEMBERS_PER_GROUP, 19).first())
        assertEquals(
            LimitResult.Blocked(GatedLimit.MAX_MEMBERS_PER_GROUP, upgradeRequired = false),
            service.checkLimit(GatedLimit.MAX_MEMBERS_PER_GROUP, 20).first()
        )
    }

    @Test
    fun `checkLimit for MAX_MEMBERS_PER_GROUP when registered respects remote config changes`() = runTest {
        // Given
        coEvery { authenticationService.isAnonymous() } returns false
        val maxMembersFlow = MutableStateFlow(20)
        every { appConfigRepository.maxMembersPerGroup } returns maxMembersFlow

        // Then (initial limit: 20)
        assertEquals(LimitResult.Allowed, service.checkLimit(GatedLimit.MAX_MEMBERS_PER_GROUP, 19).first())
        assertEquals(
            LimitResult.Blocked(GatedLimit.MAX_MEMBERS_PER_GROUP, upgradeRequired = false),
            service.checkLimit(GatedLimit.MAX_MEMBERS_PER_GROUP, 20).first()
        )

        // When limit is updated to 5
        maxMembersFlow.value = 5

        // Then (new limit: 5)
        assertEquals(LimitResult.Allowed, service.checkLimit(GatedLimit.MAX_MEMBERS_PER_GROUP, 4).first())
        assertEquals(
            LimitResult.Blocked(GatedLimit.MAX_MEMBERS_PER_GROUP, upgradeRequired = false),
            service.checkLimit(GatedLimit.MAX_MEMBERS_PER_GROUP, 5).first()
        )
    }
}
