package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.domain.model.CategoryClassifierRuleConfig
import es.pedrazamiguez.splittrip.domain.model.CategoryKeywordRule
import es.pedrazamiguez.splittrip.domain.model.CategoryRuleMergeMode
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestion
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestionSource
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCategoryClassifierService
import es.pedrazamiguez.splittrip.domain.service.category.BundledCategoryRuleCatalog
import es.pedrazamiguez.splittrip.domain.service.category.CategoryTextNormalizer
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ExpenseCategoryClassifierServiceImplTest {

    private lateinit var rulesConfigFlow: MutableStateFlow<CategoryClassifierRuleConfig>
    private lateinit var appConfigService: AppConfigService
    private lateinit var classifier: ExpenseCategoryClassifierServiceImpl

    @BeforeEach
    fun setUp() {
        rulesConfigFlow = MutableStateFlow(CategoryClassifierRuleConfig.EMPTY)
        appConfigService = mockk(relaxed = true) {
            every { categoryClassifierRules } returns rulesConfigFlow
        }
        classifier = ExpenseCategoryClassifierServiceImpl(appConfigService = appConfigService)
    }

    @Test
    fun `classifies Spanish and English issue examples accurately`() {
        assertEquals(
            CategorySuggestion(ExpenseCategory.HEALTH_PERSONAL_CARE, ExpenseSubcategory.GYM_FITNESS),
            classifier.classify("cuota gimnasio", GroupContext.EVERYDAY, "es")
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.FOOD, ExpenseSubcategory.RESTAURANT),
            classifier.classify("dinner at Bahn Mi 25", GroupContext.TRAVEL, "en")
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.HOME, ExpenseSubcategory.FURNITURE_APPLIANCES),
            classifier.classify("tostadora", GroupContext.EVERYDAY, "es")
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.HOME, ExpenseSubcategory.UTILITIES),
            classifier.classify("factura del agua", GroupContext.EVERYDAY, "es")
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.LODGING, ExpenseSubcategory.HOTEL),
            classifier.classify("3 nights Chengdu hotel", GroupContext.TRAVEL, "en")
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.TRANSPORT, null),
            classifier.classify("trayecto a Shaxi", GroupContext.TRAVEL, "es")
        )
    }

    @Test
    fun `classifies everyday lifestyle and shopping subcategories added in 2089`() {
        assertEquals(
            CategorySuggestion(ExpenseCategory.SHOPPING, ExpenseSubcategory.PHARMACY_HEALTH),
            classifier.classify("farmacia", GroupContext.TRAVEL, "es")
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.SHOPPING, ExpenseSubcategory.PHARMACY_HEALTH),
            classifier.classify("farmacia", GroupContext.EVERYDAY, "es")
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.SHOPPING, ExpenseSubcategory.TOBACCO_VAPING),
            classifier.classify("estanco tabaco", GroupContext.EVERYDAY, "es")
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.SHOPPING, ExpenseSubcategory.HOBBIES),
            classifier.classify("libreria comic", GroupContext.EVERYDAY, "es")
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.HEALTH_PERSONAL_CARE, ExpenseSubcategory.PERSONAL_CARE),
            classifier.classify("champu y desodorante", GroupContext.EVERYDAY, "es")
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.TRANSPORT, ExpenseSubcategory.CAR_REPAIR_MAINTENANCE),
            classifier.classify("taller mecanico", GroupContext.EVERYDAY, "es")
        )
    }

    @Test
    fun `strips diacritics and ignores case and punctuation`() {
        assertEquals(
            CategorySuggestion(ExpenseCategory.FOOD, ExpenseSubcategory.CAFE_BREAKFAST),
            classifier.classify("Café y Tostada!", GroupContext.TRAVEL, "es")
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.HEALTH_PERSONAL_CARE, ExpenseSubcategory.HAIRDRESSER_BARBER),
            classifier.classify("Peluquería María", GroupContext.EVERYDAY, "es")
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.TRANSPORT, ExpenseSubcategory.FUEL),
            classifier.classify("Gasolinera Repsol", GroupContext.TRAVEL, "es")
        )
    }

    @Test
    fun `respects word boundaries and does not match substrings`() {
        assertNull(classifier.classify("Barcelona", GroupContext.TRAVEL, "es"))
        assertNull(classifier.classify("embarque", GroupContext.TRAVEL, "es"))
    }

    @Test
    fun `weights ambiguous keywords by GroupContext`() {
        assertEquals(
            CategorySuggestion(ExpenseCategory.SHOPPING, ExpenseSubcategory.SOUVENIRS_GIFTS),
            classifier.classify("regalo", GroupContext.TRAVEL, "es")
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.SHOPPING, ExpenseSubcategory.GIFTS),
            classifier.classify("regalo", GroupContext.EVERYDAY, "es")
        )
    }

    @Test
    fun `allows explicit cross-context matches when title is unambiguous`() {
        assertEquals(
            CategorySuggestion(ExpenseCategory.LODGING, ExpenseSubcategory.HOTEL),
            classifier.classify("3 nights Chengdu hotel", GroupContext.EVERYDAY, "en")
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.HEALTH_PERSONAL_CARE, ExpenseSubcategory.GYM_FITNESS),
            classifier.classify("cuota gimnasio", GroupContext.TRAVEL, "es")
        )
    }

    @Test
    fun `prefers specific phrase rules over weaker single-token matches`() {
        assertEquals(
            CategorySuggestion(ExpenseCategory.TRANSPORT, ExpenseSubcategory.CAR_RENTAL),
            classifier.classify("alquiler coche aeropuerto", GroupContext.EVERYDAY, "es")
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.INSURANCE, ExpenseSubcategory.TRAVEL_INSURANCE),
            classifier.classify("seguro de viaje", GroupContext.EVERYDAY, "es")
        )
    }

    @Test
    fun `returns null for blank, gibberish, or ambiguous unmatched titles`() {
        assertNull(classifier.classify("", GroupContext.TRAVEL, "en"))
        assertNull(classifier.classify("   ", GroupContext.EVERYDAY, "en"))
        assertNull(classifier.classify("!!! ---", GroupContext.TRAVEL, "es"))
        assertNull(classifier.classify("Shaxi", GroupContext.TRAVEL, "en"))
        assertNull(classifier.classify("Juan", GroupContext.EVERYDAY, "es"))
        assertNull(classifier.classify("xyz 123", GroupContext.TRAVEL, "en"))
    }

    @Test
    fun `handles tied candidates across categories and within the same category`() {
        assertNull(classifier.classify("pizza taxi", GroupContext.TRAVEL, "en"))
        assertEquals(
            CategorySuggestion(ExpenseCategory.LODGING, null),
            classifier.classify("hotel hostel", GroupContext.TRAVEL, "en")
        )
    }

    @Test
    fun `every rule in bundled packs produces a taxonomy-valid CategorySuggestion and covers all subcategories`() {
        val allRules = BundledCategoryRuleCatalog.commonRules +
            BundledCategoryRuleCatalog.languagePacks.values.flatten()
        assertTrue(allRules.isNotEmpty())

        allRules.forEach { rule ->
            val sub = rule.subcategory
            assertTrue(
                sub == null || (sub != ExpenseSubcategory.UNSPECIFIED && sub.parentCategory == rule.category),
                "Invalid rule pair: ${rule.category} -> $sub"
            )
            val suggestion = CategorySuggestion(
                category = rule.category,
                subcategory = rule.subcategory,
                source = CategorySuggestionSource.LOCAL_RULES
            )
            assertEquals(rule.category, suggestion.category)

            rule.phrases.forEach { phrase ->
                assertEquals(
                    CategoryTextNormalizer.normalize(phrase),
                    phrase,
                    "Bundled phrase '$phrase' must be pre-normalized"
                )
            }
            rule.keywords.forEach { keyword ->
                assertEquals(
                    CategoryTextNormalizer.normalize(keyword),
                    keyword,
                    "Bundled keyword '$keyword' must be pre-normalized"
                )
                assertTrue(' ' !in keyword, "Bundled keyword '$keyword' must be a single token")
            }
        }

        val expectedSubcategories = ExpenseSubcategory.entries
            .filter { it != ExpenseSubcategory.UNSPECIFIED }
            .toSet()
        val coveredAll = allRules.mapNotNull { it.subcategory }.toSet()
        val coveredEn = (
            BundledCategoryRuleCatalog.commonRules +
                BundledCategoryRuleCatalog.languagePacks[BundledCategoryRuleCatalog.PACK_EN].orEmpty()
            ).mapNotNull { it.subcategory }.toSet()
        val coveredEs = (
            BundledCategoryRuleCatalog.commonRules +
                BundledCategoryRuleCatalog.languagePacks[BundledCategoryRuleCatalog.PACK_ES].orEmpty()
            ).mapNotNull { it.subcategory }.toSet()

        assertEquals(expectedSubcategories, coveredAll)
        assertEquals(expectedSubcategories, coveredEn)
        assertEquals(expectedSubcategories, coveredEs)
    }

    @Test
    fun `maps Andaluz language code es-AN to Spanish base rules`() {
        assertEquals(
            CategorySuggestion(ExpenseCategory.HOME, ExpenseSubcategory.UTILITIES),
            classifier.classify("factura del agua", GroupContext.EVERYDAY, "es-AN")
        )
    }

    @Test
    fun `avoids cross-language homograph collision by prioritizing active language over other language packs`() {
        assertNull(classifier.classify("pension", GroupContext.TRAVEL, "en"))
        assertEquals(
            CategorySuggestion(ExpenseCategory.LODGING, ExpenseSubcategory.HOMESTAY),
            classifier.classify("pension", GroupContext.TRAVEL, "es")
        )

        rulesConfigFlow.value = CategoryClassifierRuleConfig(
            languageRules = mapOf(
                "fr" to listOf(
                    CategoryKeywordRule(
                        category = ExpenseCategory.FOOD,
                        subcategory = ExpenseSubcategory.CAFE_BREAKFAST,
                        keywords = listOf("pain")
                    )
                ),
                "en" to listOf(
                    CategoryKeywordRule(
                        category = ExpenseCategory.SHOPPING,
                        subcategory = ExpenseSubcategory.PHARMACY_HEALTH,
                        keywords = listOf("pain")
                    )
                )
            )
        )

        assertEquals(
            CategorySuggestion(ExpenseCategory.FOOD, ExpenseSubcategory.CAFE_BREAKFAST),
            classifier.classify("pain", GroupContext.EVERYDAY, "fr")
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.SHOPPING, ExpenseSubcategory.PHARMACY_HEALTH),
            classifier.classify("pain", GroupContext.EVERYDAY, "en")
        )
    }

    @Test
    fun `falls back to English pack when active language and common packs have no match`() {
        assertEquals(
            CategorySuggestion(ExpenseCategory.TRANSPORT, ExpenseSubcategory.DOMESTIC_FLIGHT),
            classifier.classify("domestic flight to Tokyo", GroupContext.TRAVEL, "es")
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.TRANSPORT, ExpenseSubcategory.CAR_REPAIR_MAINTENANCE),
            classifier.classify("oil change", GroupContext.EVERYDAY, "fr")
        )
    }

    @Test
    fun `does not fall back to English when active language already has an ambiguous tie`() {
        assertNull(classifier.classify("pizza taxi", GroupContext.TRAVEL, "es"))
    }

    @Test
    fun `merges Remote Config common and language additions with bundled defaults and pre-normalizes input`() {
        rulesConfigFlow.value = CategoryClassifierRuleConfig(
            mergeMode = CategoryRuleMergeMode.MERGE,
            commonRules = listOf(
                CategoryKeywordRule(
                    category = ExpenseCategory.FOOD,
                    subcategory = ExpenseSubcategory.DELIVERY,
                    keywords = listOf("Rappi")
                )
            ),
            languageRules = mapOf(
                "es" to listOf(
                    CategoryKeywordRule(
                        category = ExpenseCategory.FOOD,
                        subcategory = ExpenseSubcategory.GROCERIES_SUPERMARKET,
                        keywords = listOf("BonPreu Super!")
                    )
                ),
                "fr" to listOf(
                    CategoryKeywordRule(
                        category = ExpenseCategory.FOOD,
                        subcategory = ExpenseSubcategory.CAFE_BREAKFAST,
                        keywords = listOf("boulangerie")
                    )
                )
            )
        )

        assertEquals(
            CategorySuggestion(ExpenseCategory.FOOD, ExpenseSubcategory.DELIVERY),
            classifier.classify("Rappi cena", GroupContext.EVERYDAY, "en")
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.FOOD, ExpenseSubcategory.GROCERIES_SUPERMARKET),
            classifier.classify("Compra BonPreu Super", GroupContext.EVERYDAY, "es")
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.FOOD, ExpenseSubcategory.CAFE_BREAKFAST),
            classifier.classify("boulangerie", GroupContext.TRAVEL, "fr")
        )
    }

    @Test
    fun `applies Remote Config disabled_terms to suppress false-positive bundled keywords`() {
        rulesConfigFlow.value = CategoryClassifierRuleConfig(
            disabledTerms = mapOf(
                "es" to setOf("bote"),
                "*" to setOf("taxi")
            )
        )

        assertNull(classifier.classify("bote", GroupContext.TRAVEL, "es"))
        assertEquals(
            CategorySuggestion(ExpenseCategory.OTHER, ExpenseSubcategory.TIPS_GRATUITIES),
            classifier.classify("propina", GroupContext.TRAVEL, "es")
        )
        assertNull(classifier.classify("taxi", GroupContext.TRAVEL, "en"))
        assertNull(classifier.classify("taxi", GroupContext.TRAVEL, "es"))
    }

    @Test
    fun `supports REPLACE merge mode per pack while falling back to bundled packs when omitted or empty`() {
        rulesConfigFlow.value = CategoryClassifierRuleConfig(
            mergeMode = CategoryRuleMergeMode.REPLACE,
            languageRules = mapOf(
                "es" to listOf(
                    CategoryKeywordRule(
                        category = ExpenseCategory.FOOD,
                        subcategory = ExpenseSubcategory.RESTAURANT,
                        keywords = listOf("chiringuito")
                    )
                )
            )
        )

        assertNull(classifier.classify("tostadora", GroupContext.EVERYDAY, "es"))
        assertEquals(
            CategorySuggestion(ExpenseCategory.FOOD, ExpenseSubcategory.RESTAURANT),
            classifier.classify("chiringuito", GroupContext.TRAVEL, "es")
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.LODGING, ExpenseSubcategory.HOTEL),
            classifier.classify("hotel", GroupContext.TRAVEL, "es")
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.HOME, ExpenseSubcategory.FURNITURE_APPLIANCES),
            classifier.classify("toaster", GroupContext.EVERYDAY, "en")
        )

        rulesConfigFlow.value = CategoryClassifierRuleConfig(mergeMode = CategoryRuleMergeMode.REPLACE)
        assertEquals(
            CategorySuggestion(ExpenseCategory.HOME, ExpenseSubcategory.FURNITURE_APPLIANCES),
            classifier.classify("tostadora", GroupContext.EVERYDAY, "es")
        )
    }

    @Test
    fun `two-argument interface classify defaults languageCode to null and config isEmpty reflects content`() {
        val serviceInterface: ExpenseCategoryClassifierService = classifier
        assertEquals(
            CategorySuggestion(ExpenseCategory.LODGING, ExpenseSubcategory.HOTEL),
            serviceInterface.classify("hotel", GroupContext.TRAVEL)
        )

        assertTrue(CategoryClassifierRuleConfig.EMPTY.isEmpty)
        assertTrue(
            CategoryClassifierRuleConfig(
                languageRules = mapOf("es" to emptyList()),
                disabledTerms = mapOf("es" to emptySet())
            ).isEmpty
        )
        assertFalse(
            CategoryClassifierRuleConfig(
                commonRules = listOf(
                    CategoryKeywordRule(category = ExpenseCategory.FOOD, keywords = listOf("rappi"))
                )
            ).isEmpty
        )
        assertFalse(
            CategoryClassifierRuleConfig(
                languageRules = mapOf(
                    "es" to listOf(
                        CategoryKeywordRule(category = ExpenseCategory.FOOD, keywords = listOf("tapa"))
                    )
                )
            ).isEmpty
        )
        assertFalse(
            CategoryClassifierRuleConfig(
                disabledTerms = mapOf("es" to setOf("bote"))
            ).isEmpty
        )
    }
}
