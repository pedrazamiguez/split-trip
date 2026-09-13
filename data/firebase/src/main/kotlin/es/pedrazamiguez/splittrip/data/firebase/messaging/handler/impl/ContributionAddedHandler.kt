package es.pedrazamiguez.splittrip.data.firebase.messaging.handler.impl

import android.content.Context
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.data.firebase.R
import es.pedrazamiguez.splittrip.data.firebase.messaging.handler.formatNotificationAmount
import es.pedrazamiguez.splittrip.data.firebase.messaging.handler.stableNotificationId
import es.pedrazamiguez.splittrip.domain.constant.NotificationChannelId
import es.pedrazamiguez.splittrip.domain.handler.NotificationHandler
import es.pedrazamiguez.splittrip.domain.model.NotificationContent

class ContributionAddedHandler(private val context: Context, private val localeProvider: LocaleProvider) :
    NotificationHandler {
    override fun handle(data: Map<String, String>): NotificationContent {
        val fallbackName = context.getString(R.string.notification_fallback_actor_name)
        val rawActorName = data["actorName"]
        val rawMemberName = data["memberName"]
        val isImpersonation = rawActorName != null && rawMemberName != null && rawActorName != rawMemberName

        val actorName = rawActorName ?: fallbackName
        val memberName = rawMemberName ?: fallbackName
        val amount = formatNotificationAmount(data, localeProvider)
        val groupName = data["groupName"] ?: ""
        val groupId = data["groupId"]

        val body = if (isImpersonation) {
            context.getString(
                R.string.notification_contribution_added_body_on_behalf,
                actorName,
                memberName
            )
        } else if (amount.isNotBlank()) {
            context.getString(
                R.string.notification_contribution_added_body,
                memberName,
                amount
            )
        } else {
            context.getString(
                R.string.notification_contribution_added_body_brief,
                memberName
            )
        }

        return NotificationContent(
            title = groupName.ifBlank {
                context.getString(R.string.notification_contribution_added_title)
            },
            body = body,
            deepLink = data["deepLink"],
            channelId = NotificationChannelId.FINANCIAL,
            groupId = groupId,
            notificationId = stableNotificationId("CONTRIBUTION_ADDED", groupId, data["entityId"])
        )
    }
}
