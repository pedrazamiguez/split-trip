package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedFeature
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedLimit
import es.pedrazamiguez.splittrip.domain.service.featuregate.LimitResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FeatureGateServiceQuotaAndSubunitsTest {

    private lateinit var authenticationService: AuthenticationService
    private lateinit var appConfigRepository: AppConfigRepository
    private lateinit var userRepository: UserRepository
    private lateinit var groupRepository: GroupRepository
    private lateinit var userPreferenceRepository: UserPreferenceRepository
    private lateinit var service: FeatureGateServiceImpl

    private val fixedYearMonth = YearMonth.of(2026, 11)
    private val subscriptionGatingEnabled = MutableStateFlow(true)
    private val manualReceiptMonthlyLimitFree = MutableStateFlow(5)
    private val maxSubunitsPerGroupFree = MutableStateFlow(1)
    private val maxSubunitsPerGroupPro = MutableStateFlow(-1)
    private val monthlyManualReceiptCount = MutableStateFlow(0)
    private val proAllowlistEmailHashes = MutableStateFlow<Set<String>>(emptySet())

    @BeforeEach
    fun setUp() {
        authenticationService = mockk()
        appConfigRepository = mockk(relaxed = true)
        userRepository = mockk()
        groupRepository = mockk()
        userPreferenceRepository = mockk(relaxed = true)

        every { appConfigRepository.subscriptionGatingEnabled } returns subscriptionGatingEnabled
        every { appConfigRepository.manualReceiptMonthlyLimitFree } returns manualReceiptMonthlyLimitFree
        every { appConfigRepository.maxSubunitsPerGroupFree } returns maxSubunitsPerGroupFree
        every { appConfigRepository.maxSubunitsPerGroupPro } returns maxSubunitsPerGroupPro
        every { appConfigRepository.proAllowlistEmailHashes } returns proAllowlistEmailHashes
        every {
            userPreferenceRepository.getMonthlyManualReceiptAttachmentCount(fixedYearMonth)
        } returns monthlyManualReceiptCount
        every { userRepository.observeCurrentUserProfile() } returns flowOf(null)
        every { authenticationService.currentUserEmail() } returns null

        service = FeatureGateServiceImpl(
            authenticationService = authenticationService,
            appConfigRepository = appConfigRepository,
            userRepository = userRepository,
            groupRepository = groupRepository,
            userPreferenceRepository = userPreferenceRepository,
            yearMonthProvider = { fixedYearMonth }
        )
    }

    @Test
    fun `checkLimit MAX_SUBUNITS_PER_GROUP allows first subunit on Free and blocks second`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns false
        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(userId = "free-user", email = "free@test.com", tier = SubscriptionTier.FREE)
        )
        val freeGroup = Group(id = "group-free", createdBy = "creator-free")
        val freeCreator = User(userId = "creator-free", email = "creator@test.com", tier = SubscriptionTier.FREE)
        coEvery { groupRepository.getGroupById("group-free") } returns freeGroup
        coEvery { userRepository.getUsersByIds(listOf("creator-free")) } returns mapOf("creator-free" to freeCreator)

        assertEquals(
            LimitResult.Allowed,
            service.checkLimit(GatedLimit.MAX_SUBUNITS_PER_GROUP, currentCount = 0, groupId = "group-free").first()
        )
        assertEquals(
            LimitResult.Blocked(GatedLimit.MAX_SUBUNITS_PER_GROUP, upgradeRequired = true),
            service.checkLimit(GatedLimit.MAX_SUBUNITS_PER_GROUP, currentCount = 1, groupId = "group-free").first()
        )
    }

    @Test
    fun `checkLimit MAX_SUBUNITS_PER_GROUP preserves collaborative Pro access`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns false

        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(userId = "free-user", email = "free@test.com", tier = SubscriptionTier.FREE)
        )
        val proGroup = Group(id = "group-pro", createdBy = "creator-pro")
        val proCreator = User(userId = "creator-pro", email = "pro@test.com", tier = SubscriptionTier.PRO)
        coEvery { groupRepository.getGroupById("group-pro") } returns proGroup
        coEvery { userRepository.getUsersByIds(listOf("creator-pro")) } returns mapOf("creator-pro" to proCreator)

        assertEquals(
            LimitResult.Allowed,
            service.checkLimit(GatedLimit.MAX_SUBUNITS_PER_GROUP, currentCount = 5, groupId = "group-pro").first()
        )

        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(userId = "pro-user", email = "pro@test.com", tier = SubscriptionTier.PRO)
        )
        val freeGroup = Group(id = "group-free", createdBy = "creator-free")
        val freeCreator = User(userId = "creator-free", email = "creator@test.com", tier = SubscriptionTier.FREE)
        coEvery { groupRepository.getGroupById("group-free") } returns freeGroup
        coEvery { userRepository.getUsersByIds(listOf("creator-free")) } returns mapOf("creator-free" to freeCreator)

        assertEquals(
            LimitResult.Allowed,
            service.checkLimit(GatedLimit.MAX_SUBUNITS_PER_GROUP, currentCount = 5, groupId = "group-free").first()
        )
    }

    @Test
    fun `checkLimit MAX_SUBUNITS_PER_GROUP handles finite Pro cap, zero Free cap, and disabled gating`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns false
        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(userId = "pro-user", email = "pro@test.com", tier = SubscriptionTier.PRO)
        )
        maxSubunitsPerGroupPro.value = 3

        assertEquals(
            LimitResult.Blocked(GatedLimit.MAX_SUBUNITS_PER_GROUP, upgradeRequired = false),
            service.checkLimit(GatedLimit.MAX_SUBUNITS_PER_GROUP, currentCount = 3, groupId = "group-any").first()
        )

        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(userId = "free-user", email = "free@test.com", tier = SubscriptionTier.FREE)
        )
        coEvery { groupRepository.getGroupById("group-any") } returns null
        maxSubunitsPerGroupFree.value = 0

        assertEquals(
            LimitResult.Blocked(GatedLimit.MAX_SUBUNITS_PER_GROUP, upgradeRequired = true),
            service.checkLimit(GatedLimit.MAX_SUBUNITS_PER_GROUP, currentCount = 0, groupId = "group-any").first()
        )

        subscriptionGatingEnabled.value = false
        assertEquals(
            LimitResult.Allowed,
            service.checkLimit(GatedLimit.MAX_SUBUNITS_PER_GROUP, currentCount = 5, groupId = "group-any").first()
        )
    }

    @Test
    fun `observeMediaUploadQuota and GROUP_COVER_UPLOAD evaluate shared monthly quota`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns false
        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(userId = "free-user", email = "free@test.com", tier = SubscriptionTier.FREE)
        )

        monthlyManualReceiptCount.value = 4
        val underQuota = service.observeMediaUploadQuota().first()
        assertTrue(underQuota.canUpload)
        assertEquals(1, underQuota.remainingUploads)
        assertTrue(service.isFeatureEnabled(GatedFeature.GROUP_COVER_UPLOAD).first())

        monthlyManualReceiptCount.value = 5
        val atQuota = service.observeMediaUploadQuota().first()
        assertFalse(atQuota.canUpload)
        assertTrue(atQuota.requiresUpgrade)
        assertFalse(service.isFeatureEnabled(GatedFeature.GROUP_COVER_UPLOAD).first())

        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(userId = "pro-user", email = "pro@test.com", tier = SubscriptionTier.PRO)
        )
        monthlyManualReceiptCount.value = 50
        val proQuota = service.observeMediaUploadQuota().first()
        assertTrue(proQuota.isUnlimited)
        assertTrue(proQuota.canUpload)
        assertTrue(service.isFeatureEnabled(GatedFeature.GROUP_COVER_UPLOAD).first())

        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(userId = "free-user", email = "free@test.com", tier = SubscriptionTier.FREE)
        )
        subscriptionGatingEnabled.value = false
        val ungatedQuota = service.observeMediaUploadQuota().first()
        assertTrue(ungatedQuota.isUnlimited)
        assertTrue(ungatedQuota.canUpload)
        assertTrue(service.isFeatureEnabled(GatedFeature.GROUP_COVER_UPLOAD).first())
    }
}
