package es.pedrazamiguez.splittrip.data.firebase.config

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import es.pedrazamiguez.splittrip.domain.enums.AppLanguage
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.model.CategoryClassifierRuleConfig
import es.pedrazamiguez.splittrip.domain.model.CategoryKeywordRule
import es.pedrazamiguez.splittrip.domain.model.CategoryRuleMergeMode
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestion
import es.pedrazamiguez.splittrip.domain.service.category.CategoryTextNormalizer
import timber.log.Timber

internal object CategoryClassifierRulesParser {

    private const val FIELD_MERGE_MODE = "merge_mode"
    private const val FIELD_COMMON = "common"
    private const val FIELD_LANGUAGES = "languages"
    private const val FIELD_DISABLED_TERMS = "disabled_terms"
    private const val FIELD_CATEGORY = "category"
    private const val FIELD_SUBCATEGORY = "subcategory"
    private const val FIELD_PHRASES = "phrases"
    private const val FIELD_KEYWORDS = "keywords"
    private const val MERGE_MODE_REPLACE = "replace"
    private const val PACK_COMMON = "common"
    private const val WILDCARD_PACK = "*"

    fun parse(rawJson: String): CategoryClassifierRuleConfig {
        if (rawJson.isBlank()) return CategoryClassifierRuleConfig.EMPTY

        val root = try {
            val element = JsonParser.parseString(rawJson)
            if (!element.isJsonObject) {
                Timber.w("Remote Config category_classifier_rules_json root is not a JSON object.")
                return CategoryClassifierRuleConfig.EMPTY
            }
            element.asJsonObject
        } catch (e: JsonParseException) {
            Timber.w(e, "Failed to parse Remote Config category_classifier_rules_json.")
            return CategoryClassifierRuleConfig.EMPTY
        }

        val mergeMode = parseMergeMode(root)
        val commonRules = parseRuleArray(root.get(FIELD_COMMON))
        val languageRules = parseLanguageRules(root.get(FIELD_LANGUAGES))
        val disabledTerms = parseDisabledTerms(root.get(FIELD_DISABLED_TERMS))

        return CategoryClassifierRuleConfig(
            mergeMode = mergeMode,
            commonRules = commonRules,
            languageRules = languageRules,
            disabledTerms = disabledTerms
        )
    }

    private fun parseMergeMode(root: JsonObject): CategoryRuleMergeMode {
        val rawMode = root.getStringOrNull(FIELD_MERGE_MODE)?.trim() ?: return CategoryRuleMergeMode.MERGE
        return if (rawMode.equals(MERGE_MODE_REPLACE, ignoreCase = true)) {
            CategoryRuleMergeMode.REPLACE
        } else {
            CategoryRuleMergeMode.MERGE
        }
    }

    private fun parseRuleArray(element: JsonElement?): List<CategoryKeywordRule> {
        if (element == null || !element.isJsonArray) return emptyList()
        return element.asJsonArray.mapNotNull(::parseRule)
    }

    private fun parseLanguageRules(element: JsonElement?): Map<String, List<CategoryKeywordRule>> {
        if (element == null || !element.isJsonObject) return emptyMap()
        val result = LinkedHashMap<String, MutableList<CategoryKeywordRule>>()
        for ((rawKey, value) in element.asJsonObject.entrySet()) {
            if (rawKey.isBlank() || !value.isJsonArray) continue
            val langCode = AppLanguage.resolveBaseLanguageCode(rawKey)
            val parsedRules = parseRuleArray(value)
            if (parsedRules.isNotEmpty()) {
                result.getOrPut(langCode) { mutableListOf() }.addAll(parsedRules)
            }
        }
        return result
    }

    private fun parseDisabledTerms(element: JsonElement?): Map<String, Set<String>> {
        if (element == null || !element.isJsonObject) return emptyMap()
        val result = LinkedHashMap<String, MutableSet<String>>()
        for ((rawKey, value) in element.asJsonObject.entrySet()) {
            val trimmedKey = rawKey.trim()
            if (trimmedKey.isEmpty() || !value.isJsonArray) continue
            val normalizedKey = when {
                trimmedKey == WILDCARD_PACK -> WILDCARD_PACK
                trimmedKey.equals(PACK_COMMON, ignoreCase = true) -> PACK_COMMON
                else -> AppLanguage.resolveBaseLanguageCode(trimmedKey)
            }
            val terms = extractNormalizedTerms(value.asJsonArray)
            if (terms.isNotEmpty()) {
                result.getOrPut(normalizedKey) { LinkedHashSet() }.addAll(terms)
            }
        }
        return result
    }

    private fun parseRule(element: JsonElement): CategoryKeywordRule? {
        val obj = element.takeIf { it.isJsonObject }?.asJsonObject ?: return null
        val category = obj.getStringOrNull(FIELD_CATEGORY)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { raw -> runCatching { ExpenseCategory.fromString(raw) }.getOrNull() }
            ?: return null

        val subcategoryResult = parseSubcategory(obj, category)
        val normalizedTerms = normalizeTerms(
            rawPhrases = obj.get(FIELD_PHRASES),
            rawKeywords = obj.get(FIELD_KEYWORDS)
        )
        val hasTerms = normalizedTerms.phrases.isNotEmpty() || normalizedTerms.keywords.isNotEmpty()
        return if (subcategoryResult.isValid && hasTerms) {
            CategoryKeywordRule(
                category = category,
                subcategory = subcategoryResult.subcategory,
                phrases = normalizedTerms.phrases,
                keywords = normalizedTerms.keywords
            )
        } else {
            null
        }
    }

    private fun parseSubcategory(obj: JsonObject, category: ExpenseCategory): SubcategoryParseResult {
        val subElement = obj.get(FIELD_SUBCATEGORY)
        return when {
            subElement == null || subElement.isJsonNull -> SubcategoryParseResult(isValid = true, subcategory = null)
            !subElement.isJsonPrimitive || !subElement.asJsonPrimitive.isString ->
                SubcategoryParseResult(isValid = false, subcategory = null)
            else -> validateSubcategoryName(rawSub = subElement.asString.trim(), category = category)
        }
    }

    private fun validateSubcategoryName(rawSub: String, category: ExpenseCategory): SubcategoryParseResult {
        if (rawSub.isEmpty()) return SubcategoryParseResult(isValid = true, subcategory = null)
        val parsed = runCatching { ExpenseSubcategory.fromString(rawSub) }.getOrNull()
        val isValid = parsed != null &&
            CategorySuggestion.sanitized(category = category, subcategory = parsed).subcategory == parsed
        return SubcategoryParseResult(
            isValid = isValid,
            subcategory = if (isValid) parsed else null
        )
    }

    private fun normalizeTerms(rawPhrases: JsonElement?, rawKeywords: JsonElement?): NormalizedRuleTerms {
        val phrases = LinkedHashSet<String>()
        val keywords = LinkedHashSet<String>()

        if (rawPhrases != null && rawPhrases.isJsonArray) {
            for (term in extractNormalizedTerms(rawPhrases.asJsonArray)) {
                phrases.add(term)
            }
        }
        if (rawKeywords != null && rawKeywords.isJsonArray) {
            for (term in extractNormalizedTerms(rawKeywords.asJsonArray)) {
                if (' ' in term) {
                    phrases.add(term)
                } else {
                    keywords.add(term)
                }
            }
        }

        return NormalizedRuleTerms(
            phrases = phrases.toList(),
            keywords = keywords.toList()
        )
    }

    private fun extractNormalizedTerms(array: JsonArray): List<String> = array.mapNotNull { element ->
        if (!element.isJsonPrimitive || !element.asJsonPrimitive.isString) return@mapNotNull null
        CategoryTextNormalizer.normalize(element.asString)
    }

    private fun JsonObject.getStringOrNull(key: String): String? {
        if (!has(key)) return null
        val elem = get(key)
        return if (elem.isJsonPrimitive && elem.asJsonPrimitive.isString) elem.asString else null
    }

    private data class NormalizedRuleTerms(
        val phrases: List<String>,
        val keywords: List<String>
    )

    private data class SubcategoryParseResult(
        val isValid: Boolean,
        val subcategory: ExpenseSubcategory?
    )
}
