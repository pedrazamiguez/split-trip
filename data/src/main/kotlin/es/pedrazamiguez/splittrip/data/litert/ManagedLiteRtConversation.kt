package es.pedrazamiguez.splittrip.data.litert

internal interface ManagedLiteRtConversation : AutoCloseable {
    fun sendMessageAsync(
        prompt: String,
        conversationSpec: LiteRtRuntimeConfig.ConversationSpec,
        callback: LiteRtConversationCallback
    )

    fun cancelProcess()
}

internal interface LiteRtConversationCallback {
    fun onTextChunk(text: String)
    fun onDone()
    fun onError(throwable: Throwable)
}
