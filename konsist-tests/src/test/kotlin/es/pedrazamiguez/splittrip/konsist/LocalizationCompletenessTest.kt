package es.pedrazamiguez.splittrip.konsist

import java.io.File
import java.util.regex.Pattern
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.w3c.dom.Element

@DisplayName("Localization Completeness Rules")
class LocalizationCompletenessTest {

    private val projectRootDir: File by lazy {
        generateSequence(File(".").canonicalFile) { it.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").exists() }
            ?: error("Could not find project root containing settings.gradle.kts")
    }

    private val formatSpecifierPattern = Pattern.compile(
        "%%|%(?:[0-9]+\\$)?[-#+ 0,(<]?[0-9]*(?:\\.[0-9]+)?[sSdDfF]"
    )

    private val docBuilder by lazy {
        DocumentBuilderFactory.newInstance().newDocumentBuilder()
    }

    private fun getResourceDirectories(): List<File> {
        return projectRootDir.walkTopDown()
            .filter { it.isDirectory && it.name == "res" && it.parentFile?.name == "main" }
            .filter { File(it, "values/strings.xml").exists() }
            .toList()
    }

    @Test
    @DisplayName("String resources must maintain 100% key parity across English, Spanish, and Andalusian")
    fun `string resources must have key parity across all locales`() {
        val failures = mutableListOf<String>()
        for (resDir in getResourceDirectories()) {
            validateStringsDirectory(resDir, failures)
        }
        assertTrue(failures.isEmpty(), "Key parity failures found:\n" + failures.joinToString("\n"))
    }

    private fun validateStringsDirectory(resDir: File, failures: MutableList<String>) {
        val esFile = File(resDir, "values-es/strings.xml")
        val dirName = resDir.relativeTo(projectRootDir).path
        val enParsed = parseStringsFile(File(resDir, "values/strings.xml"))

        if (!esFile.exists()) {
            val translatable = enParsed.strings.filter { it.value.isTranslatable }
            if (translatable.isNotEmpty()) {
                failures.add("$dirName: Missing values-es for translatable keys: ${translatable.keys}")
            }
            return
        }

        val esStrings = parseStringsFile(esFile).strings
        val anFile = File(resDir, "values-es-rAN/strings.xml")
        val anStrings = if (anFile.exists()) parseStringsFile(anFile).strings else emptyMap()

        validateKeyParity(dirName, enParsed.strings, esStrings, anStrings, anFile.exists(), failures)
    }

    private fun validateKeyParity(
        dirName: String,
        enStrings: Map<String, StringEntry>,
        esStrings: Map<String, StringEntry>,
        anStrings: Map<String, StringEntry>,
        anFileExists: Boolean,
        failures: MutableList<String>
    ) {
        val enTranslatableKeys = enStrings.filter { it.value.isTranslatable }.keys
        val esKeys = esStrings.keys
        val anKeys = anStrings.keys

        val missingInEs = enTranslatableKeys - esKeys
        if (missingInEs.isNotEmpty()) {
            failures.add("$dirName: Missing in values-es: $missingInEs")
        }

        val missingInEn = esKeys - enStrings.keys
        if (missingInEn.isNotEmpty()) {
            failures.add("$dirName: Orphaned in values-es (missing in values): $missingInEn")
        }

        if (!anFileExists) {
            failures.add("$dirName: Missing values-es-rAN directory")
            return
        }

        val missingInAn = esKeys - anKeys
        if (missingInAn.isNotEmpty()) {
            failures.add("$dirName: Missing in values-es-rAN: $missingInAn")
        }
        val orphanedInAn = anKeys - esKeys
        if (orphanedInAn.isNotEmpty()) {
            failures.add("$dirName: Orphaned in values-es-rAN (missing in values-es): $orphanedInAn")
        }
    }

    @Test
    @DisplayName("Plural resources must maintain 100% key and quantity parity across all locales")
    fun `plural resources must have parity across all locales`() {
        val failures = mutableListOf<String>()
        for (resDir in getResourceDirectories()) {
            validatePluralsDirectory(resDir, failures)
        }
        assertTrue(failures.isEmpty(), "Plural parity failures found:\n" + failures.joinToString("\n"))
    }

    private fun validatePluralsDirectory(resDir: File, failures: MutableList<String>) {
        val esFile = File(resDir, "values-es/strings.xml")
        if (!esFile.exists()) return

        val dirName = resDir.relativeTo(projectRootDir).path
        val enPlurals = parseStringsFile(File(resDir, "values/strings.xml")).plurals
        val esPlurals = parseStringsFile(esFile).plurals
        val anFile = File(resDir, "values-es-rAN/strings.xml")
        val anPlurals = if (anFile.exists()) parseStringsFile(anFile).plurals else emptyMap()

        validatePluralKeys(dirName, enPlurals, esPlurals, anPlurals, anFile.exists(), failures)
        validatePluralQuantities(dirName, enPlurals, esPlurals, anPlurals, failures)
    }

    private fun validatePluralKeys(
        dirName: String,
        enPlurals: Map<String, PluralEntry>,
        esPlurals: Map<String, PluralEntry>,
        anPlurals: Map<String, PluralEntry>,
        anFileExists: Boolean,
        failures: MutableList<String>
    ) {
        val enKeys = enPlurals.keys
        val esKeys = esPlurals.keys
        val anKeys = anPlurals.keys

        val missingInEs = enKeys - esKeys
        if (missingInEs.isNotEmpty()) {
            failures.add("$dirName: Plurals missing in values-es: $missingInEs")
        }
        val missingInEn = esKeys - enKeys
        if (missingInEn.isNotEmpty()) {
            failures.add("$dirName: Plurals orphaned in values-es: $missingInEn")
        }

        if (anFileExists) {
            val missingInAn = esKeys - anKeys
            if (missingInAn.isNotEmpty()) {
                failures.add("$dirName: Plurals missing in values-es-rAN: $missingInAn")
            }
        }
    }

    private fun validatePluralQuantities(
        dirName: String,
        enPlurals: Map<String, PluralEntry>,
        esPlurals: Map<String, PluralEntry>,
        anPlurals: Map<String, PluralEntry>,
        failures: MutableList<String>
    ) {
        for ((name, enPlural) in enPlurals) {
            val esPlural = esPlurals[name]
            if (esPlural != null) {
                validateSinglePluralQuantities(
                    dirName = dirName,
                    name = name,
                    enPlural = enPlural,
                    esPlural = esPlural,
                    anPlural = anPlurals[name],
                    failures = failures
                )
            }
        }
    }

    private fun validateSinglePluralQuantities(
        dirName: String,
        name: String,
        enPlural: PluralEntry,
        esPlural: PluralEntry,
        anPlural: PluralEntry?,
        failures: MutableList<String>
    ) {
        val missingQuantitiesInEs = enPlural.items.keys - esPlural.items.keys
        if (missingQuantitiesInEs.isNotEmpty()) {
            failures.add("$dirName: Plural '$name' missing quantities in values-es: $missingQuantitiesInEs")
        }
        if (anPlural != null) {
            val missingQuantitiesInAn = esPlural.items.keys - anPlural.items.keys
            if (missingQuantitiesInAn.isNotEmpty()) {
                failures.add(
                    "$dirName: Plural '$name' missing quantities in values-es-rAN: $missingQuantitiesInAn"
                )
            }
        }
    }

    @Test
    @DisplayName("Format specifiers in strings and plurals must match across English, Spanish, and Andalusian")
    fun `format specifiers must match across all locales`() {
        val failures = mutableListOf<String>()
        for (resDir in getResourceDirectories()) {
            validateSpecifiersDirectory(resDir, failures)
        }
        assertTrue(failures.isEmpty(), "Format specifier mismatches found:\n" + failures.joinToString("\n"))
    }

    private fun validateSpecifiersDirectory(resDir: File, failures: MutableList<String>) {
        val esFile = File(resDir, "values-es/strings.xml")
        if (!esFile.exists()) return

        val dirName = resDir.relativeTo(projectRootDir).path
        val enParsed = parseStringsFile(File(resDir, "values/strings.xml"))
        val esParsed = parseStringsFile(esFile)
        val anFile = File(resDir, "values-es-rAN/strings.xml")
        val anParsed = if (anFile.exists()) parseStringsFile(anFile) else ParsedStrings(emptyMap(), emptyMap())

        validateStringSpecifiers(dirName, enParsed.strings, esParsed.strings, anParsed.strings, failures)
        validateAllPluralSpecifiers(dirName, enParsed.plurals, esParsed.plurals, anParsed.plurals, failures)
    }

    private fun validateStringSpecifiers(
        dirName: String,
        enStrings: Map<String, StringEntry>,
        esStrings: Map<String, StringEntry>,
        anStrings: Map<String, StringEntry>,
        failures: MutableList<String>
    ) {
        for ((key, enString) in enStrings) {
            if (enString.isFormatted && enString.isTranslatable) {
                val esString = esStrings[key]
                if (esString != null) {
                    validateSingleStringSpecifier(
                        dirName = dirName,
                        key = key,
                        enString = enString,
                        esString = esString,
                        anString = anStrings[key],
                        failures = failures
                    )
                }
            }
        }
    }

    private fun validateSingleStringSpecifier(
        dirName: String,
        key: String,
        enString: StringEntry,
        esString: StringEntry,
        anString: StringEntry?,
        failures: MutableList<String>
    ) {
        val enSpecifiers = extractSpecifiers(enString.text)
        val esSpecifiers = extractSpecifiers(esString.text)

        if (enSpecifiers != esSpecifiers) {
            failures.add(
                "$dirName: String '$key' specifier mismatch between EN ($enSpecifiers) and ES ($esSpecifiers)"
            )
        }
        if (anString != null) {
            val anSpecifiers = extractSpecifiers(anString.text)
            if (esSpecifiers != anSpecifiers) {
                failures.add(
                    "$dirName: String '$key' specifier mismatch between ES ($esSpecifiers) and AN ($anSpecifiers)"
                )
            }
        }
    }

    private fun validateAllPluralSpecifiers(
        dirName: String,
        enPlurals: Map<String, PluralEntry>,
        esPlurals: Map<String, PluralEntry>,
        anPlurals: Map<String, PluralEntry>,
        failures: MutableList<String>
    ) {
        for ((key, enPlural) in enPlurals) {
            val esPlural = esPlurals[key]
            if (esPlural != null) {
                validatePluralItemSpecifiers(
                    dirName = dirName,
                    pluralKey = key,
                    enPlural = enPlural,
                    esPlural = esPlural,
                    anPlural = anPlurals[key],
                    failures = failures
                )
            }
        }
    }

    private fun validatePluralItemSpecifiers(
        dirName: String,
        pluralKey: String,
        enPlural: PluralEntry,
        esPlural: PluralEntry,
        anPlural: PluralEntry?,
        failures: MutableList<String>
    ) {
        for ((quantity, enItemText) in enPlural.items) {
            val esItemText = esPlural.items[quantity]
            if (esItemText != null) {
                checkPluralQuantitySpecifiers(
                    dirName = dirName,
                    pluralKey = pluralKey,
                    quantity = quantity,
                    enItemText = enItemText,
                    esItemText = esItemText,
                    anItemText = anPlural?.items?.get(quantity),
                    failures = failures
                )
            }
        }
    }

    private fun checkPluralQuantitySpecifiers(
        dirName: String,
        pluralKey: String,
        quantity: String,
        enItemText: String,
        esItemText: String,
        anItemText: String?,
        failures: MutableList<String>
    ) {
        val enSpecifiers = extractSpecifiers(enItemText)
        val esSpecifiers = extractSpecifiers(esItemText)

        if (enSpecifiers != esSpecifiers) {
            failures.add(
                "$dirName: Plural '$pluralKey' quantity '$quantity' specifier mismatch between EN and ES"
            )
        }
        if (anItemText != null) {
            val anSpecifiers = extractSpecifiers(anItemText)
            if (esSpecifiers != anSpecifiers) {
                failures.add(
                    "$dirName: Plural '$pluralKey' quantity '$quantity' specifier mismatch between ES and AN"
                )
            }
        }
    }

    private fun extractSpecifiers(text: String): List<String> {
        val matcher = formatSpecifierPattern.matcher(text)
        val specifiers = mutableListOf<String>()
        while (matcher.find()) {
            specifiers.add(matcher.group())
        }
        return specifiers.sorted()
    }

    private fun parseStringsFile(file: File): ParsedStrings {
        val doc = docBuilder.parse(file)
        val stringNodes = doc.documentElement.getElementsByTagName("string")
        val strings = mutableMapOf<String, StringEntry>()

        for (i in 0 until stringNodes.length) {
            val element = stringNodes.item(i) as Element
            val name = element.getAttribute("name")
            if (name.isNotEmpty()) {
                val isTranslatable = element.getAttribute("translatable") != "false"
                val isFormatted = element.getAttribute("formatted") != "false"
                val text = element.textContent ?: ""
                strings[name] = StringEntry(name, text, isTranslatable, isFormatted)
            }
        }

        val pluralNodes = doc.documentElement.getElementsByTagName("plurals")
        val plurals = mutableMapOf<String, PluralEntry>()

        for (i in 0 until pluralNodes.length) {
            val element = pluralNodes.item(i) as Element
            val name = element.getAttribute("name")
            if (name.isNotEmpty()) {
                val isTranslatable = element.getAttribute("translatable") != "false"
                val items = mutableMapOf<String, String>()
                val itemNodes = element.getElementsByTagName("item")
                for (j in 0 until itemNodes.length) {
                    val itemElement = itemNodes.item(j) as Element
                    val quantity = itemElement.getAttribute("quantity")
                    items[quantity] = itemElement.textContent ?: ""
                }
                plurals[name] = PluralEntry(name, items, isTranslatable)
            }
        }

        return ParsedStrings(strings, plurals)
    }

    private data class StringEntry(
        val name: String,
        val text: String,
        val isTranslatable: Boolean,
        val isFormatted: Boolean
    )

    private data class PluralEntry(
        val name: String,
        val items: Map<String, String>,
        val isTranslatable: Boolean
    )

    private data class ParsedStrings(
        val strings: Map<String, StringEntry>,
        val plurals: Map<String, PluralEntry>
    )
}
