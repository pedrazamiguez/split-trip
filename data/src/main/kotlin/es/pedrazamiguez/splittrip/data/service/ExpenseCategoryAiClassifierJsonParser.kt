package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.domain.model.CategoryPredictionOutcome
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestion
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestionSource
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

internal object ExpenseCategoryAiClassifierJsonParser {

    private const val KEY_CATEGORY = "category"
    private const val KEY_SUBCATEGORY = "subcategory"
    private const val NULL_LITERAL = "null"
    private val REQUIRED_KEYS = listOf(KEY_CATEGORY, KEY_SUBCATEGORY)

    fun buildJsonSchema(groupContext: GroupContext): String {
        val categories = ExpenseCategory.forContext(groupContext)
        val subcategories = categories.flatMap { category ->
            ExpenseSubcategory.forCategory(category, groupContext)
        }
        val properties = JSONObject().apply {
            put(
                KEY_CATEGORY,
                JSONObject().apply {
                    put("type", "string")
                    put("enum", JSONArray(categories.map { it.name }))
                }
            )
            put(
                KEY_SUBCATEGORY,
                JSONObject().apply {
                    put("type", JSONArray(listOf("string", "null")))
                    put("enum", JSONArray(subcategories.map { it.name } + JSONObject.NULL))
                }
            )
        }

        return JSONObject().apply {
            put("type", "object")
            put("properties", properties)
            put("required", JSONArray(REQUIRED_KEYS))
            put("additionalProperties", false)
        }.toString()
    }

    @Suppress("ReturnCount")
    fun parseOutcome(
        rawOutput: String,
        groupContext: GroupContext
    ): CategoryPredictionOutcome {
        val cleanJson = JsonSanitizer.sanitize(rawOutput)
        if (!cleanJson.startsWith("{")) return CategoryPredictionOutcome.NoMatch

        val jsonObject = try {
            JSONObject(cleanJson)
        } catch (e: JSONException) {
            Timber.d(e, "ExpenseCategoryAiClassifier: malformed JSON output")
            return CategoryPredictionOutcome.NoMatch
        }

        val parsedCategory = parseCategory(jsonObject)
        val parsedSubcategory = parseSubcategory(jsonObject)
        val suggestion = resolveSuggestion(
            parsedCategory = parsedCategory,
            parsedSubcategory = parsedSubcategory,
            groupContext = groupContext
        ) ?: return CategoryPredictionOutcome.NoMatch

        return if (suggestion.category == ExpenseCategory.OTHER && suggestion.subcategory == null) {
            CategoryPredictionOutcome.NoMatch
        } else {
            CategoryPredictionOutcome.Matched(suggestion)
        }
    }

    private fun resolveSuggestion(
        parsedCategory: ExpenseCategory?,
        parsedSubcategory: ExpenseSubcategory?,
        groupContext: GroupContext
    ): CategorySuggestion? {
        val validSubcategory = parsedSubcategory?.takeIf {
            it.appliesTo(groupContext) && it.parentCategory.appliesTo(groupContext)
        }
        if (validSubcategory != null) {
            val derivedCategory = validSubcategory.parentCategory
            if (parsedCategory != null && parsedCategory != derivedCategory) {
                Timber.w(
                    "ExpenseCategoryAiClassifier: mismatched category '%s' for '%s' — enforcing '%s'",
                    parsedCategory.name,
                    validSubcategory.name,
                    derivedCategory.name
                )
            }
            return CategorySuggestion(
                category = derivedCategory,
                subcategory = validSubcategory,
                source = CategorySuggestionSource.ON_DEVICE_AI
            )
        }

        if (parsedSubcategory != null) {
            Timber.d(
                "ExpenseCategoryAiClassifier: ignoring out-of-context subcategory '%s' for %s",
                parsedSubcategory.name,
                groupContext
            )
        }
        val validCategory = parsedCategory?.takeIf { it.appliesTo(groupContext) } ?: return null
        return CategorySuggestion(
            category = validCategory,
            subcategory = null,
            source = CategorySuggestionSource.ON_DEVICE_AI
        )
    }

    private fun optNullableString(jsonObject: JSONObject, key: String): String? {
        if (jsonObject.isNull(key)) return null
        val trimmed = jsonObject.optString(key).trim()
        return trimmed.takeIf { it.isNotEmpty() && !it.equals(NULL_LITERAL, ignoreCase = true) }
    }

    private fun parseCategory(jsonObject: JSONObject): ExpenseCategory? {
        val rawCategory = optNullableString(jsonObject, KEY_CATEGORY) ?: return null
        return runCatching { ExpenseCategory.fromString(rawCategory) }.getOrNull()
    }

    private fun parseSubcategory(jsonObject: JSONObject): ExpenseSubcategory? {
        val rawSubcategory = optNullableString(jsonObject, KEY_SUBCATEGORY) ?: return null
        if (rawSubcategory.equals(ExpenseSubcategory.UNSPECIFIED.name, ignoreCase = true)) return null
        return runCatching { ExpenseSubcategory.fromString(rawSubcategory) }.getOrNull()
    }
}
