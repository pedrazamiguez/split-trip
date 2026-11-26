package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestion
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestionSource
import es.pedrazamiguez.splittrip.domain.repository.AiInferenceRepository
import es.pedrazamiguez.splittrip.domain.service.AiModelResolverService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCategoryAiClassifierService
import es.pedrazamiguez.splittrip.domain.service.LocalAiModelPackService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

@Suppress("LongParameterList")
internal class ExpenseCategoryAiClassifierServiceImpl(
    private val aiCoreCapabilityProvider: AICoreCapabilityProvider,
    private val aiCoreInferenceRepository: Lazy<AiInferenceRepository>,
    private val liteRtInferenceRepository: Lazy<AiInferenceRepository>,
    private val aiModelResolver: AiModelResolverService,
    private val localAiModelPackService: LocalAiModelPackService,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val inferenceTimeoutMs: Long = DEFAULT_INFERENCE_TIMEOUT_MS
) : ExpenseCategoryAiClassifierService {

    @Suppress("ReturnCount")
    override suspend fun classify(
        title: String,
        groupContext: GroupContext
    ): CategorySuggestion? {
        val sanitizedTitle = sanitizeTitleForPrompt(title)
        if (sanitizedTitle.isBlank()) return null

        val resolvedEngine = aiModelResolver.getActiveModel().first()
        val executionEngine = resolveExecutionEngine(resolvedEngine) ?: return null

        return withContext(defaultDispatcher) {
            val prompt = buildPrompt(sanitizedTitle, groupContext)
            executeWithFallback(executionEngine, prompt)
        }
    }

    private suspend fun executeWithFallback(
        executionEngine: AiEngineType,
        prompt: String
    ): CategorySuggestion? {
        var inferenceResult = runTimedInference(executionEngine, prompt) ?: return null

        if (inferenceResult.isFailure) {
            val error = inferenceResult.exceptionOrNull()
            if (error is CancellationException) throw error
            if (executionEngine == AiEngineType.LITE_RT_LM && aiCoreCapabilityProvider.isSupported()) {
                Timber.w(
                    error,
                    "ExpenseCategoryAiClassifier: LITE_RT_LM failed — falling back to AI_CORE_GEMMA_4"
                )
                inferenceResult = runTimedInference(AiEngineType.AI_CORE_GEMMA_4, prompt) ?: return null
            }
        }

        return inferenceResult.fold(
            onSuccess = { rawOutput -> parseSuggestion(rawOutput) },
            onFailure = { error ->
                if (error is CancellationException) throw error
                Timber.w(error, "ExpenseCategoryAiClassifier: inference failed")
                null
            }
        )
    }

    private suspend fun runTimedInference(
        engine: AiEngineType,
        prompt: String
    ): Result<String>? {
        val result = withTimeoutOrNull(inferenceTimeoutMs) {
            runInference(engine, prompt)
        }
        if (result == null) {
            Timber.d(
                "ExpenseCategoryAiClassifier: %s inference timed out after %dms",
                engine,
                inferenceTimeoutMs
            )
        }
        return result
    }

    private fun resolveExecutionEngine(resolvedEngine: AiEngineType): AiEngineType? {
        if (isEngineSupported(resolvedEngine)) return resolvedEngine
        if (resolvedEngine == AiEngineType.LITE_RT_LM && aiCoreCapabilityProvider.isSupported()) {
            Timber.w("ExpenseCategoryAiClassifier: LITE_RT_LM pack not installed — falling back to AI_CORE_GEMMA_4")
            return AiEngineType.AI_CORE_GEMMA_4
        }
        return null
    }

    private fun isEngineSupported(engine: AiEngineType): Boolean = when (engine) {
        AiEngineType.AI_CORE_GEMMA_4 -> aiCoreCapabilityProvider.isSupported()
        AiEngineType.LITE_RT_LM -> localAiModelPackService.isPackInstalled()
    }

    private suspend fun runInference(engine: AiEngineType, prompt: String): Result<String> = when (engine) {
        AiEngineType.AI_CORE_GEMMA_4 ->
            aiCoreInferenceRepository.value.generateContent(prompt)
        AiEngineType.LITE_RT_LM ->
            liteRtInferenceRepository.value.generateStructuredOutput(prompt, CATEGORY_JSON_SCHEMA)
    }

    private fun sanitizeTitleForPrompt(title: String): String {
        val cleaned = CONTROL_AND_QUOTE_REGEX.replace(title, " ")
        return WHITESPACE_REGEX.replace(cleaned, " ")
            .trim()
            .take(MAX_PROMPT_TITLE_CHARS)
            .trim()
    }

    private fun buildPrompt(
        sanitizedTitle: String,
        groupContext: GroupContext
    ): String {
        val taxonomyLines = ExpenseCategory.forContext(groupContext).joinToString("\n") { category ->
            val subcategories = ExpenseSubcategory.forCategory(category, groupContext)
                .joinToString(",") { it.name }
            "${category.name}: $subcategories"
        }

        return """
            Classify the expense title into one category and optional subcategory.
            Return ONLY JSON: {"category":"...","subcategory":"..." or null}
            If uncertain, return {"category":"OTHER","subcategory":null}.
            Allowed taxonomy:
            $taxonomyLines
            Title: $sanitizedTitle
        """.trimIndent()
    }

    private fun parseSuggestion(rawOutput: String): CategorySuggestion? {
        val cleanJson = JsonSanitizer.sanitize(rawOutput)
        if (!cleanJson.startsWith("{")) return null

        val jsonObject = try {
            JSONObject(cleanJson)
        } catch (e: JSONException) {
            Timber.d(e, "ExpenseCategoryAiClassifier: malformed JSON output")
            return null
        }

        val category = parseCategory(jsonObject) ?: return null
        val subcategory = parseSubcategory(jsonObject)
        val suggestion = CategorySuggestion.sanitized(
            category = category,
            subcategory = subcategory,
            source = CategorySuggestionSource.ON_DEVICE_AI
        )

        return if (suggestion.category == ExpenseCategory.OTHER && suggestion.subcategory == null) {
            null
        } else {
            suggestion
        }
    }

    private fun parseCategory(jsonObject: JSONObject): ExpenseCategory? {
        val rawCategory = jsonObject.optString(KEY_CATEGORY).trim()
        if (rawCategory.isEmpty() || rawCategory.equals(NULL_LITERAL, ignoreCase = true)) {
            return null
        }
        return runCatching { ExpenseCategory.fromString(rawCategory) }.getOrNull()
    }

    private fun parseSubcategory(jsonObject: JSONObject): ExpenseSubcategory? {
        val rawSubcategory = jsonObject.optString(KEY_SUBCATEGORY).trim()
        val isMissing = rawSubcategory.isEmpty() ||
            rawSubcategory.equals(NULL_LITERAL, ignoreCase = true) ||
            rawSubcategory.equals(ExpenseSubcategory.UNSPECIFIED.name, ignoreCase = true)
        if (isMissing) return null

        return runCatching { ExpenseSubcategory.fromString(rawSubcategory) }.getOrNull()
    }

    companion object {
        const val DEFAULT_INFERENCE_TIMEOUT_MS = 5_000L
        private const val MAX_PROMPT_TITLE_CHARS = 120
        private const val KEY_CATEGORY = "category"
        private const val KEY_SUBCATEGORY = "subcategory"
        private const val NULL_LITERAL = "null"

        private val CONTROL_AND_QUOTE_REGEX = Regex("[\\r\\n\"`]+")
        private val WHITESPACE_REGEX = Regex("\\s+")

        private val CATEGORY_JSON_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "category": { "type": "string" },
                "subcategory": { "type": "string" }
              },
              "required": ["category"]
            }
        """.trimIndent()
    }
}
