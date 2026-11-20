package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.featuregate.AiReceiptQuota
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
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FeatureGateServiceImplTest {

    private lateinit var authenticationService: AuthenticationService
    private lateinit var appConfigRepository: AppConfigRepository
    private lateinit var userRepository: UserRepository
    private lateinit var groupRepository: GroupRepository
    private lateinit var userPreferenceRepository: UserPreferenceRepository
    private lateinit var service: FeatureGateServiceImpl

    private val fixedYearMonth = YearMonth.of(2026, 11)
    private val subscriptionGatingEnabled = MutableStateFlow(true)
    private val maxOwnedGroupsFree = MutableStateFlow(1)
    private val maxOwnedGroupsPro = MutableStateFlow(100)
    private val maxMembersPerGroupFree = MutableStateFlow(4)
    private val maxMembersPerGroupPro = MutableStateFlow(20)
    private val aiReceiptMonthlyLimitFree = MutableStateFlow(3)
    private val aiReceiptMonthlyLimitPro = MutableStateFlow(100)
    private val monthlyScanCount = MutableStateFlow(0)
    private val proAllowlistEmailHashes = MutableStateFlow<Set<String>>(emptySet())

    @BeforeEach
    fun setUp() {
        authenticationService = mockk()
        appConfigRepository = mockk()
        userRepository = mockk()
        groupRepository = mockk()
        userPreferenceRepository = mockk()

        every { appConfigRepository.subscriptionGatingEnabled } returns subscriptionGatingEnabled
        every { appConfigRepository.maxOwnedGroupsFree } returns maxOwnedGroupsFree
        every { appConfigRepository.maxOwnedGroupsPro } returns maxOwnedGroupsPro
        every { appConfigRepository.maxMembersPerGroupFree } returns maxMembersPerGroupFree
        every { appConfigRepository.maxMembersPerGroupPro } returns maxMembersPerGroupPro
        every { appConfigRepository.aiReceiptMonthlyLimitFree } returns aiReceiptMonthlyLimitFree
        every { appConfigRepository.aiReceiptMonthlyLimitPro } returns aiReceiptMonthlyLimitPro
        every { appConfigRepository.proAllowlistEmailHashes } returns proAllowlistEmailHashes
        every { userPreferenceRepository.getMonthlyAiReceiptScanCount(fixedYearMonth) } returns monthlyScanCount
        every { userRepository.observeCurrentUserProfile() } returns flowOf(null)
        every { authenticationService.currentUserEmail() } returns null

        subscriptionGatingEnabled.value = true
        maxOwnedGroupsFree.value = 1
        maxOwnedGroupsPro.value = 100
        maxMembersPerGroupFree.value = 4
        maxMembersPerGroupPro.value = 20
        aiReceiptMonthlyLimitFree.value = 3
        aiReceiptMonthlyLimitPro.value = 100
        monthlyScanCount.value = 0
        proAllowlistEmailHashes.value = emptySet()

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
    fun `when subscription gating is disabled, all features are enabled and limits are allowed`() = runTest {
        subscriptionGatingEnabled.value = false
        coEvery { authenticationService.isAnonymous() } returns true

        assertTrue(service.isFeatureEnabled(GatedFeature.AI_RECEIPT_SCANNING).first())
        assertTrue(service.isFeatureEnabled(GatedFeature.GROUP_COVER_UPLOAD).first())
        assertTrue(service.isFeatureEnabled(GatedFeature.SUBUNIT_CREATION).first())
        assertTrue(service.isFeatureEnabled(GatedFeature.AD_FREE).first())
        assertEquals(LimitResult.Allowed, service.checkLimit(GatedLimit.MAX_OWNED_GROUPS_COUNT, 50).first())
        assertEquals(LimitResult.Allowed, service.checkLimit(GatedLimit.MAX_MEMBERS_PER_GROUP, 50).first())
    }

    @Test
    fun `isFeatureEnabled when anonymous returns false for all gated features when AI limit is zero`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns true
        aiReceiptMonthlyLimitFree.value = 0

        assertFalse(service.isFeatureEnabled(GatedFeature.GROUP_COVER_UPLOAD).first())
        assertFalse(service.isFeatureEnabled(GatedFeature.SUBUNIT_CREATION).first())
        assertFalse(service.isFeatureEnabled(GatedFeature.AI_RECEIPT_SCANNING).first())
        assertFalse(service.isFeatureEnabled(GatedFeature.AD_FREE).first())
    }

    @Test
    fun `isFeatureEnabled with GatedFeature AD_FREE returns true for PRO user and false for FREE user`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns false
        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(
                userId = "user-1",
                displayName = "Pro User",
                email = "pro@example.com",
                tier = SubscriptionTier.PRO
            )
        )

        assertTrue(service.isFeatureEnabled(GatedFeature.AD_FREE).first())

        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(
                userId = "user-2",
                displayName = "Free User",
                email = "free@example.com",
                tier = SubscriptionTier.FREE
            )
        )

        assertFalse(service.isFeatureEnabled(GatedFeature.AD_FREE).first())
    }

    @Test
    fun `checkLimit for MAX_OWNED_GROUPS_COUNT when anonymous blocks after 1 group`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns true

        assertEquals(LimitResult.Allowed, service.checkLimit(GatedLimit.MAX_OWNED_GROUPS_COUNT, 0).first())
        assertEquals(
            LimitResult.Blocked(GatedLimit.MAX_OWNED_GROUPS_COUNT, upgradeRequired = true),
            service.checkLimit(GatedLimit.MAX_OWNED_GROUPS_COUNT, 1).first()
        )
        assertEquals(
            LimitResult.Blocked(GatedLimit.MAX_OWNED_GROUPS_COUNT, upgradeRequired = true),
            service.checkLimit(GatedLimit.MAX_OWNED_GROUPS_COUNT, 2).first()
        )
    }

    @Test
    fun `checkLimit for MAX_MEMBERS_PER_GROUP when anonymous blocks after 4 members`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns true

        assertEquals(LimitResult.Allowed, service.checkLimit(GatedLimit.MAX_MEMBERS_PER_GROUP, 3).first())
        assertEquals(
            LimitResult.Blocked(GatedLimit.MAX_MEMBERS_PER_GROUP, upgradeRequired = true),
            service.checkLimit(GatedLimit.MAX_MEMBERS_PER_GROUP, 4).first()
        )
    }

    @Test
    fun `isFeatureEnabled for acting free user evaluates acting tier and group creator tier correctly`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns false
        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(
                userId = "acting_free",
                email = "free@test.com",
                tier = SubscriptionTier.FREE
            )
        )

        // User-level capability: AI Receipt Scanning is enabled with remaining trial scans, disabled when exhausted
        assertTrue(service.isFeatureEnabled(GatedFeature.AI_RECEIPT_SCANNING).first())
        monthlyScanCount.value = 3
        assertFalse(service.isFeatureEnabled(GatedFeature.AI_RECEIPT_SCANNING).first())

        // Group-level capability without groupId: evaluates acting user (Free) -> false
        assertFalse(service.isFeatureEnabled(GatedFeature.GROUP_COVER_UPLOAD).first())
        assertFalse(service.isFeatureEnabled(GatedFeature.SUBUNIT_CREATION).first())

        // Group created by PRO user
        val proGroup = Group(id = "group_pro", createdBy = "creator_pro")
        val proCreator = User(userId = "creator_pro", email = "pro@test.com", tier = SubscriptionTier.PRO)
        coEvery { groupRepository.getGroupById("group_pro") } returns proGroup
        coEvery { userRepository.getUsersByIds(listOf("creator_pro")) } returns mapOf("creator_pro" to proCreator)

        // Group-level capability sponsored by Pro creator is enabled for Free member!
        assertTrue(service.isFeatureEnabled(GatedFeature.GROUP_COVER_UPLOAD, groupId = "group_pro").first())
        assertTrue(service.isFeatureEnabled(GatedFeature.SUBUNIT_CREATION, groupId = "group_pro").first())

        // Group created by FREE user
        val freeGroup = Group(id = "group_free", createdBy = "creator_free")
        val freeCreator = User(userId = "creator_free", email = "free_creator@test.com", tier = SubscriptionTier.FREE)
        coEvery { groupRepository.getGroupById("group_free") } returns freeGroup
        coEvery { userRepository.getUsersByIds(listOf("creator_free")) } returns mapOf("creator_free" to freeCreator)

        // Group-level capability with Free creator remains disabled
        assertFalse(service.isFeatureEnabled(GatedFeature.GROUP_COVER_UPLOAD, groupId = "group_free").first())
        assertFalse(service.isFeatureEnabled(GatedFeature.SUBUNIT_CREATION, groupId = "group_free").first())
    }

    @Test
    fun `checkLimit evaluates acting tier for owned groups and creator tier for members`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns false
        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(
                userId = "acting_free",
                email = "free@test.com",
                tier = SubscriptionTier.FREE
            )
        )

        // MAX_OWNED_GROUPS_COUNT: Free user can only own 1 group
        assertEquals(LimitResult.Allowed, service.checkLimit(GatedLimit.MAX_OWNED_GROUPS_COUNT, 0).first())
        assertEquals(
            LimitResult.Blocked(GatedLimit.MAX_OWNED_GROUPS_COUNT, upgradeRequired = true),
            service.checkLimit(GatedLimit.MAX_OWNED_GROUPS_COUNT, 1).first()
        )

        // MAX_MEMBERS_PER_GROUP for Pro-sponsored group: up to 20 members
        val proGroup = Group(id = "group_pro", createdBy = "creator_pro")
        val proCreator = User(userId = "creator_pro", email = "pro@test.com", tier = SubscriptionTier.PRO)
        coEvery { groupRepository.getGroupById("group_pro") } returns proGroup
        coEvery { userRepository.getUsersByIds(listOf("creator_pro")) } returns mapOf("creator_pro" to proCreator)

        assertEquals(LimitResult.Allowed, service.checkLimit(GatedLimit.MAX_MEMBERS_PER_GROUP, 4, "group_pro").first())
        assertEquals(LimitResult.Allowed, service.checkLimit(GatedLimit.MAX_MEMBERS_PER_GROUP, 19, "group_pro").first())
        assertEquals(
            LimitResult.Blocked(GatedLimit.MAX_MEMBERS_PER_GROUP, upgradeRequired = false),
            service.checkLimit(GatedLimit.MAX_MEMBERS_PER_GROUP, 20, "group_pro").first()
        )

        // MAX_MEMBERS_PER_GROUP for Free group: up to 4 members
        val freeGroup = Group(id = "group_free", createdBy = "creator_free")
        val freeCreator = User(userId = "creator_free", email = "free_creator@test.com", tier = SubscriptionTier.FREE)
        coEvery { groupRepository.getGroupById("group_free") } returns freeGroup
        coEvery { userRepository.getUsersByIds(listOf("creator_free")) } returns mapOf("creator_free" to freeCreator)

        assertEquals(LimitResult.Allowed, service.checkLimit(GatedLimit.MAX_MEMBERS_PER_GROUP, 3, "group_free").first())
        assertEquals(
            LimitResult.Blocked(GatedLimit.MAX_MEMBERS_PER_GROUP, upgradeRequired = true),
            service.checkLimit(GatedLimit.MAX_MEMBERS_PER_GROUP, 4, "group_free").first()
        )
    }

    @Test
    fun `isFeatureEnabled and checkLimit when acting user is PRO`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns false
        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(
                userId = "acting_pro",
                email = "pro@test.com",
                tier = SubscriptionTier.PRO
            )
        )

        // User-level tool is enabled
        assertTrue(service.isFeatureEnabled(GatedFeature.AI_RECEIPT_SCANNING).first())

        // Group creation defaults (groupId = null) evaluate against acting user (Pro)
        assertTrue(service.isFeatureEnabled(GatedFeature.GROUP_COVER_UPLOAD).first())
        assertTrue(service.isFeatureEnabled(GatedFeature.SUBUNIT_CREATION).first())

        // MAX_OWNED_GROUPS_COUNT: Pro user can own up to 100 groups
        assertEquals(LimitResult.Allowed, service.checkLimit(GatedLimit.MAX_OWNED_GROUPS_COUNT, 50).first())
        assertEquals(
            LimitResult.Blocked(GatedLimit.MAX_OWNED_GROUPS_COUNT, upgradeRequired = false),
            service.checkLimit(GatedLimit.MAX_OWNED_GROUPS_COUNT, 100).first()
        )

        // MAX_MEMBERS_PER_GROUP on creation: allowed up to 20
        assertEquals(LimitResult.Allowed, service.checkLimit(GatedLimit.MAX_MEMBERS_PER_GROUP, 19).first())
        assertEquals(
            LimitResult.Blocked(GatedLimit.MAX_MEMBERS_PER_GROUP, upgradeRequired = false),
            service.checkLimit(GatedLimit.MAX_MEMBERS_PER_GROUP, 20).first()
        )
    }

    @Test
    fun `fallback to FREE when group or creator or current user profile is not found`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns false
        every { userRepository.observeCurrentUserProfile() } returns flowOf(null)

        // Profile null -> acting user falls back to FREE
        aiReceiptMonthlyLimitFree.value = 0
        assertFalse(service.isFeatureEnabled(GatedFeature.AI_RECEIPT_SCANNING).first())
        assertEquals(
            LimitResult.Blocked(GatedLimit.MAX_OWNED_GROUPS_COUNT, upgradeRequired = true),
            service.checkLimit(GatedLimit.MAX_OWNED_GROUPS_COUNT, 1).first()
        )

        // Group not found -> falls back to acting user tier (FREE)
        coEvery { groupRepository.getGroupById("missing_group") } returns null
        assertFalse(service.isFeatureEnabled(GatedFeature.GROUP_COVER_UPLOAD, "missing_group").first())

        // Group creator not found -> falls back to FREE
        val groupWithUnknownCreator = Group(id = "group_unknown", createdBy = "unknown_user")
        coEvery { groupRepository.getGroupById("group_unknown") } returns groupWithUnknownCreator
        coEvery { userRepository.getUsersByIds(listOf("unknown_user")) } returns emptyMap()
        assertFalse(service.isFeatureEnabled(GatedFeature.GROUP_COVER_UPLOAD, "group_unknown").first())
    }

    @Test
    fun `checkLimit respects dynamic remote config changes`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns false
        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(
                userId = "acting_pro",
                email = "pro@test.com",
                tier = SubscriptionTier.PRO
            )
        )

        assertEquals(LimitResult.Allowed, service.checkLimit(GatedLimit.MAX_MEMBERS_PER_GROUP, 19).first())
        assertEquals(
            LimitResult.Blocked(GatedLimit.MAX_MEMBERS_PER_GROUP, upgradeRequired = false),
            service.checkLimit(GatedLimit.MAX_MEMBERS_PER_GROUP, 20).first()
        )

        // Update limit dynamically
        maxMembersPerGroupPro.value = 10

        assertEquals(LimitResult.Allowed, service.checkLimit(GatedLimit.MAX_MEMBERS_PER_GROUP, 9).first())
        assertEquals(
            LimitResult.Blocked(GatedLimit.MAX_MEMBERS_PER_GROUP, upgradeRequired = false),
            service.checkLimit(GatedLimit.MAX_MEMBERS_PER_GROUP, 10).first()
        )
    }

    @Test
    fun `acting PRO in Free group enables SUBUNIT_CREATION but keeps cover creator-bound`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns false
        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(
                userId = "acting_pro",
                email = "pro@test.com",
                tier = SubscriptionTier.PRO
            )
        )

        val freeGroup = Group(id = "group_free", createdBy = "creator_free")
        val freeCreator = User(userId = "creator_free", email = "free_creator@test.com", tier = SubscriptionTier.FREE)
        coEvery { groupRepository.getGroupById("group_free") } returns freeGroup
        coEvery { userRepository.getUsersByIds(listOf("creator_free")) } returns mapOf("creator_free" to freeCreator)

        // Subunit creation is enabled because acting user is PRO
        assertTrue(service.isFeatureEnabled(GatedFeature.SUBUNIT_CREATION, groupId = "group_free").first())

        // Group cover upload remains disabled because creator is FREE
        assertFalse(service.isFeatureEnabled(GatedFeature.GROUP_COVER_UPLOAD, groupId = "group_free").first())
    }

    @Test
    fun `isActingUserPro returns true when user is Pro and false when Free or anonymous`() = runTest {
        // Anonymous user -> false
        coEvery { authenticationService.isAnonymous() } returns true
        assertFalse(service.isActingUserPro().first())

        // Authenticated Free user -> false
        coEvery { authenticationService.isAnonymous() } returns false
        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(
                userId = "user_free",
                email = "free@test.com",
                tier = SubscriptionTier.FREE
            )
        )
        assertFalse(service.isActingUserPro().first())

        // Authenticated Pro user -> true
        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(
                userId = "user_pro",
                email = "pro@test.com",
                tier = SubscriptionTier.PRO
            )
        )
        assertTrue(service.isActingUserPro().first())
    }

    @Test
    fun `isActingUserPro emits false immediately when observed user tier transitions from PRO to FREE`() = runTest {
        val proUser = User(userId = "user-1", email = "user@test.com", tier = SubscriptionTier.PRO)
        val profileFlow = MutableStateFlow<User?>(proUser)
        coEvery { authenticationService.isAnonymous() } returns false
        every { userRepository.observeCurrentUserProfile() } returns profileFlow

        val emissions = mutableListOf<Boolean>()
        val collectJob = launch {
            service.isActingUserPro().take(2).toList(emissions)
        }
        testScheduler.runCurrent()

        profileFlow.value = proUser.copy(tier = SubscriptionTier.FREE)
        collectJob.join()

        assertEquals(listOf(true, false), emissions)
    }

    @Test
    fun `isFeatureEnabled AD_FREE transitions from true to false when user tier downgrades from PRO to FREE`() =
        runTest {
            val proUser = User(userId = "user-1", email = "user@test.com", tier = SubscriptionTier.PRO)
            val profileFlow = MutableStateFlow<User?>(proUser)
            coEvery { authenticationService.isAnonymous() } returns false
            every { userRepository.observeCurrentUserProfile() } returns profileFlow

            val emissions = mutableListOf<Boolean>()
            val collectJob = launch {
                service.isFeatureEnabled(GatedFeature.AD_FREE).take(2).toList(emissions)
            }
            testScheduler.runCurrent()

            profileFlow.value = proUser.copy(tier = SubscriptionTier.FREE)
            collectJob.join()

            assertEquals(listOf(true, false), emissions)
        }

    @Test
    fun `checkLimit transitions Allowed to Blocked when groups exceed free limit on downgrade`() =
        runTest {
            val proUser = User(userId = "user-1", email = "user@test.com", tier = SubscriptionTier.PRO)
            val profileFlow = MutableStateFlow<User?>(proUser)
            coEvery { authenticationService.isAnonymous() } returns false
            every { userRepository.observeCurrentUserProfile() } returns profileFlow

            val emissions = mutableListOf<LimitResult>()
            val collectJob = launch {
                service.checkLimit(GatedLimit.MAX_OWNED_GROUPS_COUNT, currentCount = 2).take(2).toList(emissions)
            }
            testScheduler.runCurrent()

            profileFlow.value = proUser.copy(tier = SubscriptionTier.FREE)
            collectJob.join()

            assertEquals(
                listOf(
                    LimitResult.Allowed,
                    LimitResult.Blocked(GatedLimit.MAX_OWNED_GROUPS_COUNT, upgradeRequired = true)
                ),
                emissions
            )
        }

    @Test
    fun `allowlisted non-anonymous FREE user receives full PRO entitlements across FeatureGateService`() = runTest {
        val allowlistedUser = User(
            userId = "user-vip",
            email = "vip@example.com",
            tier = SubscriptionTier.FREE
        )
        proAllowlistEmailHashes.value = setOf(User.hashEmail("vip@example.com"))
        coEvery { authenticationService.isAnonymous() } returns false
        every { userRepository.observeCurrentUserProfile() } returns flowOf(allowlistedUser)

        assertTrue(service.isActingUserPro().first())
        assertTrue(service.isFeatureEnabled(GatedFeature.AD_FREE).first())
        assertTrue(service.isFeatureEnabled(GatedFeature.AI_RECEIPT_SCANNING).first())
        assertTrue(service.isFeatureEnabled(GatedFeature.SUBUNIT_CREATION).first())
        assertTrue(service.isFeatureEnabled(GatedFeature.GROUP_COVER_UPLOAD, groupId = null).first())
        assertEquals(
            LimitResult.Allowed,
            service.checkLimit(GatedLimit.MAX_OWNED_GROUPS_COUNT, 50).first()
        )
        assertEquals(
            LimitResult.Blocked(GatedLimit.MAX_OWNED_GROUPS_COUNT, upgradeRequired = false),
            service.checkLimit(GatedLimit.MAX_OWNED_GROUPS_COUNT, 100).first()
        )
    }

    @Test
    fun `anonymous user never receives complimentary PRO even when email hash is in proAllowlistEmailHashes`() =
        runTest {
            val user = User(
                userId = "anon-user",
                email = "vip@example.com",
                tier = SubscriptionTier.FREE
            )
            proAllowlistEmailHashes.value = setOf(User.hashEmail("vip@example.com"))
            coEvery { authenticationService.isAnonymous() } returns true
            every { userRepository.observeCurrentUserProfile() } returns flowOf(user)

            assertFalse(service.isActingUserPro().first())
            assertFalse(service.isFeatureEnabled(GatedFeature.AD_FREE).first())
            val aiQuota = service.observeAiReceiptQuota().first()
            assertFalse(aiQuota.isProTier)
            assertEquals(3, aiQuota.monthlyLimit)
            assertEquals(
                LimitResult.Blocked(GatedLimit.MAX_OWNED_GROUPS_COUNT, upgradeRequired = true),
                service.checkLimit(GatedLimit.MAX_OWNED_GROUPS_COUNT, 1).first()
            )
        }

    @Test
    fun `group created by allowlisted FREE user grants PRO limits to FREE members`() =
        runTest {
            val freeMember = User(
                userId = "member-free",
                email = "member@example.com",
                tier = SubscriptionTier.FREE
            )
            val vipCreator = User(
                userId = "creator-vip",
                email = "vip_creator@example.com",
                tier = SubscriptionTier.FREE
            )
            val vipGroup = Group(id = "group-vip", createdBy = "creator-vip")
            proAllowlistEmailHashes.value = setOf(User.hashEmail("vip_creator@example.com"))

            coEvery { authenticationService.isAnonymous() } returns false
            every { userRepository.observeCurrentUserProfile() } returns flowOf(freeMember)
            coEvery { groupRepository.getGroupById("group-vip") } returns vipGroup
            coEvery { userRepository.getUsersByIds(listOf("creator-vip")) } returns mapOf("creator-vip" to vipCreator)

            assertFalse(service.isActingUserPro().first())
            assertTrue(service.isFeatureEnabled(GatedFeature.GROUP_COVER_UPLOAD, groupId = "group-vip").first())
            assertTrue(service.isFeatureEnabled(GatedFeature.SUBUNIT_CREATION, groupId = "group-vip").first())
            assertEquals(
                LimitResult.Allowed,
                service.checkLimit(GatedLimit.MAX_MEMBERS_PER_GROUP, 19, groupId = "group-vip").first()
            )
            assertEquals(
                LimitResult.Blocked(GatedLimit.MAX_MEMBERS_PER_GROUP, upgradeRequired = false),
                service.checkLimit(GatedLimit.MAX_MEMBERS_PER_GROUP, 20, groupId = "group-vip").first()
            )
        }

    @Test
    fun `removing user from proAllowlistEmailHashes reactively revokes complimentary PRO entitlements`() =
        runTest {
            val actingUser = User(
                userId = "user-vip",
                email = "vip@example.com",
                tier = SubscriptionTier.FREE
            )
            val vipCreator = User(
                userId = "creator-vip",
                email = "vip_creator@example.com",
                tier = SubscriptionTier.FREE
            )
            val vipGroup = Group(id = "group-vip", createdBy = "creator-vip")
            val actingHash = User.hashEmail("vip@example.com")
            val creatorHash = User.hashEmail("vip_creator@example.com")

            proAllowlistEmailHashes.value = setOf(actingHash, creatorHash)
            coEvery { authenticationService.isAnonymous() } returns false
            every { userRepository.observeCurrentUserProfile() } returns flowOf(actingUser)
            coEvery { groupRepository.getGroupById("group-vip") } returns vipGroup
            coEvery { userRepository.getUsersByIds(listOf("creator-vip")) } returns mapOf("creator-vip" to vipCreator)

            val actingEmissions = mutableListOf<Boolean>()
            val actingJob = launch {
                service.isActingUserPro().take(2).toList(actingEmissions)
            }
            val groupEmissions = mutableListOf<Boolean>()
            val groupJob = launch {
                service.isFeatureEnabled(GatedFeature.GROUP_COVER_UPLOAD, groupId = "group-vip")
                    .take(2)
                    .toList(groupEmissions)
            }
            testScheduler.runCurrent()

            proAllowlistEmailHashes.value = emptySet()
            actingJob.join()
            groupJob.join()

            assertEquals(listOf(true, false), actingEmissions)
            assertEquals(listOf(true, false), groupEmissions)
        }

    @Test
    fun `allowlisted FREE user with blank cached email receives PRO entitlements via currentUserEmail fallback`() =
        runTest {
            val user = User(
                userId = "user-vip",
                email = "",
                tier = SubscriptionTier.FREE
            )
            proAllowlistEmailHashes.value = setOf(User.hashEmail("vip@example.com"))
            coEvery { authenticationService.isAnonymous() } returns false
            every { authenticationService.currentUserEmail() } returns "vip@example.com"
            every { userRepository.observeCurrentUserProfile() } returns flowOf(user)

            assertTrue(service.isActingUserPro().first())
            assertTrue(service.isFeatureEnabled(GatedFeature.AD_FREE).first())
        }

    @Test
    fun `observeAiReceiptQuota emits remaining scans for Free user and updates reactively when scan count increases`() =
        runTest {
            coEvery { authenticationService.isAnonymous() } returns false
            every { userRepository.observeCurrentUserProfile() } returns flowOf(
                User(userId = "free-user", email = "free@test.com", tier = SubscriptionTier.FREE)
            )

            val emissions = mutableListOf<AiReceiptQuota>()
            val job = launch {
                service.observeAiReceiptQuota()
                    .take(3)
                    .toList(emissions)
            }
            testScheduler.runCurrent()

            monthlyScanCount.value = 1
            testScheduler.runCurrent()
            monthlyScanCount.value = 3
            testScheduler.runCurrent()
            job.join()

            assertEquals(3, emissions.size)
            assertTrue(emissions[0].canScan && !emissions[0].requiresUpgrade && emissions[0].remainingScans == 3)
            assertTrue(emissions[1].canScan && !emissions[1].requiresUpgrade && emissions[1].remainingScans == 2)
            assertTrue(!emissions[2].canScan && emissions[2].requiresUpgrade && emissions[2].remainingScans == 0)
        }

    @Test
    fun `isFeatureEnabled AI_RECEIPT_SCANNING returns true with trial quota and false once exhausted`() =
        runTest {
            coEvery { authenticationService.isAnonymous() } returns false
            every { userRepository.observeCurrentUserProfile() } returns flowOf(
                User(userId = "free-user", email = "free@test.com", tier = SubscriptionTier.FREE)
            )

            monthlyScanCount.value = 2
            assertTrue(service.isFeatureEnabled(GatedFeature.AI_RECEIPT_SCANNING).first())

            monthlyScanCount.value = 3
            assertFalse(service.isFeatureEnabled(GatedFeature.AI_RECEIPT_SCANNING).first())
        }

    @Test
    fun `isFeatureEnabled AI_RECEIPT_SCANNING returns false for Free user when remote config free limit is zero`() =
        runTest {
            coEvery { authenticationService.isAnonymous() } returns false
            every { userRepository.observeCurrentUserProfile() } returns flowOf(
                User(userId = "free-user", email = "free@test.com", tier = SubscriptionTier.FREE)
            )
            aiReceiptMonthlyLimitFree.value = 0
            monthlyScanCount.value = 0

            assertFalse(service.isFeatureEnabled(GatedFeature.AI_RECEIPT_SCANNING).first())
        }

    @Test
    fun `observeAiReceiptQuota evaluates Pro monthly limit and sets requiresUpgrade false when Pro cap reached`() =
        runTest {
            coEvery { authenticationService.isAnonymous() } returns false
            every { userRepository.observeCurrentUserProfile() } returns flowOf(
                User(userId = "pro-user", email = "pro@test.com", tier = SubscriptionTier.PRO)
            )

            monthlyScanCount.value = 99
            val almostExhausted = service.observeAiReceiptQuota().first()
            assertTrue(almostExhausted.canScan)
            assertEquals(1, almostExhausted.remainingScans)

            monthlyScanCount.value = 100
            val exhausted = service.observeAiReceiptQuota().first()
            assertFalse(exhausted.canScan)
            assertFalse(exhausted.requiresUpgrade)
            assertEquals(0, exhausted.remainingScans)
        }

    @Test
    fun `checkLimit for AI_RECEIPT_MONTHLY_SCANS evaluates free and pro limits against acting tier`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns false
        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(userId = "free-user", email = "free@test.com", tier = SubscriptionTier.FREE)
        )

        assertEquals(LimitResult.Allowed, service.checkLimit(GatedLimit.AI_RECEIPT_MONTHLY_SCANS, 2).first())
        assertEquals(
            LimitResult.Blocked(GatedLimit.AI_RECEIPT_MONTHLY_SCANS, upgradeRequired = true),
            service.checkLimit(GatedLimit.AI_RECEIPT_MONTHLY_SCANS, 3).first()
        )

        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(userId = "pro-user", email = "pro@test.com", tier = SubscriptionTier.PRO)
        )

        assertEquals(LimitResult.Allowed, service.checkLimit(GatedLimit.AI_RECEIPT_MONTHLY_SCANS, 99).first())
        assertEquals(
            LimitResult.Blocked(GatedLimit.AI_RECEIPT_MONTHLY_SCANS, upgradeRequired = false),
            service.checkLimit(GatedLimit.AI_RECEIPT_MONTHLY_SCANS, 100).first()
        )
    }

    @Test
    fun `observeAiReceiptQuota respects dynamic remote config changes to aiReceiptMonthlyLimitFree`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns false
        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(userId = "free-user", email = "free@test.com", tier = SubscriptionTier.FREE)
        )
        monthlyScanCount.value = 3

        assertFalse(service.observeAiReceiptQuota().first().canScan)

        aiReceiptMonthlyLimitFree.value = 5

        val updated = service.observeAiReceiptQuota().first()
        assertTrue(updated.canScan)
        assertEquals(2, updated.remainingScans)
    }
}
