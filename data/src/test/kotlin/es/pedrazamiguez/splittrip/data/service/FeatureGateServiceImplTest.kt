package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
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
    private lateinit var userRepository: UserRepository
    private lateinit var groupRepository: GroupRepository
    private lateinit var service: FeatureGateServiceImpl

    private val subscriptionGatingEnabled = MutableStateFlow(true)
    private val maxOwnedGroupsFree = MutableStateFlow(1)
    private val maxOwnedGroupsPro = MutableStateFlow(100)
    private val maxMembersPerGroupFree = MutableStateFlow(4)
    private val maxMembersPerGroupPro = MutableStateFlow(20)

    @BeforeEach
    fun setUp() {
        authenticationService = mockk()
        appConfigRepository = mockk()
        userRepository = mockk()
        groupRepository = mockk()

        every { appConfigRepository.subscriptionGatingEnabled } returns subscriptionGatingEnabled
        every { appConfigRepository.maxOwnedGroupsFree } returns maxOwnedGroupsFree
        every { appConfigRepository.maxOwnedGroupsPro } returns maxOwnedGroupsPro
        every { appConfigRepository.maxMembersPerGroupFree } returns maxMembersPerGroupFree
        every { appConfigRepository.maxMembersPerGroupPro } returns maxMembersPerGroupPro

        subscriptionGatingEnabled.value = true
        maxOwnedGroupsFree.value = 1
        maxOwnedGroupsPro.value = 100
        maxMembersPerGroupFree.value = 4
        maxMembersPerGroupPro.value = 20

        service = FeatureGateServiceImpl(
            authenticationService = authenticationService,
            appConfigRepository = appConfigRepository,
            userRepository = userRepository,
            groupRepository = groupRepository
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
    fun `isFeatureEnabled when anonymous returns false for all gated features`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns true

        assertFalse(service.isFeatureEnabled(GatedFeature.GROUP_COVER_UPLOAD).first())
        assertFalse(service.isFeatureEnabled(GatedFeature.SUBUNIT_CREATION).first())
        assertFalse(service.isFeatureEnabled(GatedFeature.AI_RECEIPT_SCANNING).first())
        assertFalse(service.isFeatureEnabled(GatedFeature.AD_FREE).first())
    }

    @Test
    fun `isFeatureEnabled with GatedFeature AD_FREE returns true for PRO user and false for FREE user`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns false
        coEvery { userRepository.getCurrentUserProfile() } returns User(
            userId = "user-1",
            displayName = "Pro User",
            email = "pro@example.com",
            tier = SubscriptionTier.PRO
        )

        assertTrue(service.isFeatureEnabled(GatedFeature.AD_FREE).first())

        coEvery { userRepository.getCurrentUserProfile() } returns User(
            userId = "user-2",
            displayName = "Free User",
            email = "free@example.com",
            tier = SubscriptionTier.FREE
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
        coEvery { userRepository.getCurrentUserProfile() } returns User(
            userId = "acting_free",
            email = "free@test.com",
            tier = SubscriptionTier.FREE
        )

        // User-level capability: AI Receipt Scanning is disabled for Free user
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
        coEvery { userRepository.getCurrentUserProfile() } returns User(
            userId = "acting_free",
            email = "free@test.com",
            tier = SubscriptionTier.FREE
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
        coEvery { userRepository.getCurrentUserProfile() } returns User(
            userId = "acting_pro",
            email = "pro@test.com",
            tier = SubscriptionTier.PRO
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
        coEvery { userRepository.getCurrentUserProfile() } returns null

        // Profile null -> acting user falls back to FREE
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
        coEvery { userRepository.getCurrentUserProfile() } returns User(
            userId = "acting_pro",
            email = "pro@test.com",
            tier = SubscriptionTier.PRO
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
        coEvery { userRepository.getCurrentUserProfile() } returns User(
            userId = "acting_pro",
            email = "pro@test.com",
            tier = SubscriptionTier.PRO
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
        coEvery { userRepository.getCurrentUserProfile() } returns User(
            userId = "user_free",
            email = "free@test.com",
            tier = SubscriptionTier.FREE
        )
        assertFalse(service.isActingUserPro().first())

        // Authenticated Pro user -> true
        coEvery { userRepository.getCurrentUserProfile() } returns User(
            userId = "user_pro",
            email = "pro@test.com",
            tier = SubscriptionTier.PRO
        )
        assertTrue(service.isActingUserPro().first())
    }
}
