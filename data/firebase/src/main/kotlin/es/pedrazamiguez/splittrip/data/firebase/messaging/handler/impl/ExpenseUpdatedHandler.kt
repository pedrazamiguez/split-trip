package es.pedrazamiguez.splittrip.data.firebase.messaging.handler.impl

import android.content.Context
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.data.firebase.R
import es.pedrazamiguez.splittrip.data.firebase.messaging.handler.formatNotificationAmount
import es.pedrazamiguez.splittrip.data.firebase.messaging.handler.stableNotificationId
import es.pedrazamiguez.splittrip.domain.constant.NotificationChannelId
import es.pedrazamiguez.splittrip.domain.handler.NotificationHandler
import es.pedrazamiguez.splittrip.domain.model.NotificationContent

class ExpenseUpdatedHandler(private val context: Context, private val localeProvider: LocaleProvider) :
    NotificationHandler {
    override fun handle(data: Map<String, String>): NotificationContent {
        val fallbackName = context.getString(R.string.notification_fallback_actor_name)
        val actorName = data["actorName"] ?: data["memberName"] ?: fallbackName
        val amount = formatNotificationAmount(data, localeProvider)
        val groupName = data["groupName"] ?: ""
        val groupId = data["groupId"]
        val body = if (amount.isNotBlank()) {
            context.getString(
                R.string.notification_expense_updated_body,
                actorName,
                amount
            )
        } else {
            context.getString(
                R.string.notification_expense_updated_body_brief,
                actorName
            )
        }
        return NotificationContent(
            title = groupName.ifBlank {
                context.getString(R.string.notification_expense_updated_title)
            },
            body = body,
            deepLink = data["deepLink"],
            channelId = NotificationChannelId.EXPENSES,
            groupId = groupId,
            notificationId = stableNotificationId("EXPENSE_UPDATED", groupId, data["entityId"])
        )
    }
}
