package es.pedrazamiguez.splittrip.konsist

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.w3c.dom.Element

@DisplayName("Everyday Group Copy Rules")
class EverydayGroupCopyTest {

    private val projectRootDir: File by lazy {
        generateSequence(File(".").canonicalFile) { it.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").exists() }
            ?: error("Could not find project root containing settings.gradle.kts")
    }

    private val docBuilder by lazy {
        DocumentBuilderFactory.newInstance().newDocumentBuilder()
    }

    @Test
    @DisplayName("English string resources must not use trip-locked copy outside allowlisted travel entries")
    fun `english string resources do not use trip-locked copy outside allowlisted travel entries`() {
        val failures = mutableListOf<String>()
        for (resDir in getResourceDirectories()) {
            val file = File(resDir, "values/strings.xml")
            if (!file.exists()) continue
            val relativePath = file.relativeTo(projectRootDir).path
            checkEntries(relativePath, parseEntries(file), FORBIDDEN_EN_PATTERNS, failures)
        }
        assertTrue(failures.isEmpty(), "Trip-locked EN copy found:\n" + failures.joinToString("\n"))
    }

    @Test
    @DisplayName("Spanish string resources must not use trip-locked copy outside allowlisted travel entries")
    fun `spanish string resources do not use trip-locked copy outside allowlisted travel entries`() {
        val failures = mutableListOf<String>()
        for (resDir in getResourceDirectories()) {
            val file = File(resDir, "values-es/strings.xml")
            if (!file.exists()) continue
            val relativePath = file.relativeTo(projectRootDir).path
            checkEntries(relativePath, parseEntries(file), FORBIDDEN_ES_PATTERNS, failures)
        }
        assertTrue(failures.isEmpty(), "Trip-locked ES copy found:\n" + failures.joinToString("\n"))
    }

    @Test
    @DisplayName("String resources must not contain em-dashes in user-facing copy")
    fun `string resources do not contain em-dashes`() {
        val failures = mutableListOf<String>()
        getResourceDirectories()
            .flatMap { resDir -> listOf(File(resDir, "values/strings.xml"), File(resDir, "values-es/strings.xml")) }
            .filter { it.exists() }
            .forEach { file -> collectEmDashViolations(file, failures) }
        assertTrue(failures.isEmpty(), "Em-dashes found in string resources:\n" + failures.joinToString("\n"))
    }

    private fun collectEmDashViolations(file: File, failures: MutableList<String>) {
        val relativePath = file.relativeTo(projectRootDir).path
        parseEntries(file).forEach { (key, values) ->
            values
                .filter { it.trim() != "—" && it.contains("—") }
                .forEach { text -> failures.add("$relativePath [$key] contains em-dash: \"$text\"") }
        }
    }

    private fun getResourceDirectories(): List<File> {
        return projectRootDir.walkTopDown()
            .filter { it.isDirectory && it.name == "res" && it.parentFile?.name == "main" }
            .filter { File(it, "values/strings.xml").exists() }
            .toList()
    }

    private fun checkEntries(
        relativePath: String,
        entries: Map<String, List<String>>,
        patterns: List<Regex>,
        failures: MutableList<String>
    ) {
        entries
            .filterKeys { it !in ALLOWLISTED_TRAVEL_KEYS }
            .forEach { (key, values) ->
                collectViolations(relativePath, key, values, patterns, failures)
            }
    }

    private fun collectViolations(
        relativePath: String,
        key: String,
        values: List<String>,
        patterns: List<Regex>,
        failures: MutableList<String>
    ) {
        for (text in values) {
            patterns
                .mapNotNull { it.find(text) }
                .forEach { match -> failures.add("$relativePath [$key] matched '${match.value}': \"$text\"") }
        }
    }

    private fun parseEntries(file: File): Map<String, List<String>> {
        val doc = docBuilder.parse(file)
        val result = linkedMapOf<String, MutableList<String>>()

        val stringNodes = doc.documentElement.getElementsByTagName("string")
        for (i in 0 until stringNodes.length) {
            val element = stringNodes.item(i) as Element
            val name = element.getAttribute("name")
            if (name.isNotEmpty()) {
                result.getOrPut(name) { mutableListOf() }.add(element.textContent ?: "")
            }
        }

        val pluralNodes = doc.documentElement.getElementsByTagName("plurals")
        for (i in 0 until pluralNodes.length) {
            val element = pluralNodes.item(i) as Element
            val name = element.getAttribute("name")
            if (name.isNotEmpty()) {
                val itemNodes = element.getElementsByTagName("item")
                val texts = result.getOrPut(name) { mutableListOf() }
                for (j in 0 until itemNodes.length) {
                    val item = itemNodes.item(j) as Element
                    texts.add(item.textContent ?: "")
                }
            }
        }

        return result
    }

    companion object {
        private val FORBIDDEN_EN_PATTERNS = listOf(
            Regex("\\btrips?\\b", RegexOption.IGNORE_CASE),
            Regex("\\btravell?ers?\\b", RegexOption.IGNORE_CASE),
            Regex("\\btravel companions?\\b", RegexOption.IGNORE_CASE),
            Regex("\\btravel group\\b", RegexOption.IGNORE_CASE),
            Regex("\\btravel style\\b", RegexOption.IGNORE_CASE),
            Regex("\\bexpedition\\b", RegexOption.IGNORE_CASE),
            Regex("\\bexplorer\\b", RegexOption.IGNORE_CASE)
        )

        private val FORBIDDEN_ES_PATTERNS = listOf(
            Regex("\\bviajes?\\b", RegexOption.IGNORE_CASE),
            Regex("\\bviajer[oa]s?\\b", RegexOption.IGNORE_CASE),
            Regex("\\bcompañer[oa]s de viaje\\b", RegexOption.IGNORE_CASE),
            Regex("\\bexpedición\\b", RegexOption.IGNORE_CASE),
            Regex("\\bexplorador(?:a|es|as)?\\b", RegexOption.IGNORE_CASE)
        )

        private val ALLOWLISTED_TRAVEL_KEYS: Set<String> = setOf(
            "expense_subcategory_travel_insurance",
            "expense_subcategory_vacation_rental",
            "expense_subcategory_tour_excursion",
            "expense_subcategory_adventure_sports",
            "expense_subcategory_international_flight",
            "expense_subcategory_domestic_flight",
            "expense_subcategory_hotel",
            "faq_answer_1",
            "onboarding_step_trips_description",
            "pro_preview_unlimited_groups_description",
            "subscriptions_tier_free_description"
        )
    }
}
