package es.pedrazamiguez.splittrip.data.litert

import android.content.Context

interface LiteRtEngineClientFactory {
    fun create(context: Context): LiteRtEngineClient
}
