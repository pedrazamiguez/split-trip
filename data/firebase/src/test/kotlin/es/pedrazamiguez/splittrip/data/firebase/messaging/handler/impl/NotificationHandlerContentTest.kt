package es.pedrazamiguez.splittrip.data.firebase.messaging.handler.impl

import android.content.Context
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.data.firebase.R
import es.pedrazamiguez.splittrip.data.firebase.messaging.handler.formatNotificationAmount
import es.pedrazamiguez.splittrip.domain.constant.NotificationChannelId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Notification Handlers — channel routing and metadata")
class NotificationHandlerContentTest {

    private lateinit var context: Context
    private lateinit var localeProvider: LocaleProvider

    private val baseData = mapOf(
        "memberName" to "John",
        "groupName" to "Trip to Paris",
        "groupId" to "group-123",
        "entityId" to "entity-456",
        "amountCents" to "5000",
        "currencyCode" to "EUR",
        "deepLink" to "splittrip://groups/group-123/expenses/entity-456"
    )

    @BeforeEach
    fun setUp() {
        context = mockk(relaxed = true)
        localeProvider = mockk()
        every { localeProvider.getCurrentLocale() } returns Locale.US

        // Stub string resources to return format strings
        every { context.getString(any()) } returns "Fallback"
        every { context.getString(any(), any()) } returns "Formatted"
        every { context.getString(any(), any(), any()) } returns "Formatted with amount"
    }

    @Nested
    @DisplayName("Expense handlers use EXPENSES channel")
    inner class ExpenseHandlers {

        @Test
        fun `ExpenseAddedHandler sets EXPENSES channel`() {
            val handler = ExpenseAddedHandler(context, localeProvider)
            val content = handler.handle(baseData)
            assertEquals(NotificationChannelId.EXPENSES, content.channelId)
            assertEquals("group-123", content.groupId)
            assertNotNull(content.deepLink)
        }

        @Test
        fun `ExpenseUpdatedHandler sets EXPENSES channel`() {
            val handler = ExpenseUpdatedHandler(context, localeProvider)
            val content = handler.handle(baseData)
            assertEquals(NotificationChannelId.EXPENSES, content.channelId)
            assertEquals("group-123", content.groupId)
        }

        @Test
        fun `ExpenseDeletedHandler sets EXPENSES channel`() {
            val handler = ExpenseDeletedHandler(context, localeProvider)
            val content = handler.handle(baseData)
            assertEquals(NotificationChannelId.EXPENSES, content.channelId)
            assertEquals("group-123", content.groupId)
        }

        @Test
        fun `ExpenseAddedHandler falls back to brief body when amount is blank`() {
            val handler = ExpenseAddedHandler(context, localeProvider)
            val dataWithoutAmount = baseData - "amountCents" - "currencyCode"
            handler.handle(dataWithoutAmount)

            verify {
                context.getString(
                    R.string.notification_expense_added_body_brief,
                    "John"
                )
            }
        }

        @Test
        fun `ExpenseUpdatedHandler falls back to brief body when amount is blank`() {
            val handler = ExpenseUpdatedHandler(context, localeProvider)
            val dataWithoutAmount = baseData - "amountCents" - "currencyCode"
            handler.handle(dataWithoutAmount)

            verify {
                context.getString(
                    R.string.notification_expense_updated_body_brief,
                    "John"
                )
            }
        }

        @Test
        fun `ExpenseDeletedHandler falls back to brief body when amount is blank`() {
            val handler = ExpenseDeletedHandler(context, localeProvider)
            val dataWithoutAmount = baseData - "amountCents" - "currencyCode"
            handler.handle(dataWithoutAmount)

            verify {
                context.getString(
                    R.string.notification_expense_deleted_body_brief,
                    "John"
                )
            }
        }
    }

    @Nested
    @DisplayName("Membership handlers use MEMBERSHIP channel")
    inner class MembershipHandlers {

        @Test
        fun `MemberAddedHandler sets MEMBERSHIP channel`() {
            val handler = MemberAddedHandler(context)
            val content = handler.handle(baseData)
            assertEquals(NotificationChannelId.MEMBERSHIP, content.channelId)
            assertEquals("group-123", content.groupId)
        }

        @Test
        fun `MemberRemovedHandler sets MEMBERSHIP channel`() {
            val handler = MemberRemovedHandler(context)
            val content = handler.handle(baseData)
            assertEquals(NotificationChannelId.MEMBERSHIP, content.channelId)
            assertEquals("group-123", content.groupId)
        }

        @Test
        fun `GroupDeletedHandler sets MEMBERSHIP channel`() {
            val handler = GroupDeletedHandler(context)
            val content = handler.handle(baseData)
            assertEquals(NotificationChannelId.MEMBERSHIP, content.channelId)
            assertEquals("group-123", content.groupId)
        }

        @Test
        fun `GroupDeletedHandler uses group name and actor name when group name is present`() {
            val handler = GroupDeletedHandler(context)
            val content = handler.handle(baseData)
            assertEquals("Trip to Paris", content.title)
            verify {
                context.getString(
                    R.string.notification_group_deleted_body,
                    "John",
                    "Trip to Paris"
                )
            }
        }

        @Test
        fun `GroupDeletedHandler falls back to brief body and default title when group name is blank`() {
            val handler = GroupDeletedHandler(context)
            val dataWithoutGroup = baseData - "groupName"
            handler.handle(dataWithoutGroup)

            verify {
                context.getString(R.string.notification_group_deleted_title)
            }
            verify {
                context.getString(
                    R.string.notification_group_deleted_body_brief,
                    "John"
                )
            }
        }
    }

    @Nested
    @DisplayName("MemberAddedHandler admin vs self-join body")
    inner class MemberAddedAdminAction {

        @Test
        fun `uses single-arg getString for self-join when actorName is absent`() {
            val handler = MemberAddedHandler(context)
            handler.handle(baseData)

            verify { context.getString(any(), eq("John")) }
            verify(exactly = 0) { context.getString(any(), any<String>(), eq("John")) }
        }

        @Test
        fun `uses two-arg getString for admin action when actorName is present`() {
            val data = baseData + ("actorName" to "Admin")
            val handler = MemberAddedHandler(context)
            handler.handle(data)

            verify { context.getString(any(), eq("Admin"), eq("John")) }
        }
    }

    @Nested
    @DisplayName("MemberRemovedHandler admin vs self-leave body")
    inner class MemberRemovedAdminAction {

        @Test
        fun `uses single-arg getString for self-leave when actorName is absent`() {
            val handler = MemberRemovedHandler(context)
            handler.handle(baseData)

            verify { context.getString(any(), eq("John")) }
            verify(exactly = 0) { context.getString(any(), any<String>(), eq("John")) }
        }

        @Test
        fun `uses two-arg getString for admin action when actorName is present`() {
            val data = baseData + ("actorName" to "Admin")
            val handler = MemberRemovedHandler(context)
            handler.handle(data)

            verify { context.getString(any(), eq("Admin"), eq("John")) }
        }
    }

    @Nested
    @DisplayName("Financial handlers use FINANCIAL channel")
    inner class FinancialHandlers {

        @Test
        fun `CashWithdrawalHandler sets FINANCIAL channel`() {
            val handler = CashWithdrawalHandler(context, localeProvider)
            val content = handler.handle(baseData)
            assertEquals(NotificationChannelId.FINANCIAL, content.channelId)
            assertEquals("group-123", content.groupId)
        }

        @Test
        fun `ContributionAddedHandler sets FINANCIAL channel`() {
            val handler = ContributionAddedHandler(context, localeProvider)
            val content = handler.handle(baseData)
            assertEquals(NotificationChannelId.FINANCIAL, content.channelId)
            assertEquals("group-123", content.groupId)
        }
    }

    @Nested
    @DisplayName("ContributionAddedHandler self vs impersonation body")
    inner class ContributionAddedImpersonation {

        @Test
        fun `uses two-arg getString for normal contribution when actorName is absent`() {
            val handler = ContributionAddedHandler(context, localeProvider)
            handler.handle(baseData)

            verify { context.getString(any(), eq("John"), any<String>()) }
            verify(exactly = 0) { context.getString(any(), eq("Admin"), eq("John")) }
        }

        @Test
        fun `uses on-behalf-of getString when actorName is present`() {
            val data = baseData + ("actorName" to "Admin")
            val handler = ContributionAddedHandler(context, localeProvider)
            handler.handle(data)

            verify { context.getString(any(), eq("Admin"), eq("John")) }
        }

        @Test
        fun `ContributionAddedHandler falls back to brief body when amount is blank and not impersonation`() {
            val handler = ContributionAddedHandler(context, localeProvider)
            val dataWithoutAmount = baseData - "amountCents" - "currencyCode"
            handler.handle(dataWithoutAmount)

            verify {
                context.getString(
                    R.string.notification_contribution_added_body_brief,
                    "John"
                )
            }
        }
    }

    @Nested
    @DisplayName("CashWithdrawalHandler self vs impersonation body")
    inner class CashWithdrawalImpersonation {

        @Test
        fun `uses two-arg getString for normal withdrawal when actorName is absent`() {
            val handler = CashWithdrawalHandler(context, localeProvider)
            handler.handle(baseData)

            verify { context.getString(any(), eq("John"), any<String>()) }
            verify(exactly = 0) { context.getString(any(), eq("Admin"), eq("John")) }
        }

        @Test
        fun `uses on-behalf-of getString when actorName is present`() {
            val data = baseData + ("actorName" to "Admin")
            val handler = CashWithdrawalHandler(context, localeProvider)
            handler.handle(data)

            verify { context.getString(any(), eq("Admin"), eq("John")) }
        }

        @Test
        fun `CashWithdrawalHandler falls back to brief body when amount is blank and not impersonation`() {
            val handler = CashWithdrawalHandler(context, localeProvider)
            val dataWithoutAmount = baseData - "amountCents" - "currencyCode"
            handler.handle(dataWithoutAmount)

            verify {
                context.getString(
                    R.string.notification_cash_withdrawal_body_brief,
                    "John"
                )
            }
        }
    }

    @Nested
    @DisplayName("SettlementRequestHandler uses FINANCIAL channel")
    inner class SettlementRequestHandlerTests {

        @Test
        fun `SettlementRequestHandler sets FINANCIAL channel`() {
            val handler = SettlementRequestHandler(context, localeProvider)
            val content = handler.handle(baseData)
            assertEquals(NotificationChannelId.FINANCIAL, content.channelId)
            assertEquals("group-123", content.groupId)
            assertNotNull(content.deepLink)
        }

        @Test
        fun `SettlementRequestHandler body contains payer name`() {
            val handler = SettlementRequestHandler(context, localeProvider)

            val content = handler.handle(baseData)

            assertNotNull(content.body)
            assertNotNull(content.title)
        }

        @Test
        fun `SettlementRequestHandler uses payerName and formatted amount in notification body`() {
            val handler = SettlementRequestHandler(context, localeProvider)

            handler.handle(baseData)

            verify {
                context.getString(
                    R.string.notification_settlement_request_body,
                    "John",
                    "€50.00"
                )
            }
        }

        @Test
        fun `SettlementRequestHandler uses localized fallback name when payerName is missing`() {
            val handler = SettlementRequestHandler(context, localeProvider)
            val dataWithoutName = baseData - "memberName" - "payerName" - "actorName"

            handler.handle(dataWithoutName)

            verify {
                context.getString(R.string.notification_fallback_actor_name)
            }
        }

        @Test
        fun `SettlementConfirmedHandler uses actorName and formatted amount in notification body`() {
            val handler = SettlementConfirmedHandler(context, localeProvider)

            handler.handle(baseData)

            verify {
                context.getString(
                    R.string.notification_settlement_confirmed_body,
                    "John",
                    "€50.00"
                )
            }
        }

        @Test
        fun `SettlementDisputedHandler uses actorName and formatted amount in notification body`() {
            val handler = SettlementDisputedHandler(context, localeProvider)

            handler.handle(baseData)

            verify {
                context.getString(
                    R.string.notification_settlement_disputed_body,
                    "John",
                    "€50.00"
                )
            }
        }

        @Test
        fun `SettlementConfirmedHandler falls back to title when groupName is blank`() {
            val handler = SettlementConfirmedHandler(context, localeProvider)
            val dataWithoutGroup = baseData - "groupName"
            handler.handle(dataWithoutGroup)

            verify {
                context.getString(R.string.notification_settlement_confirmed_title)
            }
        }

        @Test
        fun `SettlementDisputedHandler falls back to title when groupName is blank`() {
            val handler = SettlementDisputedHandler(context, localeProvider)
            val dataWithoutGroup = baseData - "groupName"
            handler.handle(dataWithoutGroup)

            verify {
                context.getString(R.string.notification_settlement_disputed_title)
            }
        }
    }

    @Nested
    @DisplayName("DefaultHandler uses DEFAULT channel")
    inner class DefaultHandlerTests {

        @Test
        fun `DefaultHandler sets DEFAULT channel`() {
            val handler = DefaultHandler(context)
            val content = handler.handle(baseData)
            assertEquals(NotificationChannelId.DEFAULT, content.channelId)
        }

        @Test
        fun `DefaultHandler uses string resources for fallback body`() {
            val handler = DefaultHandler(context)
            val content = handler.handle(emptyMap())
            // Should not contain hardcoded English
            assertNotNull(content.title)
            assertNotNull(content.body)
        }
    }

    @Nested
    @DisplayName("Notification IDs are stable and unique")
    inner class NotificationIds {

        @Test
        fun `same data produces same notificationId`() {
            val handler = ExpenseAddedHandler(context, localeProvider)
            val content1 = handler.handle(baseData)
            val content2 = handler.handle(baseData)
            assertEquals(content1.notificationId, content2.notificationId)
        }

        @Test
        fun `different groups produce different notificationIds`() {
            val handler = ExpenseAddedHandler(context, localeProvider)
            val data1 = baseData + ("groupId" to "groupA")
            val data2 = baseData + ("groupId" to "groupB")
            val id1 = handler.handle(data1).notificationId
            val id2 = handler.handle(data2).notificationId
            assertNotEquals(id1, id2)
        }
    }

    @Nested
    @DisplayName("NotificationAmountFormatter fallback")
    inner class NotificationAmountFormatterTests {

        @Test
        fun `falls back to formattedAmount when amountCents is missing`() {
            val data = mapOf("formattedAmount" to "50,00\u00A0€")
            val formatted = formatNotificationAmount(data, localeProvider)
            assertEquals("50,00\u2800€", formatted)
        }

        @Test
        fun `returns empty string when both amountCents and formattedAmount are missing`() {
            val data = emptyMap<String, String>()
            val formatted = formatNotificationAmount(data, localeProvider)
            assertEquals("", formatted)
        }

        @Test
        fun `formats amount using LocaleProvider when amountCents and currencyCode are present`() {
            val data = mapOf("amountCents" to "5000", "currencyCode" to "EUR")
            val formatted = formatNotificationAmount(data, localeProvider)
            assertEquals("€50.00", formatted)
        }
    }
}
