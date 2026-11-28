package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.domain.model.CategoryPredictionOutcome
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestion
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestionSource
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExpenseCategoryAiClassifierJsonParserTest {

    @Test
    fun `buildJsonSchema for TRAVEL constrains category and subcategory enums to TRAVEL context only`() {
        val schema = JSONObject(ExpenseCategoryAiClassifierJsonParser.buildJsonSchema(GroupContext.TRAVEL))
        assertFalse(schema.getBoolean("additionalProperties"))
        assertEquals(listOf("category", "subcategory"), schema.getJSONArray("required").toStringList())

        val properties = schema.getJSONObject("properties")
        val categorySchema = properties.getJSONObject("category")
        assertEquals("string", categorySchema.getString("type"))
        val expectedCategories = ExpenseCategory.forContext(GroupContext.TRAVEL).map { it.name }
        val categoryEnum = categorySchema.getJSONArray("enum").toStringList()
        assertEquals(expectedCategories, categoryEnum)
        assertFalse(categoryEnum.contains(ExpenseCategory.HOME.name))
        assertFalse(categoryEnum.contains(ExpenseCategory.HEALTH_PERSONAL_CARE.name))

        val subcategorySchema = properties.getJSONObject("subcategory")
        assertEquals(listOf("string", "null"), subcategorySchema.getJSONArray("type").toStringList())
        val expectedSubcategories = ExpenseCategory.forContext(GroupContext.TRAVEL).flatMap { category ->
            ExpenseSubcategory.forCategory(category, GroupContext.TRAVEL).map { it.name }
        }
        val subcategoryEnum = subcategorySchema.getJSONArray("enum")
        assertEquals(expectedSubcategories.size + 1, subcategoryEnum.length())
        expectedSubcategories.forEachIndexed { index, name ->
            assertEquals(name, subcategoryEnum.getString(index))
        }
        assertTrue(subcategoryEnum.isNull(expectedSubcategories.size))
        val nonNullSubcategories = expectedSubcategories.toSet()
        assertFalse(nonNullSubcategories.contains(ExpenseSubcategory.HOME_SUPPLIES.name))
        assertFalse(nonNullSubcategories.contains(ExpenseSubcategory.PERSONAL_CARE.name))
        assertFalse(nonNullSubcategories.contains(ExpenseSubcategory.UNSPECIFIED.name))
    }

    @Test
    fun `buildJsonSchema for EVERYDAY constrains category and subcategory enums to EVERYDAY context only`() {
        val schema = JSONObject(ExpenseCategoryAiClassifierJsonParser.buildJsonSchema(GroupContext.EVERYDAY))
        val properties = schema.getJSONObject("properties")

        val expectedCategories = ExpenseCategory.forContext(GroupContext.EVERYDAY).map { it.name }
        val categoryEnum = properties.getJSONObject("category").getJSONArray("enum").toStringList()
        assertEquals(expectedCategories, categoryEnum)
        assertFalse(categoryEnum.contains(ExpenseCategory.LODGING.name))
        assertFalse(categoryEnum.contains(ExpenseCategory.ACTIVITIES.name))

        val expectedSubcategories = ExpenseCategory.forContext(GroupContext.EVERYDAY).flatMap { category ->
            ExpenseSubcategory.forCategory(category, GroupContext.EVERYDAY).map { it.name }
        }
        val subcategoryEnum = properties.getJSONObject("subcategory").getJSONArray("enum")
        assertEquals(expectedSubcategories.size + 1, subcategoryEnum.length())
        expectedSubcategories.forEachIndexed { index, name ->
            assertEquals(name, subcategoryEnum.getString(index))
        }
        assertTrue(subcategoryEnum.isNull(expectedSubcategories.size))
        val nonNullSubcategories = expectedSubcategories.toSet()
        assertFalse(nonNullSubcategories.contains(ExpenseSubcategory.HOTEL.name))
        assertFalse(nonNullSubcategories.contains(ExpenseSubcategory.SPA_WELLNESS.name))
        assertFalse(nonNullSubcategories.contains(ExpenseSubcategory.TICKETS_ATTRACTIONS.name))
        assertFalse(nonNullSubcategories.contains(ExpenseSubcategory.UNSPECIFIED.name))
    }

    @Test
    fun `parseOutcome enforces subcategory parentCategory when category mismatches for valid in-context subcategory`() {
        val travelOutcome = ExpenseCategoryAiClassifierJsonParser.parseOutcome(
            rawOutput = """{"category":"FOOD","subcategory":"HOTEL"}""",
            groupContext = GroupContext.TRAVEL
        )
        assertEquals(
            CategoryPredictionOutcome.Matched(
                CategorySuggestion(
                    category = ExpenseCategory.LODGING,
                    subcategory = ExpenseSubcategory.HOTEL,
                    source = CategorySuggestionSource.ON_DEVICE_AI
                )
            ),
            travelOutcome
        )

        val everydayOutcome = ExpenseCategoryAiClassifierJsonParser.parseOutcome(
            rawOutput = """{"category":"HOME","subcategory":"PERSONAL_CARE"}""",
            groupContext = GroupContext.EVERYDAY
        )
        assertEquals(
            CategoryPredictionOutcome.Matched(
                CategorySuggestion(
                    category = ExpenseCategory.HEALTH_PERSONAL_CARE,
                    subcategory = ExpenseSubcategory.PERSONAL_CARE,
                    source = CategorySuggestionSource.ON_DEVICE_AI
                )
            ),
            everydayOutcome
        )
    }

    @Test
    fun `parseOutcome derives category from valid in-context subcategory when category is missing, null, or unknown`() {
        val cases = listOf(
            """{"subcategory":"TICKETS_ATTRACTIONS"}""" to ExpenseSubcategory.TICKETS_ATTRACTIONS,
            """{"category":null,"subcategory":"MUSEUM_CULTURE"}""" to ExpenseSubcategory.MUSEUM_CULTURE,
            """{"category":"UNKNOWN","subcategory":"SPA_WELLNESS"}""" to ExpenseSubcategory.SPA_WELLNESS
        )

        cases.forEach { (rawOutput, expectedSubcategory) ->
            val outcome = ExpenseCategoryAiClassifierJsonParser.parseOutcome(rawOutput, GroupContext.TRAVEL)
            assertEquals(
                CategoryPredictionOutcome.Matched(
                    CategorySuggestion(
                        category = ExpenseCategory.ACTIVITIES,
                        subcategory = expectedSubcategory,
                        source = CategorySuggestionSource.ON_DEVICE_AI
                    )
                ),
                outcome
            )
        }
    }

    @Test
    fun `parseOutcome drops out-of-context subcategory while preserving valid in-context category`() {
        val outcome = ExpenseCategoryAiClassifierJsonParser.parseOutcome(
            rawOutput = """{"category":"FOOD","subcategory":"HOME_SUPPLIES"}""",
            groupContext = GroupContext.TRAVEL
        )

        assertEquals(
            CategoryPredictionOutcome.Matched(
                CategorySuggestion(
                    category = ExpenseCategory.FOOD,
                    subcategory = null,
                    source = CategorySuggestionSource.ON_DEVICE_AI
                )
            ),
            outcome
        )
    }

    @Test
    fun `parseOutcome returns NoMatch when both category and subcategory are out-of-context`() {
        assertEquals(
            CategoryPredictionOutcome.NoMatch,
            ExpenseCategoryAiClassifierJsonParser.parseOutcome(
                rawOutput = """{"category":"HOME","subcategory":"HOME_SUPPLIES"}""",
                groupContext = GroupContext.TRAVEL
            )
        )
        assertEquals(
            CategoryPredictionOutcome.NoMatch,
            ExpenseCategoryAiClassifierJsonParser.parseOutcome(
                rawOutput = """{"category":"ACTIVITIES","subcategory":"SPA_WELLNESS"}""",
                groupContext = GroupContext.EVERYDAY
            )
        )
    }

    @Test
    fun `parseOutcome returns NoMatch for uncertain or out-of-context OTHER and Matched for valid OTHER`() {
        assertEquals(
            CategoryPredictionOutcome.NoMatch,
            ExpenseCategoryAiClassifierJsonParser.parseOutcome(
                rawOutput = """{"category":"OTHER","subcategory":null}""",
                groupContext = GroupContext.TRAVEL
            )
        )
        assertEquals(
            CategoryPredictionOutcome.NoMatch,
            ExpenseCategoryAiClassifierJsonParser.parseOutcome(
                rawOutput = """{"category":"OTHER","subcategory":"PET_CARE"}""",
                groupContext = GroupContext.TRAVEL
            )
        )
        assertEquals(
            CategoryPredictionOutcome.Matched(
                CategorySuggestion(
                    category = ExpenseCategory.OTHER,
                    subcategory = ExpenseSubcategory.PET_CARE,
                    source = CategorySuggestionSource.ON_DEVICE_AI
                )
            ),
            ExpenseCategoryAiClassifierJsonParser.parseOutcome(
                rawOutput = """{"category":"OTHER","subcategory":"PET_CARE"}""",
                groupContext = GroupContext.EVERYDAY
            )
        )
    }

    @Test
    fun `parseOutcome sanitizes fences, think blocks, null variants, UNSPECIFIED, and malformed JSON`() {
        val fencedWithThink = """
            <think>Thinking through categories...</think>
            ```json
            {"category":"FOOD","subcategory":"RESTAURANT"}
            ```
        """.trimIndent()
        assertEquals(
            CategoryPredictionOutcome.Matched(
                CategorySuggestion(
                    category = ExpenseCategory.FOOD,
                    subcategory = ExpenseSubcategory.RESTAURANT,
                    source = CategorySuggestionSource.ON_DEVICE_AI
                )
            ),
            ExpenseCategoryAiClassifierJsonParser.parseOutcome(fencedWithThink, GroupContext.TRAVEL)
        )

        listOf(
            """{"category":"FOOD","subcategory":null}""",
            """{"category":"FOOD","subcategory":"null"}""",
            """{"category":"FOOD","subcategory":"UNSPECIFIED"}"""
        ).forEach { rawOutput ->
            assertEquals(
                CategoryPredictionOutcome.Matched(
                    CategorySuggestion(
                        category = ExpenseCategory.FOOD,
                        subcategory = null,
                        source = CategorySuggestionSource.ON_DEVICE_AI
                    )
                ),
                ExpenseCategoryAiClassifierJsonParser.parseOutcome(rawOutput, GroupContext.TRAVEL)
            )
        }

        listOf(
            "not json at all",
            "{malformed:",
            """{"category":"UNKNOWN_CATEGORY","subcategory":null}"""
        ).forEach { rawOutput ->
            assertEquals(
                CategoryPredictionOutcome.NoMatch,
                ExpenseCategoryAiClassifierJsonParser.parseOutcome(rawOutput, GroupContext.TRAVEL)
            )
        }
    }

    @Test
    fun `DEFAULT_TRAVEL_PROMPT_TEMPLATE and DEFAULT_EVERYDAY_PROMPT_TEMPLATE match English raw resources verbatim`() {
        val travelRaw = resolveDataResourceFile("data/src/main/res/raw/ai_category_travel_prompt.txt")
            .readText()
            .replace("\r\n", "\n")
        assertEquals(travelRaw, ExpenseCategoryAiClassifierServiceImpl.DEFAULT_TRAVEL_PROMPT_TEMPLATE)

        val everydayRaw = resolveDataResourceFile("data/src/main/res/raw/ai_category_everyday_prompt.txt")
            .readText()
            .replace("\r\n", "\n")
        assertEquals(everydayRaw, ExpenseCategoryAiClassifierServiceImpl.DEFAULT_EVERYDAY_PROMPT_TEMPLATE)
    }

    @Test
    fun `raw TRAVEL prompt resources have valid placeholders, guidance, bilingual examples, and no TRAIN bias`() {
        listOf(
            "data/src/main/res/raw/ai_category_travel_prompt.txt",
            "data/src/main/res/raw-es/ai_category_travel_prompt.txt"
        ).forEach { path ->
            val content = resolveDataResourceFile(path).readText().replace("\r\n", "\n")
            assertTrue(content.contains("{{TAXONOMY}}"), "Missing TAXONOMY placeholder in $path")
            assertTrue(content.contains("<expense_title>\n%1\$s\n</expense_title>"), "Missing input block in $path")

            listOf(
                "TICKETS_ATTRACTIONS",
                "MUSEUM_CULTURE",
                "NATURE_PARK",
                "SPA_WELLNESS",
                "TIPS_GRATUITIES"
            ).forEach { term ->
                assertTrue(content.contains(term), "Missing guidance term $term in $path")
            }

            val examples = extractExampleOutputs(content)
            assertEquals(EXPECTED_EXAMPLE_COUNT, examples.size, "Expected 8 examples in $path")

            val distinctCategories = mutableSetOf<ExpenseCategory>()
            examples.forEach { json ->
                assertEquals(EXPECTED_KEYS, json.keys().asSequence().toSet(), "Unexpected keys in $path")
                val category = ExpenseCategory.fromString(json.getString("category"))
                val subcategory = ExpenseSubcategory.fromString(json.getString("subcategory"))
                assertTrue(category.appliesTo(GroupContext.TRAVEL), "$category not valid for TRAVEL in $path")
                assertTrue(subcategory.appliesTo(GroupContext.TRAVEL), "$subcategory not valid for TRAVEL in $path")
                assertEquals(category, subcategory.parentCategory, "Mismatched parent in $path")
                distinctCategories.add(category)
            }

            val titles = extractExampleTitles(content)
            assertTrue(titles.any { it.contains("Colosseum") || it.contains("Dinner") }, "Missing EN example in $path")
            assertTrue(titles.any { it.contains("Entradas") || it.contains("Masaje") }, "Missing ES example in $path")
            assertTrue(distinctCategories.size >= MIN_TRAVEL_EXAMPLE_CATEGORIES, "Too few categories in $path")

            val lastExample = examples.last()
            val lastCategory = ExpenseCategory.fromString(lastExample.getString("category"))
            val lastSubcategory = ExpenseSubcategory.fromString(lastExample.getString("subcategory"))
            assertFalse(
                lastCategory == ExpenseCategory.TRANSPORT && lastSubcategory == ExpenseSubcategory.TRAIN,
                "Last example must not be TRANSPORT:TRAIN in $path"
            )
            assertNotEquals(ExpenseCategory.OTHER, lastCategory, "Last example must not be OTHER in $path")
        }
    }

    @Test
    fun `raw EVERYDAY prompt resources have valid placeholders, guidance, examples, and no HOME_SUPPLIES bias`() {
        listOf(
            "data/src/main/res/raw/ai_category_everyday_prompt.txt",
            "data/src/main/res/raw-es/ai_category_everyday_prompt.txt"
        ).forEach { path ->
            val content = resolveDataResourceFile(path).readText().replace("\r\n", "\n")
            assertTrue(content.contains("{{TAXONOMY}}"), "Missing TAXONOMY placeholder in $path")
            assertTrue(content.contains("<expense_title>\n%1\$s\n</expense_title>"), "Missing input block in $path")

            listOf("PERSONAL_CARE", "HOME_SUPPLIES", "TIPS_GRATUITIES", "masaje", "spa").forEach { term ->
                assertTrue(content.contains(term), "Missing guidance term $term in $path")
            }

            val examples = extractExampleOutputs(content)
            assertEquals(EXPECTED_EXAMPLE_COUNT, examples.size, "Expected 8 examples in $path")

            val distinctCategories = mutableSetOf<ExpenseCategory>()
            examples.forEach { json ->
                assertEquals(EXPECTED_KEYS, json.keys().asSequence().toSet(), "Unexpected keys in $path")
                val category = ExpenseCategory.fromString(json.getString("category"))
                val subcategory = ExpenseSubcategory.fromString(json.getString("subcategory"))
                assertTrue(category.appliesTo(GroupContext.EVERYDAY), "$category not valid for EVERYDAY in $path")
                assertTrue(subcategory.appliesTo(GroupContext.EVERYDAY), "$subcategory not valid for EVERYDAY in $path")
                assertEquals(category, subcategory.parentCategory, "Mismatched parent in $path")
                distinctCategories.add(category)
            }

            val titles = extractExampleTitles(content)
            assertTrue(titles.any { it.contains("Monthly") || it.contains("Dentist") }, "Missing EN example in $path")
            assertTrue(titles.any { it.contains("Detergente") || it.contains("masaje") }, "Missing ES example in $path")
            assertTrue(distinctCategories.size >= MIN_EVERYDAY_EXAMPLE_CATEGORIES, "Too few categories in $path")

            val lastExample = examples.last()
            val lastCategory = ExpenseCategory.fromString(lastExample.getString("category"))
            val lastSubcategory = ExpenseSubcategory.fromString(lastExample.getString("subcategory"))
            assertFalse(
                lastCategory == ExpenseCategory.HOME && lastSubcategory == ExpenseSubcategory.HOME_SUPPLIES,
                "Last example must not be HOME:HOME_SUPPLIES in $path"
            )
            assertNotEquals(ExpenseCategory.OTHER, lastCategory, "Last example must not be OTHER in $path")
        }
    }

    private fun extractExampleOutputs(content: String): List<JSONObject> =
        EXAMPLE_OUTPUT_REGEX.findAll(content).map { match ->
            JSONObject(match.groupValues[1])
        }.toList()

    private fun extractExampleTitles(content: String): List<String> =
        EXAMPLE_TITLE_REGEX.findAll(content).map { match ->
            match.groupValues[1].trim()
        }.toList()

    private fun resolveDataResourceFile(relativePath: String): File {
        val fromRoot = File(relativePath)
        return if (fromRoot.exists()) fromRoot else File("..", relativePath)
    }

    private fun JSONArray.toStringList(): List<String> =
        List(length()) { index -> getString(index) }

    private companion object {
        const val EXPECTED_EXAMPLE_COUNT = 8
        const val MIN_TRAVEL_EXAMPLE_CATEGORIES = 5
        const val MIN_EVERYDAY_EXAMPLE_CATEGORIES = 6
        val EXPECTED_KEYS = setOf("category", "subcategory")
        val EXAMPLE_OUTPUT_REGEX = Regex("""(?m)^Output:\s*(\{.*\})\s*$""")
        val EXAMPLE_TITLE_REGEX = Regex("""<expense_title>\n(.*?)\n</expense_title>\nOutput: \{""")
    }
}
