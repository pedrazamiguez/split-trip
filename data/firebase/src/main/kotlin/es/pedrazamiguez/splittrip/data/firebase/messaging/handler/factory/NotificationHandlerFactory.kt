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
import es.pedrazamiguez.splittrip.data.firebase.messaging.handler.impl.ScheduledExpenseEffectiveHandler
import es.pedrazamiguez.splittrip.data.firebase.messaging.handler.impl.ScheduledExpenseReminderHandler
import es.pedrazamiguez.splittrip.data.firebase.messaging.handler.impl.SettlementConfirmedHandler
import es.pedrazamiguez.splittrip.data.firebase.messaging.handler.impl.SettlementDisputedHandler
import es.pedrazamiguez.splittrip.data.firebase.messaging.handler.impl.SettlementRequestHandler
import es.pedrazamiguez.splittrip.domain.enums.NotificationType
import es.pedrazamiguez.splittrip.domain.handler.NotificationHandler

class NotificationHandlerFactory(private val context: Context, private val localeProvider: LocaleProvider) {

    fun getHandler(type: NotificationType): NotificationHandler {
        return getExpenseHandler(type)
            ?: getFinancialHandler(type)
            ?: getMembershipHandler(type)
            ?: getReminderHandler(type)
            ?: DefaultHandler(context)
    }

    private fun getExpenseHandler(type: NotificationType): NotificationHandler? = when (type) {
        NotificationType.EXPENSE_ADDED -> ExpenseAddedHandler(context, localeProvider)
        NotificationType.EXPENSE_UPDATED -> ExpenseUpdatedHandler(context, localeProvider)
        NotificationType.EXPENSE_DELETED -> ExpenseDeletedHandler(context, localeProvider)
        else -> null
    }

    private fun getFinancialHandler(type: NotificationType): NotificationHandler? = when (type) {
        NotificationType.CASH_WITHDRAWAL -> CashWithdrawalHandler(context, localeProvider)
        NotificationType.CONTRIBUTION_ADDED -> ContributionAddedHandler(context, localeProvider)
        NotificationType.SETTLEMENT_REQUEST -> SettlementRequestHandler(context, localeProvider)
        NotificationType.SETTLEMENT_CONFIRMED -> SettlementConfirmedHandler(context, localeProvider)
        NotificationType.SETTLEMENT_DISPUTED -> SettlementDisputedHandler(context, localeProvider)
        else -> null
    }

    private fun getMembershipHandler(type: NotificationType): NotificationHandler? = when (type) {
        NotificationType.MEMBER_ADDED -> MemberAddedHandler(context)
        NotificationType.MEMBER_REMOVED -> MemberRemovedHandler(context)
        NotificationType.GROUP_DELETED -> GroupDeletedHandler(context)
        else -> null
    }

    private fun getReminderHandler(type: NotificationType): NotificationHandler? = when (type) {
        NotificationType.EXPENSE_SCHEDULED_REMINDER -> ScheduledExpenseReminderHandler(context)
        NotificationType.EXPENSE_SCHEDULED_EFFECTIVE -> ScheduledExpenseEffectiveHandler(context)
        NotificationType.EXPENSE_REFUNDABLE_REMINDER -> RefundableExpenseReminderHandler(context)
        else -> null
    }
}
