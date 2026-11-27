package es.pedrazamiguez.splittrip.core.logging.exit

fun interface ProcessExitInfoProvider {
    fun getRecentAbnormalExits(maxResults: Int): List<ProcessExitRecord>
}
