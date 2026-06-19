package es.pedrazamiguez.splittrip.data.firebase.messaging.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import es.pedrazamiguez.splittrip.core.designsystem.provider.IntentProvider
import es.pedrazamiguez.splittrip.data.firebase.R
import es.pedrazamiguez.splittrip.data.firebase.messaging.channel.NotificationChannelInitializer
import es.pedrazamiguez.splittrip.data.firebase.messaging.handler.factory.NotificationHandlerFactory
import es.pedrazamiguez.splittrip.data.firebase.messaging.handler.stableNotificationId
import es.pedrazamiguez.splittrip.domain.enums.NotificationType
import es.pedrazamiguez.splittrip.domain.model.NotificationContent
import es.pedrazamiguez.splittrip.domain.repository.DeviceRepository
import es.pedrazamiguez.splittrip.domain.repository.NotificationPreferencesRepository
import es.pedrazamiguez.splittrip.domain.repository.NotificationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class SplitTripMessagingService :
    FirebaseMessagingService(),
    KoinComponent {

    private val intentProvider: IntentProvider by inject()
    private val notificationHandlerFactory: NotificationHandlerFactory by inject()
    private val notificationRepository: NotificationRepository by inject()
    private val notificationPreferencesRepository: NotificationPreferencesRepository by inject()
    private val deviceRepository: DeviceRepository by inject()

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        private const val GROUP_SUMMARY_TYPE = "GROUP_SUMMARY"
        private const val PREFERENCES_TIMEOUT_MS = 2000L
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.i("FCM onNewToken called — token=%s…", token.take(10))
        scope.launch {
            try {
                notificationRepository.registerDeviceTokenWithRetry(token)
                Timber.i("FCM token registered successfully via onNewToken")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Error registering device token via onNewToken")
            }
        }
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
        Timber.d("onDeletedMessages called — re-registering device token")
        scope.launch {
            try {
                val token = deviceRepository.getDeviceToken().getOrThrow()
                notificationRepository.registerDeviceTokenWithRetry(token)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Error re-registering device token after onDeletedMessages")
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.d(
            "FCM onMessageReceived — from=%s, dataKeys=%s, hasNotification=%b",
            remoteMessage.from,
            remoteMessage.data.keys.toList(),
            remoteMessage.notification != null
        )
        val notificationType = NotificationType.fromString(remoteMessage.data["type"])
        Timber.d("FCM notification type resolved: %s", notificationType)
        val handler = notificationHandlerFactory.getHandler(notificationType)
        val content = handler.handle(remoteMessage.data)
        Timber.d(
            "FCM handler output — title=%s, body=%s, channelId=%s, notificationId=%d, groupId=%s",
            content.title,
            content.body,
            content.channelId,
            content.notificationId,
            content.groupId
        )

        // IMPORTANT: Notification display MUST happen synchronously within
        // onMessageReceived(). Using scope.launch (async) causes the notification
        // to be silently dropped — the FirebaseMessagingService is destroyed
        // immediately after onMessageReceived() returns, which triggers
        // onDestroy() → job.cancel() → coroutine cancelled before notify().
        // Since onMessageReceived() runs on a worker thread (not the main thread),
        // runBlocking is safe here.
        try {
            val isEnabled = runBlocking {
                val category = notificationType.toCategory()
                if (category != null) {
                    val prefs = withTimeoutOrNull(PREFERENCES_TIMEOUT_MS) {
                        notificationPreferencesRepository.getPreferencesFlow().first()
                    }
                    prefs?.isCategoryEnabled(category) ?: true
                } else {
                    true
                }
            }

            if (isEnabled) {
                Timber.d(
                    "Showing notification: type=%s, channelId=%s, id=%d",
                    notificationType,
                    content.channelId,
                    content.notificationId
                )
                showNotification(content)
            } else {
                Timber.d("Notification of type %s suppressed by user preferences", notificationType)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error reading notification preferences, showing notification by default")
            showNotification(content)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun showNotification(content: NotificationContent) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        ensureNotificationChannelsExist()

        val contentIntent = if (!content.deepLink.isNullOrBlank()) {
            createDeepLinkPendingIntent(content.deepLink!!)
        } else {
            createContentPendingIntent()
        }

        val builder = NotificationCompat.Builder(this, content.channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // Notification grouping by group
        if (!content.groupId.isNullOrBlank()) {
            builder.setGroup(content.groupId)
        }

        notificationManager.notify(content.notificationId, builder.build())
        Timber.i(
            "FCM notification POSTED — id=%d, channel=%s, title=%s",
            content.notificationId,
            content.channelId,
            content.title
        )

        // Post a summary notification for the group only when there are multiple
        if (!content.groupId.isNullOrBlank()) {
            postGroupSummaryIfNeeded(notificationManager, content)
        }
    }

    private fun postGroupSummaryIfNeeded(notificationManager: NotificationManager, content: NotificationContent) {
        val groupCount = countActiveGroupNotifications(
            notificationManager.activeNotifications,
            content.groupId!!
        )

        // Only show a summary when there are at least 2 notifications in the group
        if (groupCount < 2) return

        val summaryNotification = NotificationCompat.Builder(this, content.channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(content.title)
            .setContentText(getString(R.string.notification_group_summary, groupCount))
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setSummaryText(content.title)
            )
            .setGroup(content.groupId)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setContentIntent(createContentPendingIntent())
            .build()

        val summaryId = stableNotificationId(GROUP_SUMMARY_TYPE, content.groupId, null)
        notificationManager.notify(summaryId, summaryNotification)
    }

    /**
     * Creates a [PendingIntent] that launches the main activity.
     *
     * The [Intent] is constructed with the explicit [Intent(Context, Class)] constructor
     * so that static analysis (CodeQL) can verify the target component within this
     * compilation unit.
     */
    private fun createContentPendingIntent(): PendingIntent {
        val explicitIntent = Intent(this, Class.forName(intentProvider.targetActivityClassName))
        explicitIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        return PendingIntent.getActivity(
            this,
            0,
            explicitIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * Creates a [PendingIntent] that launches the main activity with a deep-link URI.
     *
     * The [Intent] is constructed with the explicit [Intent(Context, Class)] constructor
     * so that static analysis (CodeQL) can verify the target component within this
     * compilation unit.
     */
    private fun createDeepLinkPendingIntent(deepLink: String): PendingIntent {
        val explicitIntent = Intent(this, Class.forName(intentProvider.targetActivityClassName))
        explicitIntent.action = Intent.ACTION_VIEW
        explicitIntent.data = Uri.parse(deepLink)
        explicitIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        return PendingIntent.getActivity(
            this,
            deepLink.hashCode(),
            explicitIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * Counts the active non-summary notifications that belong to the given group.
     * Returns 1 if the active notifications array is unavailable.
     */
    private fun countActiveGroupNotifications(
        activeNotifications: Array<StatusBarNotification>?,
        groupId: String
    ): Int {
        if (activeNotifications == null) return 1
        return activeNotifications.count { sbn ->
            sbn.notification?.group == groupId &&
                (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0
        }
    }

    private fun ensureNotificationChannelsExist() {
        // Delegates to the shared initializer (idempotent — safe to call multiple times).
        // Primary creation happens in App.onCreate(); this is a safety net in case the
        // service starts before Application.onCreate() completes.
        NotificationChannelInitializer.createChannels(this)
    }
}
