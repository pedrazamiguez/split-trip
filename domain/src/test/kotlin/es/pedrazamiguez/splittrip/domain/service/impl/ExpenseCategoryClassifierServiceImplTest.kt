package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestion
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestionSource
import es.pedrazamiguez.splittrip.domain.service.category.CategoryEverydayAndLifestyleRules
import es.pedrazamiguez.splittrip.domain.service.category.CategoryTravelAndCoreRules
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExpenseCategoryClassifierServiceImplTest {

    private val classifier = ExpenseCategoryClassifierServiceImpl()

    @Test
    fun `classifies Spanish and English issue examples accurately`() {
        assertEquals(
            CategorySuggestion(ExpenseCategory.HEALTH_PERSONAL_CARE, ExpenseSubcategory.GYM_FITNESS),
            classifier.classify("cuota gimnasio", GroupContext.EVERYDAY)
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.FOOD, ExpenseSubcategory.RESTAURANT),
            classifier.classify("dinner at Bahn Mi 25", GroupContext.TRAVEL)
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.HOME, ExpenseSubcategory.FURNITURE_APPLIANCES),
            classifier.classify("tostadora", GroupContext.EVERYDAY)
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.HOME, ExpenseSubcategory.UTILITIES),
            classifier.classify("factura del agua", GroupContext.EVERYDAY)
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.LODGING, ExpenseSubcategory.HOTEL),
            classifier.classify("3 nights Chengdu hotel", GroupContext.TRAVEL)
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.TRANSPORT, null),
            classifier.classify("trayecto a Shaxi", GroupContext.TRAVEL)
        )
    }

    @Test
    fun `classifies everyday lifestyle and shopping subcategories added in 2089`() {
        assertEquals(
            CategorySuggestion(ExpenseCategory.SHOPPING, ExpenseSubcategory.PHARMACY_HEALTH),
            classifier.classify("farmacia", GroupContext.TRAVEL)
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.SHOPPING, ExpenseSubcategory.PHARMACY_HEALTH),
            classifier.classify("farmacia", GroupContext.EVERYDAY)
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.SHOPPING, ExpenseSubcategory.TOBACCO_VAPING),
            classifier.classify("estanco tabaco", GroupContext.EVERYDAY)
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.SHOPPING, ExpenseSubcategory.HOBBIES),
            classifier.classify("libreria comic", GroupContext.EVERYDAY)
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.HEALTH_PERSONAL_CARE, ExpenseSubcategory.PERSONAL_CARE),
            classifier.classify("champu y desodorante", GroupContext.EVERYDAY)
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.TRANSPORT, ExpenseSubcategory.CAR_REPAIR_MAINTENANCE),
            classifier.classify("taller mecanico", GroupContext.EVERYDAY)
        )
    }

    @Test
    fun `strips diacritics and ignores case and punctuation`() {
        assertEquals(
            CategorySuggestion(ExpenseCategory.FOOD, ExpenseSubcategory.CAFE_BREAKFAST),
            classifier.classify("Café y Tostada!", GroupContext.TRAVEL)
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.HEALTH_PERSONAL_CARE, ExpenseSubcategory.HAIRDRESSER_BARBER),
            classifier.classify("Peluquería María", GroupContext.EVERYDAY)
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.TRANSPORT, ExpenseSubcategory.FUEL),
            classifier.classify("Gasolinera Repsol", GroupContext.TRAVEL)
        )
    }

    @Test
    fun `respects word boundaries and does not match substrings`() {
        assertNull(classifier.classify("Barcelona", GroupContext.TRAVEL))
        assertNull(classifier.classify("embarque", GroupContext.TRAVEL))
    }

    @Test
    fun `weights ambiguous keywords by GroupContext`() {
        assertEquals(
            CategorySuggestion(ExpenseCategory.SHOPPING, ExpenseSubcategory.SOUVENIRS_GIFTS),
            classifier.classify("regalo", GroupContext.TRAVEL)
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.SHOPPING, ExpenseSubcategory.GIFTS),
            classifier.classify("regalo", GroupContext.EVERYDAY)
        )
    }

    @Test
    fun `allows explicit cross-context matches when title is unambiguous`() {
        assertEquals(
            CategorySuggestion(ExpenseCategory.LODGING, ExpenseSubcategory.HOTEL),
            classifier.classify("3 nights Chengdu hotel", GroupContext.EVERYDAY)
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.HEALTH_PERSONAL_CARE, ExpenseSubcategory.GYM_FITNESS),
            classifier.classify("cuota gimnasio", GroupContext.TRAVEL)
        )
    }

    @Test
    fun `prefers specific phrase rules over weaker single-token matches`() {
        assertEquals(
            CategorySuggestion(ExpenseCategory.TRANSPORT, ExpenseSubcategory.CAR_RENTAL),
            classifier.classify("alquiler coche aeropuerto", GroupContext.EVERYDAY)
        )
        assertEquals(
            CategorySuggestion(ExpenseCategory.INSURANCE, ExpenseSubcategory.TRAVEL_INSURANCE),
            classifier.classify("seguro de viaje", GroupContext.EVERYDAY)
        )
    }

    @Test
    fun `returns null for blank, gibberish, or ambiguous unmatched titles`() {
        assertNull(classifier.classify("", GroupContext.TRAVEL))
        assertNull(classifier.classify("   ", GroupContext.EVERYDAY))
        assertNull(classifier.classify("!!! ---", GroupContext.TRAVEL))
        assertNull(classifier.classify("Shaxi", GroupContext.TRAVEL))
        assertNull(classifier.classify("Juan", GroupContext.EVERYDAY))
        assertNull(classifier.classify("xyz 123", GroupContext.TRAVEL))
    }

    @Test
    fun `handles tied candidates across categories and within the same category`() {
        assertNull(classifier.classify("pizza taxi", GroupContext.TRAVEL))
        assertEquals(
            CategorySuggestion(ExpenseCategory.LODGING, null),
            classifier.classify("hotel hostel", GroupContext.TRAVEL)
        )
    }

    @Test
    fun `every rule in catalog produces a taxonomy-valid CategorySuggestion`() {
        val allRules = CategoryTravelAndCoreRules.rules + CategoryEverydayAndLifestyleRules.rules
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
        }

        val coveredSubcategories = allRules.mapNotNull { it.subcategory }.toSet()
        val expectedSubcategories = ExpenseSubcategory.entries
            .filter { it != ExpenseSubcategory.UNSPECIFIED }
            .toSet()
        assertEquals(expectedSubcategories, coveredSubcategories)
    }
}
