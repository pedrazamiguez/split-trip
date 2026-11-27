package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import java.io.File
import java.math.BigDecimal
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReceiptExtractionJsonParserTest {

    @Test
    fun `parse enforces parent category from valid subcategory when category mismatches`() {
        val rawJson = """
            {
                "amounts": ["25.00"],
                "currency": "EUR",
                "date": "2026-10-02",
                "title": "Stay",
                "category": "FOOD",
                "subcategory": "HOTEL"
            }
        """.trimIndent()

        val receipt = ReceiptExtractionJsonParser.parse(rawJson, AiEngineType.AI_CORE_GEMMA_4)

        assertEquals("LODGING", receipt.category)
        assertEquals("HOTEL", receipt.subcategory)
    }

    @Test
    fun `parse derives parent category from valid subcategory when category is missing or invalid`() {
        val responses = listOf(
            """{"amounts":["42.20"],"currency":"EUR","date":"2026-10-02","title":"Ticket","subcategory":"TRAIN"}""",
            """
                {
                    "amounts":["42.20"],
                    "currency":"EUR",
                    "date":"2026-10-02",
                    "title":"Ticket",
                    "category":"UNKNOWN",
                    "subcategory":"TRAIN"
                }
            """.trimIndent()
        )

        responses.forEach { rawJson ->
            val receipt = ReceiptExtractionJsonParser.parse(rawJson, AiEngineType.AI_CORE_GEMMA_4)
            assertEquals("TRANSPORT", receipt.category)
            assertEquals("TRAIN", receipt.subcategory)
        }
    }

    @Test
    fun `parse returns null category and subcategory when both are missing or invalid`() {
        val rawJson = """
            {
                "amounts": ["10.00"],
                "currency": "EUR",
                "date": "2026-10-02",
                "title": "Item",
                "category": "INVALID",
                "subcategory": "SPACESHIP"
            }
        """.trimIndent()

        val receipt = ReceiptExtractionJsonParser.parse(rawJson, AiEngineType.AI_CORE_GEMMA_4)

        assertNull(receipt.category)
        assertNull(receipt.subcategory)
    }

    @Test
    fun `parse handles explicit JSON null optional fields without leaking literal null strings`() {
        val rawJson = """
            {
                "amounts": [],
                "currency": "EUR",
                "date": null,
                "time": null,
                "vendor": "Museo del Prado",
                "title": "Museum admission ticket",
                "category": "ACTIVITIES",
                "subcategory": "MUSEUM_CULTURE",
                "paymentMethod": null,
                "notes": null
            }
        """.trimIndent()

        val receipt = ReceiptExtractionJsonParser.parse(rawJson, AiEngineType.AI_CORE_GEMMA_4)

        assertNull(receipt.amount)
        assertEquals("EUR", receipt.currency)
        assertNull(receipt.date)
        assertNull(receipt.time)
        assertEquals("Museo del Prado", receipt.vendor)
        assertEquals("Museum admission ticket", receipt.title)
        assertEquals("ACTIVITIES", receipt.category)
        assertEquals("MUSEUM_CULTURE", receipt.subcategory)
        assertNull(receipt.paymentMethod)
        assertNull(receipt.notes)
    }

    @Test
    fun `parse converts European and US thousands separators into exact BigDecimal`() {
        listOf("1.250,50 €", "1,250.50").forEach { rawAmount ->
            val rawJson = """{"amounts":["$rawAmount"],"currency":"EUR"}"""
            val receipt = ReceiptExtractionJsonParser.parse(rawJson, AiEngineType.AI_CORE_GEMMA_4)
            assertEquals(BigDecimal("1250.50"), receipt.amount)
        }
    }

    @Test
    fun `parse returns null amount for negative refund totals in amounts array or legacy amount field`() {
        val responses = listOf(
            """{"amounts":["-25.00"],"currency":"EUR"}""",
            """{"amounts":["25.00-"],"currency":"EUR"}""",
            """{"amount":"-15.50","currency":"EUR"}"""
        )

        responses.forEach { rawJson ->
            val receipt = ReceiptExtractionJsonParser.parse(rawJson, AiEngineType.AI_CORE_GEMMA_4)
            assertNull(receipt.amount)
        }
    }

    @Test
    fun `buildJsonSchema produces valid JSON schema with nullable optional fields and enum constraints`() {
        val schemaJson = JSONObject(ReceiptExtractionJsonParser.buildJsonSchema())
        assertFalse(schemaJson.getBoolean("additionalProperties"))

        val properties = schemaJson.getJSONObject("properties")
        val nullableKeys = listOf("date", "time", "vendor", "title", "subcategory", "paymentMethod", "notes")
        nullableKeys.forEach { key ->
            val typeArray = properties.getJSONObject(key).getJSONArray("type")
            assertEquals(listOf("string", "null"), typeArray.toStringList())
        }

        val categoryEnum = properties.getJSONObject("category").getJSONArray("enum").toStringList()
        assertEquals(ExpenseCategory.entries.map { it.name }, categoryEnum)

        val paymentMethodEnum = properties.getJSONObject("paymentMethod").getJSONArray("enum")
        val expectedPaymentMethods = PaymentMethod.entries.map { it.name }
        assertEquals(expectedPaymentMethods.size + 1, paymentMethodEnum.length())
        expectedPaymentMethods.forEachIndexed { index, name ->
            assertEquals(name, paymentMethodEnum.getString(index))
        }
        assertTrue(paymentMethodEnum.isNull(expectedPaymentMethods.size))

        val required = schemaJson.getJSONArray("required").toStringList()
        assertEquals(
            listOf(
                "amounts",
                "currency",
                "date",
                "time",
                "vendor",
                "title",
                "category",
                "subcategory",
                "paymentMethod",
                "notes"
            ),
            required
        )
    }

    @Test
    fun `DEFAULT_PROMPT_TEMPLATE matches English raw ai_prompt resource verbatim`() {
        val englishPrompt = resolveDataResourceFile("data/src/main/res/raw/ai_prompt.txt")
            .readText()
            .replace("\r\n", "\n")
            .trimEnd()

        assertEquals(englishPrompt, ReceiptExtractionServiceImpl.DEFAULT_PROMPT_TEMPLATE.trimEnd())
    }

    @Test
    fun `raw English and Spanish prompt resources contain valid taxonomy and self-consistent few-shot JSON examples`() {
        val resourcePaths = listOf(
            "data/src/main/res/raw/ai_prompt.txt",
            "data/src/main/res/raw-es/ai_prompt.txt"
        )
        resourcePaths.forEach(::assertPromptResourceValid)
    }

    private fun assertPromptResourceValid(path: String) {
        val content = resolveDataResourceFile(path).readText().replace("\r\n", "\n")
        assertTrue(content.contains("<receipt_ocr>\n%1\$s\n</receipt_ocr>"), "Missing placeholder in $path")

        ExpenseCategory.entries.forEach { category ->
            assertTrue(content.contains(category.name), "Missing category ${category.name} in $path")
        }
        ExpenseSubcategory.entries
            .filter { it != ExpenseSubcategory.UNSPECIFIED }
            .forEach { subcategory ->
                assertTrue(content.contains(subcategory.name), "Missing subcategory ${subcategory.name} in $path")
            }
        PaymentMethod.entries.forEach { method ->
            assertTrue(content.contains(method.name), "Missing paymentMethod ${method.name} in $path")
        }

        val exampleJsons = content.lines()
            .map { it.trim() }
            .filter { it.startsWith("{") && it.endsWith("}") }
            .map { JSONObject(it) }
        assertEquals(4, exampleJsons.size, "Expected 4 few-shot JSON examples in $path")

        exampleJsons.forEach { json ->
            assertEquals(EXPECTED_KEYS, json.keys().asSequence().toSet(), "Unexpected keys in $path")
            val category = json.getString("category")
            if (!json.isNull("subcategory")) {
                val subcategory = ExpenseSubcategory.fromString(json.getString("subcategory"))
                assertEquals(subcategory.parentCategory.name, category, "Mismatched parent in $path")
            }
            if (!json.isNull("title")) {
                val wordCount = json.getString("title").trim().split(Regex("\\s+")).size
                assertTrue(wordCount <= 4, "Title exceeds 4 words in $path: ${json.getString("title")}")
            }
        }

        assertTrue(
            exampleJsons.any { json ->
                json.getJSONArray("amounts").length() == 0 &&
                    json.isNull("date") &&
                    json.isNull("time") &&
                    json.isNull("paymentMethod")
            },
            "Expected sparse example in $path"
        )
        assertTrue(exampleJsons.any { json -> json.isNull("notes") }, "Expected null notes example in $path")
    }

    private fun resolveDataResourceFile(relativePath: String): File {
        val fromRoot = File(relativePath)
        return if (fromRoot.exists()) fromRoot else File("..", relativePath)
    }

    private fun JSONArray.toStringList(): List<String> {
        return List(length()) { index -> getString(index) }
    }

    private companion object {
        val EXPECTED_KEYS = setOf(
            "amounts",
            "currency",
            "date",
            "time",
            "vendor",
            "title",
            "category",
            "subcategory",
            "paymentMethod",
            "notes"
        )
    }
}
