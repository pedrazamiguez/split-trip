package es.pedrazamiguez.splittrip.core.logging.tree

import es.pedrazamiguez.splittrip.core.logging.LogContext
import timber.log.Timber

class DevelopmentLogcatTree(
    private val logContext: LogContext
) : Timber.DebugTree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val threadName = Thread.currentThread().name
        val sessionId = logContext.sessionId
        val formattedMessage = "[$threadName] [Session:$sessionId] $message"
        super.log(priority, tag, formattedMessage, t)
    }
}
