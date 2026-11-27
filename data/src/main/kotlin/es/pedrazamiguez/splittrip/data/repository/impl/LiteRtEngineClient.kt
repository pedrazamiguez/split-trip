package es.pedrazamiguez.splittrip.data.repository.impl

internal interface LiteRtEngineClient : AutoCloseable {
    suspend fun generate(
        modelPath: String,
        prompt: String,
        systemInstruction: String? = null
    ): String

    fun invalidate(modelPath: String? = null)
}
