package es.pedrazamiguez.splittrip.data.firebase.config

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import es.pedrazamiguez.splittrip.data.firebase.repository.FirebaseAppConfigRepository
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.domain.model.CategoryKeywordRule
import es.pedrazamiguez.splittrip.domain.model.CategoryRuleMergeMode
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestion
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.impl.ExpenseCategoryClassifierServiceImpl
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.nio.charset.StandardCharsets
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.w3c.dom.Element

class RemoteConfigTemplateConsistencyTest {

    private lateinit var xmlDefaults: Map<String, String>
    private lateinit var templateParameters: Map<String, ParameterDefinition>
    private lateinit var groupDescriptions: Map<String, String>
    private lateinit var templateConditions: List<ConditionDefinition>
    private var hasMalformedConditions: Boolean = false

    data class ConditionDefinition(
        val name: String?,
        val expression: String?,
        val tagColor: String?
    )

    data class ParameterDefinition(
        val description: String?,
        val valueType: String?,
        val defaultValue: String?,
        val conditionalValues: Map<String, String?> = emptyMap(),
        val hasMalformedConditionalValues: Boolean = false
    )

    private data class ParsedTemplate(
        val parameters: Map<String, ParameterDefinition>,
        val groupDescriptions: Map<String, String>,
        val conditions: List<ConditionDefinition>,
        val hasMalformedConditions: Boolean
    )

    @BeforeEach
    fun setUp() {
        val rootDir = resolveProjectRoot()
        val xmlFile = File(rootDir, "data/firebase/src/main/res/xml/remote_config_defaults.xml")
        val jsonFile = File(rootDir, "firebase/remoteconfig.template.json")

        assertTrue(xmlFile.exists(), "XML defaults file not found: ${xmlFile.absolutePath}")
        assertTrue(jsonFile.exists(), "Template JSON file not found: ${jsonFile.absolutePath}")

        xmlDefaults = parseXmlDefaults(xmlFile)
        val parsed = parseTemplate(jsonFile)
        templateParameters = parsed.parameters
        groupDescriptions = parsed.groupDescriptions
        templateConditions = parsed.conditions
        hasMalformedConditions = parsed.hasMalformedConditions
    }

    @Test
    fun `all XML keys exist in remote config template JSON`() {
        val missingKeys = xmlDefaults.keys - templateParameters.keys
        assertTrue(
            missingKeys.isEmpty(),
            "The following keys exist in XML defaults but are missing in remote config template JSON: $missingKeys"
        )
    }

    @Test
    fun `all template JSON keys exist in in-app XML defaults`() {
        val extraKeys = templateParameters.keys - xmlDefaults.keys
        assertTrue(
            extraKeys.isEmpty(),
            "The following keys exist in remote config template JSON but are missing in XML defaults: $extraKeys"
        )
    }

    @Test
    fun `default values in XML match template JSON default values`() {
        xmlDefaults.forEach { (key, xmlValue) ->
            val param = templateParameters[key]
            assertTrue(param != null, "Parameter '$key' should exist in template")
            val templateValue = param?.defaultValue
            assertTrue(templateValue != null, "Parameter '$key' should have a defaultValue")

            if (param?.valueType == "JSON") {
                val xmlJson = JsonParser.parseString(xmlValue)
                val tmplJson = JsonParser.parseString(templateValue)
                assertEquals(xmlJson, tmplJson, "JSON value mismatch for key: $key")
            } else {
                assertEquals(xmlValue.trim(), templateValue?.trim(), "Value mismatch for key: $key")
            }
        }
    }

    @Test
    fun `ai_receipt_monthly_limit_free defaults to 3 in template and XML`() {
        assertEquals("3", templateParameters["ai_receipt_monthly_limit_free"]?.defaultValue)
        assertEquals("3", xmlDefaults["ai_receipt_monthly_limit_free"])
    }

    @Test
    fun `ai_receipt_monthly_limit_pro defaults to -1 in template and XML`() {
        assertEquals("-1", templateParameters["ai_receipt_monthly_limit_pro"]?.defaultValue)
        assertEquals("-1", xmlDefaults["ai_receipt_monthly_limit_pro"])
    }

    @Test
    fun `manual_receipt_monthly_limit_free defaults to 5 in template and XML`() {
        assertEquals("5", templateParameters["manual_receipt_monthly_limit_free"]?.defaultValue)
        assertEquals("5", xmlDefaults["manual_receipt_monthly_limit_free"])
    }

    @Test
    fun `report_export_monthly_limit_free defaults to 1 in template and XML`() {
        assertEquals("1", templateParameters["report_export_monthly_limit_free"]?.defaultValue)
        assertEquals("1", xmlDefaults["report_export_monthly_limit_free"])
    }

    @Test
    fun `max_subunits_per_group_free defaults to 1 in template and XML`() {
        assertEquals("1", templateParameters["max_subunits_per_group_free"]?.defaultValue)
        assertEquals("1", xmlDefaults["max_subunits_per_group_free"])
    }

    @Test
    fun `max_subunits_per_group_pro defaults to -1 in template and XML`() {
        assertEquals("-1", templateParameters["max_subunits_per_group_pro"]?.defaultValue)
        assertEquals("-1", xmlDefaults["max_subunits_per_group_pro"])
    }

    @Test
    fun `max_owned_groups_pro defaults to -1 in template and XML`() {
        assertEquals("-1", templateParameters["max_owned_groups_pro"]?.defaultValue)
        assertEquals("-1", xmlDefaults["max_owned_groups_pro"])
    }

    @Test
    fun `every parameter specifies a valid valueType and non-empty description`() {
        val validTypes = setOf("STRING", "BOOLEAN", "NUMBER", "JSON")
        templateParameters.forEach { (key, param) ->
            assertTrue(
                !param.description.isNullOrBlank(),
                "Parameter '$key' must have a non-empty description"
            )
            assertTrue(
                param.valueType in validTypes,
                "Parameter '$key' has invalid valueType '${param.valueType}'. Must be one of $validTypes"
            )
        }
    }

    @Test
    fun `every parameter group specifies a non-empty description`() {
        assertTrue(groupDescriptions.isNotEmpty(), "Template must define parameterGroups")
        groupDescriptions.forEach { (groupName, description) ->
            assertTrue(
                description.isNotBlank(),
                "Parameter group '$groupName' must have a non-empty description"
            )
        }
    }

    @Test
    fun `parameter values conform to declared valueType`() {
        templateParameters.forEach { (key, param) ->
            val value = param.defaultValue
            assertTrue(value != null, "Parameter '$key' defaultValue must not be null")
            assertValueConformsToType("Parameter '$key'", param.valueType, value)
        }
    }

    @Test
    fun `every declared condition has a non-empty unique name and non-empty expression`() {
        assertFalse(hasMalformedConditions, "Top-level 'conditions' must be a list of JSON objects")
        val infixComparisonPattern = Regex("""\bapp\.(?:build|version)\s*(?:<=|>=|==|!=|<|>)""")
        val seenNames = mutableSetOf<String>()
        templateConditions.forEachIndexed { index, condition ->
            val name = condition.name
            assertTrue(!name.isNullOrBlank(), "Condition at index $index must have a non-empty name")
            val expression = condition.expression
            assertTrue(
                !expression.isNullOrBlank(),
                "Condition '$name' must have a non-empty expression"
            )
            assertFalse(
                infixComparisonPattern.containsMatchIn(expression.orEmpty()),
                "Condition '$name' must use dot-method syntax (e.g. app.build.<(['1000000'])), not infix operators"
            )
            assertTrue(seenNames.add(name!!), "Duplicate condition name '$name' found")
        }
    }

    @Test
    fun `every conditionalValue references a declared top-level condition`() {
        val declaredConditionNames = templateConditions.mapNotNull { it.name }.toSet()
        templateParameters.forEach { (key, param) ->
            assertFalse(
                param.hasMalformedConditionalValues,
                "Parameter '$key' has malformed conditionalValues"
            )
            param.conditionalValues.keys.forEach { conditionName ->
                assertTrue(
                    conditionName in declaredConditionNames,
                    "Parameter '$key' conditionalValues references undeclared condition '$conditionName'"
                )
            }
        }
    }

    @Test
    fun `every conditionalValue conforms to its parameter declared valueType`() {
        templateParameters.forEach { (key, param) ->
            param.conditionalValues.forEach { (conditionName, value) ->
                assertTrue(
                    value != null,
                    "Parameter '$key' [condition '$conditionName'] value must not be null"
                )
                assertValueConformsToType(
                    "Parameter '$key' [condition '$conditionName']",
                    param.valueType,
                    value
                )
            }
        }
    }

    @Test
    fun `ads_enabled disables ads for Pre-1_0_0 Closed Test condition while keeping default true`() {
        val closedTestCondition = templateConditions.find { it.name == "Pre-1.0.0 Closed Test" }
        assertTrue(closedTestCondition != null, "Condition 'Pre-1.0.0 Closed Test' should be declared")
        assertEquals("app.build.<(['1000000'])", closedTestCondition?.expression)

        val adsEnabledParam = templateParameters["ads_enabled"]
        assertTrue(adsEnabledParam != null, "Parameter 'ads_enabled' should exist in template")
        assertEquals("true", adsEnabledParam?.defaultValue)
        assertEquals("true", xmlDefaults["ads_enabled"])
        assertEquals("false", adsEnabledParam?.conditionalValues?.get("Pre-1.0.0 Closed Test"))
    }

    private fun assertValueConformsToType(label: String, valueType: String?, value: String?) {
        when (valueType) {
            "BOOLEAN" -> {
                assertTrue(
                    value.equals("true", ignoreCase = true) || value.equals("false", ignoreCase = true),
                    "$label is BOOLEAN but value is '$value'"
                )
            }
            "NUMBER" -> {
                val isNumber = value?.toDoubleOrNull() != null
                assertTrue(isNumber, "$label is NUMBER but value '$value' is not numeric")
            }
            "JSON" -> {
                val isJson = try {
                    JsonParser.parseString(value)
                    true
                } catch (_: Exception) {
                    false
                }
                assertTrue(isJson, "$label is JSON but value is not valid JSON")
            }
            "STRING" -> {
                assertTrue(value != null, "$label is STRING but value is null")
            }
        }
    }

    @Test
    fun `developer_info_json default value matches DEFAULT_DEVELOPER_INFO in FirebaseAppConfigRepository`() {
        val devInfoParam = templateParameters["developer_info_json"]
        assertTrue(devInfoParam != null, "Parameter 'developer_info_json' should exist in template")
        val devInfoJson = JsonParser.parseString(devInfoParam?.defaultValue).asJsonObject
        val avatarUrl = devInfoJson.get("avatar_url")?.asString

        assertEquals(
            FirebaseAppConfigRepository.DEFAULT_DEVELOPER_INFO.avatarUrl,
            avatarUrl,
            "avatar_url in template JSON should match FirebaseAppConfigRepository.DEFAULT_DEVELOPER_INFO.avatarUrl"
        )
        assertTrue(
            avatarUrl?.contains(
                "/o/public%2Favatars%2Fdeveloper.webp?alt=media&token=7151f846-8346-4944-9cc7-2d4c02bf492d"
            ) == true,
            "avatar_url should target /o/public%2Favatars%2Fdeveloper.webp?alt=media with token"
        )
    }

    @Test
    fun `app update parameters exist with expected defaults and valueTypes`() {
        val enabled = templateParameters["in_app_updates_enabled"]
        val minVersion = templateParameters["min_supported_version_code"]
        val autoForcePatch = templateParameters["auto_force_patch_updates"]
        val optionalCodes = templateParameters["optional_update_version_codes"]
        val cooldownHours = templateParameters["optional_update_cooldown_hours"]

        assertEquals("BOOLEAN", enabled?.valueType)
        assertEquals("true", enabled?.defaultValue)
        assertEquals("NUMBER", minVersion?.valueType)
        assertEquals("0", minVersion?.defaultValue)
        assertEquals("BOOLEAN", autoForcePatch?.valueType)
        assertEquals("true", autoForcePatch?.defaultValue)
        assertEquals("STRING", optionalCodes?.valueType)
        assertEquals("", optionalCodes?.defaultValue)
        assertEquals("NUMBER", cooldownHours?.valueType)
        assertEquals("24", cooldownHours?.defaultValue)
    }

    @Test
    fun `category_classifier_rules_json exists with JSON valueType and valid merge default`() {
        val param = templateParameters["category_classifier_rules_json"]
        assertTrue(param != null, "Parameter 'category_classifier_rules_json' should exist in template")
        assertEquals("JSON", param?.valueType)

        val rawValue = param?.defaultValue.orEmpty()
        assertDefaultCategoryClassifierJsonStructure(rawValue)

        val parsed = CategoryClassifierRulesParser.parse(rawValue)
        assertEquals(CategoryRuleMergeMode.MERGE, parsed.mergeMode)
        assertEquals(expectedCommonRules(), parsed.commonRules)
        assertEquals(setOf("en", "es", "fr"), parsed.languageRules.keys)
        assertEquals(expectedEnglishRules(), parsed.languageRules["en"])
        assertEquals(expectedSpanishRules(), parsed.languageRules["es"])
        assertEquals(expectedFrenchRules(), parsed.languageRules["fr"])
        assertRuleInvariants(parsed.commonRules, parsed.languageRules)
        assertEquals(
            mapOf(
                "common" to setOf("example common term"),
                "en" to setOf("example en term"),
                "es" to setOf("example es term"),
                "*" to setOf("example global term")
            ),
            parsed.disabledTerms
        )
    }

    @Test
    fun `category_classifier_rules_json default integrates with classifier without suppressing bundled rules`() {
        val rawValue = templateParameters["category_classifier_rules_json"]?.defaultValue.orEmpty()
        val appConfigService = mockk<AppConfigService>(relaxed = true)
        every { appConfigService.categoryClassifierRules } returns MutableStateFlow(
            CategoryClassifierRulesParser.parse(rawValue)
        )

        val classifier = ExpenseCategoryClassifierServiceImpl(appConfigService = appConfigService)
        assertNewDefaultClassifications(classifier)
        assertBundledClassificationsRemainActive(classifier)
    }

    private fun assertDefaultCategoryClassifierJsonStructure(rawValue: String) {
        val defaultJson = JsonParser.parseString(rawValue).asJsonObject
        val xmlJson = JsonParser.parseString(xmlDefaults["category_classifier_rules_json"].orEmpty())
        assertEquals(defaultJson, xmlJson)

        assertEquals(1, defaultJson.get("version")?.asInt)
        assertEquals("merge", defaultJson.get("merge_mode")?.asString)
        val commonArray = defaultJson.getAsJsonArray("common")
        assertTrue(commonArray != null && commonArray.size() > 0)

        assertTrue(defaultJson.get("languages")?.isJsonObject == true)
        val languages = defaultJson.getAsJsonObject("languages")
        assertTrue(languages.size() >= 3)
        listOf("en", "es", "fr").forEach { lang ->
            assertTrue(languages.has(lang) && languages.get(lang).isJsonArray)
            assertTrue(languages.getAsJsonArray(lang).size() > 0)
        }

        assertTrue(defaultJson.get("disabled_terms")?.isJsonObject == true)
        val disabledTerms = defaultJson.getAsJsonObject("disabled_terms")
        listOf("common", "en", "es", "*").forEach { key ->
            assertTrue(disabledTerms.has(key) && disabledTerms.get(key).isJsonArray)
            assertTrue(disabledTerms.getAsJsonArray(key).size() > 0)
        }
    }

    private fun expectedCommonRules(): List<CategoryKeywordRule> = listOf(
        CategoryKeywordRule(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.DELIVERY,
            phrases = listOf("uber eats", "pedidos ya"),
            keywords = listOf("rappi", "wolt")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.HOME,
            subcategory = ExpenseSubcategory.UTILITIES,
            phrases = emptyList(),
            keywords = listOf("o2")
        )
    )

    private fun expectedEnglishRules(): List<CategoryKeywordRule> = listOf(
        CategoryKeywordRule(
            category = ExpenseCategory.HEALTH_PERSONAL_CARE,
            subcategory = ExpenseSubcategory.DOCTOR_DENTIST,
            phrases = listOf("dental checkup"),
            keywords = listOf("filling")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.GROCERIES_SUPERMARKET,
            phrases = listOf("farmers market"),
            keywords = listOf("costco")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.TOBACCO_VAPING,
            phrases = listOf("pocket lighter"),
            keywords = listOf("lighter", "lighters")
        )
    )

    private fun expectedSpanishRules(): List<CategoryKeywordRule> = listOf(
        CategoryKeywordRule(
            category = ExpenseCategory.HEALTH_PERSONAL_CARE,
            subcategory = ExpenseSubcategory.DOCTOR_DENTIST,
            phrases = listOf("revision dental"),
            keywords = listOf("empaste")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.GROCERIES_SUPERMARKET,
            phrases = listOf("cash fresh"),
            keywords = listOf("hipercor")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.TOBACCO_VAPING,
            phrases = listOf("mechero recargable"),
            keywords = listOf("mechero", "encendedor")
        )
    )

    private fun expectedFrenchRules(): List<CategoryKeywordRule> = listOf(
        CategoryKeywordRule(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.CAFE_BREAKFAST,
            phrases = listOf("petit dejeuner"),
            keywords = listOf("boulangerie")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.PHARMACY_HEALTH,
            phrases = listOf("parapharmacie bio"),
            keywords = listOf("pharmacie")
        )
    )

    private fun assertRuleInvariants(
        commonRules: List<CategoryKeywordRule>,
        languageRules: Map<String, List<CategoryKeywordRule>>
    ) {
        val allRules = commonRules + languageRules.values.flatten()
        allRules.forEach { rule ->
            assertTrue(rule.phrases.isNotEmpty() || rule.keywords.isNotEmpty())
            val sub = rule.subcategory
            assertTrue(
                sub == null ||
                    (sub != ExpenseSubcategory.UNSPECIFIED && sub.parentCategory == rule.category)
            )
        }
        assertTrue(commonRules.any { it.phrases.isNotEmpty() && it.keywords.isNotEmpty() })
        languageRules.values.flatten().forEach { rule ->
            assertTrue(rule.phrases.isNotEmpty() && rule.keywords.isNotEmpty())
        }
    }

    private fun assertNewDefaultClassifications(classifier: ExpenseCategoryClassifierServiceImpl) {
        val expected = listOf(
            Triple("Uber Eats", GroupContext.EVERYDAY, "en") to
                CategorySuggestion(ExpenseCategory.FOOD, ExpenseSubcategory.DELIVERY),
            Triple("Rappi", GroupContext.TRAVEL, "es") to
                CategorySuggestion(ExpenseCategory.FOOD, ExpenseSubcategory.DELIVERY),
            Triple("o2", GroupContext.EVERYDAY, "es") to
                CategorySuggestion(ExpenseCategory.HOME, ExpenseSubcategory.UTILITIES),
            Triple("o2", GroupContext.EVERYDAY, "en") to
                CategorySuggestion(ExpenseCategory.HOME, ExpenseSubcategory.UTILITIES),
            Triple("filling", GroupContext.EVERYDAY, "en") to
                CategorySuggestion(ExpenseCategory.HEALTH_PERSONAL_CARE, ExpenseSubcategory.DOCTOR_DENTIST),
            Triple("dental checkup", GroupContext.TRAVEL, "en") to
                CategorySuggestion(ExpenseCategory.HEALTH_PERSONAL_CARE, ExpenseSubcategory.DOCTOR_DENTIST),
            Triple("lighter", GroupContext.EVERYDAY, "en") to
                CategorySuggestion(ExpenseCategory.SHOPPING, ExpenseSubcategory.TOBACCO_VAPING),
            Triple("pocket lighter", GroupContext.TRAVEL, "en") to
                CategorySuggestion(ExpenseCategory.SHOPPING, ExpenseSubcategory.TOBACCO_VAPING),
            Triple("empaste", GroupContext.EVERYDAY, "es") to
                CategorySuggestion(ExpenseCategory.HEALTH_PERSONAL_CARE, ExpenseSubcategory.DOCTOR_DENTIST),
            Triple("cash fresh", GroupContext.EVERYDAY, "es") to
                CategorySuggestion(ExpenseCategory.FOOD, ExpenseSubcategory.GROCERIES_SUPERMARKET),
            Triple("cash fresh", GroupContext.TRAVEL, "es") to
                CategorySuggestion(ExpenseCategory.FOOD, ExpenseSubcategory.GROCERIES_SUPERMARKET),
            Triple("hipercor", GroupContext.EVERYDAY, "es") to
                CategorySuggestion(ExpenseCategory.FOOD, ExpenseSubcategory.GROCERIES_SUPERMARKET),
            Triple("mechero", GroupContext.EVERYDAY, "es") to
                CategorySuggestion(ExpenseCategory.SHOPPING, ExpenseSubcategory.TOBACCO_VAPING),
            Triple("mechero recargable", GroupContext.TRAVEL, "es") to
                CategorySuggestion(ExpenseCategory.SHOPPING, ExpenseSubcategory.TOBACCO_VAPING),
            Triple("boulangerie", GroupContext.TRAVEL, "fr") to
                CategorySuggestion(ExpenseCategory.FOOD, ExpenseSubcategory.CAFE_BREAKFAST),
            Triple("pharmacie", GroupContext.EVERYDAY, "fr") to
                CategorySuggestion(ExpenseCategory.SHOPPING, ExpenseSubcategory.PHARMACY_HEALTH)
        )
        expected.forEach { (input, suggestion) ->
            val (description, context, lang) = input
            assertEquals(suggestion, classifier.classify(description, context, lang))
        }
        assertNull(classifier.classify("o2", GroupContext.TRAVEL, "es"))
        assertNull(classifier.classify("filling", GroupContext.TRAVEL, "en"))
    }

    private fun assertBundledClassificationsRemainActive(classifier: ExpenseCategoryClassifierServiceImpl) {
        val bundledCases = listOf(
            Triple("taxi", GroupContext.TRAVEL, "en") to
                CategorySuggestion(ExpenseCategory.TRANSPORT, ExpenseSubcategory.TAXI_RIDESHARE),
            Triple("bote", GroupContext.TRAVEL, "es") to
                CategorySuggestion(ExpenseCategory.OTHER, ExpenseSubcategory.TIPS_GRATUITIES),
            Triple("hotel", GroupContext.TRAVEL, "en") to
                CategorySuggestion(ExpenseCategory.LODGING, ExpenseSubcategory.HOTEL),
            Triple("factura del agua", GroupContext.EVERYDAY, "es") to
                CategorySuggestion(ExpenseCategory.HOME, ExpenseSubcategory.UTILITIES)
        )
        bundledCases.forEach { (input, suggestion) ->
            val (description, context, lang) = input
            assertEquals(suggestion, classifier.classify(description, context, lang))
        }
    }

    private fun resolveProjectRoot(): File {
        var dir = File(System.getProperty("user.dir") ?: ".")
        while (!File(dir, "settings.gradle.kts").exists()) {
            dir = dir.parentFile ?: error("Could not find project root containing settings.gradle.kts")
        }
        return dir
    }

    private fun parseXmlDefaults(xmlFile: File): Map<String, String> {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(xmlFile)
        val entries = doc.getElementsByTagName("entry")
        val defaults = mutableMapOf<String, String>()
        for (i in 0 until entries.length) {
            val entry = entries.item(i) as? Element ?: continue
            val key = entry.getElementsByTagName("key").item(0)?.textContent?.trim()
            val value = entry.getElementsByTagName("value").item(0)?.textContent
            if (key != null && value != null) {
                defaults[key] = value
            }
        }
        return defaults
    }

    private fun parseTemplate(jsonFile: File): ParsedTemplate {
        val jsonElement = JsonParser.parseString(jsonFile.readText(StandardCharsets.UTF_8))
        val rootObj = jsonElement.asJsonObject

        val (conditions, malformedConditions) = extractConditions(rootObj)

        val parameters = mutableMapOf<String, ParameterDefinition>()
        parameters.putAll(extractParameters(rootObj.getAsJsonObject("parameters")))

        val (groupParams, groupDescriptions) = extractGroups(rootObj.getAsJsonObject("parameterGroups"))
        parameters.putAll(groupParams)

        return ParsedTemplate(
            parameters = parameters,
            groupDescriptions = groupDescriptions,
            conditions = conditions,
            hasMalformedConditions = malformedConditions
        )
    }

    private fun extractConditions(rootObj: JsonObject): Pair<List<ConditionDefinition>, Boolean> {
        if (!rootObj.has("conditions")) return Pair(emptyList(), false)
        val conditionsElem = rootObj.get("conditions")
        if (!conditionsElem.isJsonArray) return Pair(emptyList(), true)

        val result = mutableListOf<ConditionDefinition>()
        var hasMalformed = false
        for (elem in conditionsElem.asJsonArray) {
            if (!elem.isJsonObject) {
                hasMalformed = true
                continue
            }
            val condObj = elem.asJsonObject
            result.add(
                ConditionDefinition(
                    name = condObj.getStringOrNull("name"),
                    expression = condObj.getStringOrNull("expression"),
                    tagColor = condObj.getStringOrNull("tagColor")
                )
            )
        }
        return Pair(result, hasMalformed)
    }

    private fun extractParameters(container: JsonObject?): Map<String, ParameterDefinition> {
        if (container == null) return emptyMap()
        val result = mutableMapOf<String, ParameterDefinition>()
        for ((key, elem) in container.entrySet()) {
            if (elem.isJsonObject) {
                result[key] = parseParameterDefinition(elem.asJsonObject)
            }
        }
        return result
    }

    private fun extractGroups(
        groupsContainer: JsonObject?
    ): Pair<Map<String, ParameterDefinition>, Map<String, String>> {
        if (groupsContainer == null) return Pair(emptyMap(), emptyMap())
        val allParams = mutableMapOf<String, ParameterDefinition>()
        val descriptions = mutableMapOf<String, String>()

        for ((groupName, groupElem) in groupsContainer.entrySet()) {
            if (!groupElem.isJsonObject) continue
            val groupObj = groupElem.asJsonObject
            descriptions[groupName] = groupObj.get("description")?.asString.orEmpty()
            allParams.putAll(extractParameters(groupObj.getAsJsonObject("parameters")))
        }
        return Pair(allParams, descriptions)
    }

    private fun parseParameterDefinition(obj: JsonObject): ParameterDefinition {
        val description = obj.getStringOrNull("description")
        val valueType = obj.getStringOrNull("valueType")

        val defaultValue = if (obj.has("defaultValue") && obj.get("defaultValue").isJsonObject) {
            obj.getAsJsonObject("defaultValue").getStringOrNull("value")
        } else {
            null
        }

        val (conditionalValues, hasMalformedConditionalValues) = parseConditionalValues(obj)

        return ParameterDefinition(
            description = description,
            valueType = valueType,
            defaultValue = defaultValue,
            conditionalValues = conditionalValues,
            hasMalformedConditionalValues = hasMalformedConditionalValues
        )
    }

    private fun parseConditionalValues(obj: JsonObject): Pair<Map<String, String?>, Boolean> {
        if (!obj.has("conditionalValues")) return Pair(emptyMap(), false)
        val condValuesElem = obj.get("conditionalValues")
        if (!condValuesElem.isJsonObject) return Pair(emptyMap(), true)

        val result = mutableMapOf<String, String?>()
        var hasMalformed = false
        for ((conditionName, condValElem) in condValuesElem.asJsonObject.entrySet()) {
            if (!condValElem.isJsonObject) {
                hasMalformed = true
                result[conditionName] = null
            } else {
                result[conditionName] = condValElem.asJsonObject.getStringOrNull("value")
            }
        }
        return Pair(result, hasMalformed)
    }

    private fun JsonObject.getStringOrNull(key: String): String? =
        if (has(key) && !get(key).isJsonNull) get(key).asString else null
}
