package es.pedrazamiguez.splittrip.data.litert

interface LiteRtEngineClient : AutoCloseable {
    suspend fun generate(
        modelPath: String,
        prompt: String,
        systemInstruction: String? = null
    ): String

    fun invalidate(modelPath: String? = null)
}
