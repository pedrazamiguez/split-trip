package es.pedrazamiguez.splittrip.data.firebase.messaging.handler.factory

import android.content.Context
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.data.firebase.messaging.handler.impl.CashWithdrawalHandler
import es.pedrazamiguez.splittrip.data.firebase.messaging.handler.impl.ContributionAddedHandler
import es.pedrazamiguez.splittrip.data.firebase.messaging.handler.impl.DefaultHandler
import es.pedrazamiguez.splittrip.data.firebase.messaging.handler.impl.ExpenseAddedHandler
import es.pedrazamiguez.splittrip.data.firebase.messaging.handler.impl.ExpenseDeletedHandler
import es.pedrazamiguez.splittrip.data.firebase.messaging.handler.impl.ExpenseUpdatedHandler
import es.pedrazamiguez.splittrip.data.firebase.messaging.handler.impl.GroupDeletedHandler
import es.pedrazamiguez.splittrip.data.firebase.messaging.handler.impl.MemberAddedHandler
import es.pedrazamiguez.splittrip.data.firebase.messaging.handler.impl.MemberRemovedHandler
import es.pedrazamiguez.splittrip.data.firebase.messaging.handler.impl.RefundableExpenseReminderHandler
import es.pedrazamiguez.splittrip.data.firebase.messaging.handler.impl.ScheduledExpenseReminderHandler
import es.pedrazamiguez.splittrip.data.firebase.messaging.handler.impl.SettlementConfirmedHandler
import es.pedrazamiguez.splittrip.data.firebase.messaging.handler.impl.SettlementDisputedHandler
import es.pedrazamiguez.splittrip.data.firebase.messaging.handler.impl.SettlementRequestHandler
import es.pedrazamiguez.splittrip.domain.enums.NotificationType
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("NotificationHandlerFactory")
class NotificationHandlerFactoryTest {

    private lateinit var factory: NotificationHandlerFactory

    @BeforeEach
    fun setUp() {
        val context: Context = mockk(relaxed = true)
        val localeProvider: LocaleProvider = mockk(relaxed = true)
        factory = NotificationHandlerFactory(context, localeProvider)
    }

    @Test
    fun `EXPENSE_ADDED returns ExpenseAddedHandler`() {
        assertTrue(factory.getHandler(NotificationType.EXPENSE_ADDED) is ExpenseAddedHandler)
    }

    @Test
    fun `EXPENSE_UPDATED returns ExpenseUpdatedHandler`() {
        assertTrue(factory.getHandler(NotificationType.EXPENSE_UPDATED) is ExpenseUpdatedHandler)
    }

    @Test
    fun `EXPENSE_DELETED returns ExpenseDeletedHandler`() {
        assertTrue(factory.getHandler(NotificationType.EXPENSE_DELETED) is ExpenseDeletedHandler)
    }

    @Test
    fun `MEMBER_ADDED returns MemberAddedHandler`() {
        assertTrue(factory.getHandler(NotificationType.MEMBER_ADDED) is MemberAddedHandler)
    }

    @Test
    fun `MEMBER_REMOVED returns MemberRemovedHandler`() {
        assertTrue(factory.getHandler(NotificationType.MEMBER_REMOVED) is MemberRemovedHandler)
    }

    @Test
    fun `CASH_WITHDRAWAL returns CashWithdrawalHandler`() {
        assertTrue(factory.getHandler(NotificationType.CASH_WITHDRAWAL) is CashWithdrawalHandler)
    }

    @Test
    fun `CONTRIBUTION_ADDED returns ContributionAddedHandler`() {
        assertTrue(factory.getHandler(NotificationType.CONTRIBUTION_ADDED) is ContributionAddedHandler)
    }

    @Test
    fun `DEFAULT returns DefaultHandler`() {
        assertTrue(factory.getHandler(NotificationType.DEFAULT) is DefaultHandler)
    }

    @Test
    fun `GROUP_INVITE falls through to DefaultHandler`() {
        assertTrue(factory.getHandler(NotificationType.GROUP_INVITE) is DefaultHandler)
    }

    @Test
    fun `SETTLEMENT_REQUEST returns SettlementRequestHandler`() {
        assertTrue(factory.getHandler(NotificationType.SETTLEMENT_REQUEST) is SettlementRequestHandler)
    }

    @Test
    fun `SETTLEMENT_CONFIRMED returns SettlementConfirmedHandler`() {
        assertTrue(factory.getHandler(NotificationType.SETTLEMENT_CONFIRMED) is SettlementConfirmedHandler)
    }

    @Test
    fun `SETTLEMENT_DISPUTED returns SettlementDisputedHandler`() {
        assertTrue(factory.getHandler(NotificationType.SETTLEMENT_DISPUTED) is SettlementDisputedHandler)
    }

    @Test
    fun `GROUP_DELETED returns GroupDeletedHandler`() {
        assertTrue(factory.getHandler(NotificationType.GROUP_DELETED) is GroupDeletedHandler)
    }

    @Test
    fun `EXPENSE_SCHEDULED_REMINDER returns ScheduledExpenseReminderHandler`() {
        assertTrue(factory.getHandler(NotificationType.EXPENSE_SCHEDULED_REMINDER) is ScheduledExpenseReminderHandler)
    }

    @Test
    fun `EXPENSE_REFUNDABLE_REMINDER returns RefundableExpenseReminderHandler`() {
        assertTrue(factory.getHandler(NotificationType.EXPENSE_REFUNDABLE_REMINDER) is RefundableExpenseReminderHandler)
    }
}
