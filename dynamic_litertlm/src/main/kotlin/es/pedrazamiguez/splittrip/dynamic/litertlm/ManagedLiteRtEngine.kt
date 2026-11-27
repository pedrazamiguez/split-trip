package es.pedrazamiguez.splittrip.dynamic.litertlm

import es.pedrazamiguez.splittrip.data.litert.LiteRtRuntimeConfig

internal interface ManagedLiteRtEngine : AutoCloseable {
    fun isInitialized(): Boolean

    fun createConversation(
        conversationSpec: LiteRtRuntimeConfig.ConversationSpec
    ): ManagedLiteRtConversation
}

internal fun interface LiteRtNativeEngineFactory {
    fun createAndInitialize(modelPath: String): ManagedLiteRtEngine
}
