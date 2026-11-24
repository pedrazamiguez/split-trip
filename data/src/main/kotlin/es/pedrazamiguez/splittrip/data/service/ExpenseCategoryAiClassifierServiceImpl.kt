package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestion
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestionSource
import es.pedrazamiguez.splittrip.domain.repository.AiInferenceRepository
import es.pedrazamiguez.splittrip.domain.service.ExpenseCategoryAiClassifierService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

internal class ExpenseCategoryAiClassifierServiceImpl(
    private val aiCoreCapabilityProvider: AICoreCapabilityProvider,
    private val aiCoreInferenceRepository: Lazy<AiInferenceRepository>,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val inferenceTimeoutMs: Long = DEFAULT_INFERENCE_TIMEOUT_MS
) : ExpenseCategoryAiClassifierService {

    override suspend fun classify(
        title: String,
        groupContext: GroupContext
    ): CategorySuggestion? {
        if (!aiCoreCapabilityProvider.isSupported()) return null
        val sanitizedTitle = sanitizeTitleForPrompt(title)
        if (sanitizedTitle.isBlank()) return null

        return withContext(defaultDispatcher) {
            val prompt = buildPrompt(sanitizedTitle, groupContext)
            val inferenceResult = withTimeoutOrNull(inferenceTimeoutMs) {
                aiCoreInferenceRepository.value.generateContent(prompt)
            } ?: run {
                Timber.d("ExpenseCategoryAiClassifier: inference timed out after %dms", inferenceTimeoutMs)
                return@withContext null
            }

            inferenceResult.fold(
                onSuccess = { rawOutput -> parseSuggestion(rawOutput) },
                onFailure = { error ->
                    if (error is CancellationException) throw error
                    Timber.w(error, "ExpenseCategoryAiClassifier: inference failed")
                    null
                }
            )
        }
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
    }
}
