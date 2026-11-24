package es.pedrazamiguez.splittrip.data.firebase.config

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.model.CategoryClassifierRuleConfig
import es.pedrazamiguez.splittrip.domain.model.CategoryKeywordRule
import es.pedrazamiguez.splittrip.domain.model.CategoryRuleMergeMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CategoryClassifierRulesParserTest {

    @Test
    fun `returns EMPTY config for blank, malformed, or non-object JSON`() {
        listOf("", "   ", "{invalid", "[]", "\"text\"", "42", "null").forEach { raw ->
            assertEquals(CategoryClassifierRuleConfig.EMPTY, CategoryClassifierRulesParser.parse(raw))
        }
    }

    @Test
    fun `parses valid merge mode, common rules, language packs, and disabled terms with pre-normalization`() {
        val parsed = CategoryClassifierRulesParser.parse(validFullCatalogJson())

        assertEquals(CategoryRuleMergeMode.MERGE, parsed.mergeMode)
        assertEquals(
            listOf(
                CategoryKeywordRule(
                    category = ExpenseCategory.FOOD,
                    subcategory = ExpenseSubcategory.DELIVERY,
                    phrases = listOf("uber eats", "pedidos ya"),
                    keywords = listOf("rappi")
                )
            ),
            parsed.commonRules
        )
        assertEquals(
            listOf(
                CategoryKeywordRule(
                    category = ExpenseCategory.FOOD,
                    subcategory = ExpenseSubcategory.GROCERIES_SUPERMARKET,
                    phrases = listOf("bonpreu super"),
                    keywords = emptyList()
                ),
                CategoryKeywordRule(
                    category = ExpenseCategory.HOME,
                    subcategory = ExpenseSubcategory.UTILITIES,
                    phrases = emptyList(),
                    keywords = listOf("endesa")
                )
            ),
            parsed.languageRules["es"]
        )
        assertEquals(
            listOf(
                CategoryKeywordRule(
                    category = ExpenseCategory.FOOD,
                    subcategory = ExpenseSubcategory.CAFE_BREAKFAST,
                    phrases = emptyList(),
                    keywords = listOf("boulangerie")
                )
            ),
            parsed.languageRules["fr"]
        )
        assertEquals(setOf("taxi"), parsed.disabledTerms["common"])
        assertEquals(setOf("bote"), parsed.disabledTerms["es"])
        assertEquals(setOf("cafe"), parsed.disabledTerms["*"])
    }

    private fun validFullCatalogJson(): String = """
        {
          "version": 1,
          "merge_mode": "merge",
          "common": [
            {
              "category": "FOOD",
              "subcategory": "DELIVERY",
              "phrases": ["Uber Eats!"],
              "keywords": ["Rappi", "Pedidos Ya", "rappi"]
            }
          ],
          "languages": {
            "ES": [
              {
                "category": "FOOD",
                "subcategory": "GROCERIES_SUPERMARKET",
                "keywords": ["BonPreu Súper!"]
              }
            ],
            "es-AN": [
              {
                "category": "HOME",
                "subcategory": "UTILITIES",
                "keywords": ["Endesa"]
              }
            ],
            "fr-FR": [
              {
                "category": "FOOD",
                "subcategory": "CAFE_BREAKFAST",
                "keywords": ["Boulangerie"]
              }
            ]
          },
          "disabled_terms": {
            "common": ["Taxi!"],
            "es-AN": ["Bote"],
            "*": ["Café"]
          }
        }
    """.trimIndent()

    @Test
    fun `parses replace merge mode case-insensitively and defaults unknown merge modes to MERGE`() {
        val replaceLower = CategoryClassifierRulesParser.parse("""{"merge_mode":"replace"}""")
        val replaceUpper = CategoryClassifierRulesParser.parse("""{"merge_mode":" REPLACE "}""")
        val unknownMode = CategoryClassifierRulesParser.parse("""{"merge_mode":"overwrite"}""")
        val numericMode = CategoryClassifierRulesParser.parse("""{"merge_mode":123}""")

        assertEquals(CategoryRuleMergeMode.REPLACE, replaceLower.mergeMode)
        assertEquals(CategoryRuleMergeMode.REPLACE, replaceUpper.mergeMode)
        assertEquals(CategoryRuleMergeMode.MERGE, unknownMode.mergeMode)
        assertEquals(CategoryRuleMergeMode.MERGE, numericMode.mergeMode)
    }

    @Test
    fun `ignores unknown category entries without failing valid sibling entries`() {
        val json = """
            {
              "common": [
                {
                  "category": "UNKNOWN_CAT",
                  "keywords": ["foo"]
                },
                {
                  "category": "LODGING",
                  "subcategory": "HOTEL",
                  "keywords": ["hotel"]
                }
              ]
            }
        """.trimIndent()

        val parsed = CategoryClassifierRulesParser.parse(json)

        assertEquals(
            listOf(
                CategoryKeywordRule(
                    category = ExpenseCategory.LODGING,
                    subcategory = ExpenseSubcategory.HOTEL,
                    keywords = listOf("hotel")
                )
            ),
            parsed.commonRules
        )
    }

    @Test
    fun `ignores invalid subcategories and preserves null subcategory rules`() {
        val json = """
            {
              "common": [
                {
                  "category": "FOOD",
                  "subcategory": "UNKNOWN_SUB",
                  "keywords": ["alpha"]
                },
                {
                  "category": "FOOD",
                  "subcategory": "UNSPECIFIED",
                  "keywords": ["beta"]
                },
                {
                  "category": "FOOD",
                  "subcategory": "HOTEL",
                  "keywords": ["gamma"]
                },
                {
                  "category": "TRANSPORT",
                  "subcategory": null,
                  "keywords": ["transit"]
                }
              ]
            }
        """.trimIndent()

        val parsed = CategoryClassifierRulesParser.parse(json)

        assertEquals(
            listOf(
                CategoryKeywordRule(
                    category = ExpenseCategory.TRANSPORT,
                    subcategory = null,
                    keywords = listOf("transit")
                )
            ),
            parsed.commonRules
        )
    }

    @Test
    fun `ignores rules whose phrases and keywords become empty after normalization`() {
        val json = """
            {
              "common": [
                {
                  "category": "FOOD",
                  "subcategory": "RESTAURANT",
                  "phrases": ["---"],
                  "keywords": ["!!!", "   "]
                }
              ]
            }
        """.trimIndent()

        val parsed = CategoryClassifierRulesParser.parse(json)

        assertTrue(parsed.commonRules.isEmpty())
    }

    @Test
    fun `ignores non-array or wrong-type JSON fields defensively without crashing`() {
        val json = """
            {
              "common": "not-an-array",
              "languages": {
                "": [{"category":"FOOD","keywords":["test"]}],
                "es": "not-an-array",
                "en": [
                  "not-an-object",
                  {
                    "category": 123,
                    "keywords": ["ignored"]
                  },
                  {
                    "category": "FOOD",
                    "subcategory": 999,
                    "keywords": ["ignored"]
                  },
                  {
                    "category": "FOOD",
                    "subcategory": "RESTAURANT",
                    "phrases": [123, true, null, "Night Market"],
                    "keywords": [false, {}, "bistro"]
                  }
                ]
              },
              "disabled_terms": "not-an-object"
            }
        """.trimIndent()

        val parsed = CategoryClassifierRulesParser.parse(json)

        assertTrue(parsed.commonRules.isEmpty())
        assertTrue(parsed.disabledTerms.isEmpty())
        assertEquals(
            listOf(
                CategoryKeywordRule(
                    category = ExpenseCategory.FOOD,
                    subcategory = ExpenseSubcategory.RESTAURANT,
                    phrases = listOf("night market"),
                    keywords = listOf("bistro")
                )
            ),
            parsed.languageRules["en"]
        )
    }
}
