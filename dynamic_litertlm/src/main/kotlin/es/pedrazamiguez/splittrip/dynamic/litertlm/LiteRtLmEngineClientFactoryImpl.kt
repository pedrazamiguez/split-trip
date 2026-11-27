package es.pedrazamiguez.splittrip.dynamic.litertlm

import android.content.Context
import es.pedrazamiguez.splittrip.data.litert.LiteRtEngineClient
import es.pedrazamiguez.splittrip.data.litert.LiteRtEngineClientFactory

class LiteRtLmEngineClientFactoryImpl : LiteRtEngineClientFactory {
    override fun create(context: Context): LiteRtEngineClient =
        LiteRtLmEngineClientImpl(context = context.applicationContext)
}
