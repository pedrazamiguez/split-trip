package es.pedrazamiguez.splittrip.data.litert

internal interface ManagedLiteRtEngine : AutoCloseable {
    fun isInitialized(): Boolean

    fun createConversation(
        conversationSpec: LiteRtRuntimeConfig.ConversationSpec
    ): ManagedLiteRtConversation
}

internal fun interface LiteRtNativeEngineFactory {
    fun createAndInitialize(modelPath: String): ManagedLiteRtEngine
}
