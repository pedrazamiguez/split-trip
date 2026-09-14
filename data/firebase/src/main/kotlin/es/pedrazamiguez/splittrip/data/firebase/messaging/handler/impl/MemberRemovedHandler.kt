package es.pedrazamiguez.splittrip.data.firebase.messaging.handler.impl

import android.content.Context
import es.pedrazamiguez.splittrip.data.firebase.R
import es.pedrazamiguez.splittrip.data.firebase.messaging.handler.stableNotificationId
import es.pedrazamiguez.splittrip.domain.constant.NotificationChannelId
import es.pedrazamiguez.splittrip.domain.handler.NotificationHandler
import es.pedrazamiguez.splittrip.domain.model.NotificationContent

class MemberRemovedHandler(private val context: Context) : NotificationHandler {
    override fun handle(data: Map<String, String>): NotificationContent {
        val fallbackName = context.getString(R.string.notification_fallback_actor_name)
        val rawActorName = data["actorName"]
        val rawMemberName = data["memberName"]
        val isAdminAction = rawActorName != null && rawMemberName != null && rawActorName != rawMemberName

        val actorName = rawActorName ?: fallbackName
        val memberName = rawMemberName ?: fallbackName
        val groupName = data["groupName"] ?: ""
        val groupId = data["groupId"]

        val body = if (isAdminAction) {
            context.getString(
                R.string.notification_member_removed_by_admin_body,
                actorName,
                memberName
            )
        } else {
            context.getString(
                R.string.notification_member_removed_body,
                memberName
            )
        }

        return NotificationContent(
            title = groupName.ifBlank {
                context.getString(R.string.notification_member_removed_title)
            },
            body = body,
            deepLink = data["deepLink"],
            channelId = NotificationChannelId.MEMBERSHIP,
            groupId = groupId,
            notificationId = stableNotificationId("MEMBER_REMOVED", groupId, data["entityId"])
        )
    }
}
