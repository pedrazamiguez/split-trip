package es.pedrazamiguez.splittrip.core.logging.exit

data class ProcessExitRecord(
    val reason: AbnormalExitReason,
    val timestampMillis: Long,
    val pid: Int,
    val status: Int,
    val importance: Int,
    val pssKb: Long,
    val rssKb: Long,
    val processName: String,
    val description: String?
)
