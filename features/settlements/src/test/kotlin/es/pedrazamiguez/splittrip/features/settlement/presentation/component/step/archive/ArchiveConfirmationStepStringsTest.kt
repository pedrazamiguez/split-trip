package es.pedrazamiguez.splittrip.features.settlement.presentation.component.step.archive

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.w3c.dom.Element

class ArchiveConfirmationStepStringsTest {

    @Test
    fun `archive_wizard_confirm_impact_title is distinct from archive_wizard_confirm_title in all locales`() {
        val locales = listOf("values", "values-es", "values-es-rAN")
        locales.forEach { qualifier ->
            val strings = loadStrings("src/main/res/$qualifier/strings.xml")
            val confirmTitle = strings["archive_wizard_confirm_title"]
            val impactTitle = strings["archive_wizard_confirm_impact_title"]

            assertNotNull(confirmTitle, "Missing archive_wizard_confirm_title in $qualifier")
            assertNotNull(impactTitle, "Missing archive_wizard_confirm_impact_title in $qualifier")
            assertNotEquals(
                confirmTitle,
                impactTitle,
                "Header and SectionCard titles must be distinct in $qualifier"
            )
        }
    }

    @Test
    fun `settlement_overview_close_trip matches canonical End Trip copy in EN and ES`() {
        val enStrings = loadStrings("src/main/res/values/strings.xml")
        val esStrings = loadStrings("src/main/res/values-es/strings.xml")

        assertEquals("End Trip", enStrings["settlement_overview_close_trip"])
        assertEquals("Finalizar viaje", esStrings["settlement_overview_close_trip"])
    }

    @Test
    fun `trip finalisation copy does not leak archive, close, or inactive jargon`() {
        val keysToCheck = listOf(
            "settlement_overview_subtitle",
            "settlement_overview_close_trip",
            "settlement_overview_closing_trip",
            "settlement_overview_trip_closed",
            "settlement_overview_error_archive",
            "archive_wizard_summary_subtitle",
            "archive_wizard_summary_ready_title",
            "archive_wizard_summary_ready_desc",
            "archive_wizard_summary_unresolved_desc",
            "archive_wizard_confirm_warning_banner",
            "archive_wizard_confirm_desc"
        )
        val forbiddenEnTerms = listOf("archive", "archiving", "close", "closing", "closed", "inactive")
        val forbiddenEsTerms = listOf("archivar", "cerrar", "cerrando", "cerrado", "inactivo")

        val enStrings = loadStrings("src/main/res/values/strings.xml")
        val esStrings = loadStrings("src/main/res/values-es/strings.xml")

        keysToCheck.forEach { key ->
            val enValue = requireNotNull(enStrings[key]) { "Missing EN key: $key" }.lowercase()
            val esValue = requireNotNull(esStrings[key]) { "Missing ES key: $key" }.lowercase()

            forbiddenEnTerms.forEach { term ->
                assertFalse(enValue.contains(term), "EN '$key' contains forbidden term '$term': $enValue")
            }
            forbiddenEsTerms.forEach { term ->
                assertFalse(esValue.contains(term), "ES '$key' contains forbidden term '$term': $esValue")
            }
        }
    }

    @Test
    fun `groups module does not contain duplicate archive_wizard strings`() {
        val groupsEn = loadStrings("../groups/src/main/res/values/strings.xml")
        val groupsEs = loadStrings("../groups/src/main/res/values-es/strings.xml")

        assertTrue(groupsEn.keys.none { it.startsWith("archive_wizard_") })
        assertTrue(groupsEs.keys.none { it.startsWith("archive_wizard_") })
    }

    private fun loadStrings(relativePath: String): Map<String, String> {
        val file = resolveResFile(relativePath)
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = document.getElementsByTagName("string")
        val result = mutableMapOf<String, String>()
        for (i in 0 until nodes.length) {
            val element = nodes.item(i) as Element
            result[element.getAttribute("name")] = element.textContent
        }
        return result
    }

    private fun resolveResFile(relativePath: String): File {
        val moduleRelative = File(relativePath)
        if (moduleRelative.exists()) return moduleRelative

        val normalized = relativePath.removePrefix("../groups/")
        val rootRelative = if (relativePath.startsWith("../groups/")) {
            File("features/groups/$normalized")
        } else {
            File("features/settlements/$relativePath")
        }
        check(rootRelative.exists()) { "Could not locate resource file: $relativePath" }
        return rootRelative
    }
}
