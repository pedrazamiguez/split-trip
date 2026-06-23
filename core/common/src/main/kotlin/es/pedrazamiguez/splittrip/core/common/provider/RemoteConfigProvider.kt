package es.pedrazamiguez.splittrip.core.common.provider

interface RemoteConfigProvider {
    fun getString(key: String): String
    fun getLong(key: String): Long
    fun getBoolean(key: String): Boolean
    fun fetchAndActivate(onComplete: (Boolean) -> Unit)
}
