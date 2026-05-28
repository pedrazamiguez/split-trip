package es.pedrazamiguez.splittrip.domain.repository

import es.pedrazamiguez.splittrip.domain.enums.AiEngineType

interface AiInferenceRepository {
    suspend fun generateContent(prompt: String): Result<String>
    suspend fun generateStructuredOutput(prompt: String, jsonSchema: String): Result<String>
    fun getEngineType(): AiEngineType
}
