package es.pedrazamiguez.splittrip.data.firebase.messaging.handler.impl

import android.content.Context
import es.pedrazamiguez.splittrip.data.firebase.R
import es.pedrazamiguez.splittrip.data.firebase.messaging.handler.stableNotificationId
import es.pedrazamiguez.splittrip.domain.constant.NotificationChannelId
import es.pedrazamiguez.splittrip.domain.handler.NotificationHandler
import es.pedrazamiguez.splittrip.domain.model.NotificationContent

class GroupDeletedHandler(private val context: Context) : NotificationHandler {
    override fun handle(data: Map<String, String>): NotificationContent {
        val fallbackName = context.getString(R.string.notification_fallback_actor_name)
        val actorName = data["actorName"] ?: data["memberName"] ?: fallbackName
        val groupName = data["groupName"] ?: ""
        val groupId = data["groupId"]

        val title = groupName.ifBlank {
            context.getString(R.string.notification_group_deleted_title)
        }
        val body = if (groupName.isNotBlank()) {
            context.getString(R.string.notification_group_deleted_body, actorName, groupName)
        } else {
            context.getString(R.string.notification_group_deleted_body_brief, actorName)
        }

        return NotificationContent(
            title = title,
            body = body,
            deepLink = data["deepLink"],
            channelId = NotificationChannelId.MEMBERSHIP,
            groupId = groupId,
            notificationId = stableNotificationId("GROUP_DELETED", groupId, data["entityId"])
        )
    }
}
