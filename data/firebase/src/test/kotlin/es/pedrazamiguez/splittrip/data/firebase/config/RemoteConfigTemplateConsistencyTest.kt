package es.pedrazamiguez.splittrip.data.firebase.config

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import es.pedrazamiguez.splittrip.data.firebase.repository.FirebaseAppConfigRepository
import java.io.File
import java.nio.charset.StandardCharsets
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.w3c.dom.Element

class RemoteConfigTemplateConsistencyTest {

    private lateinit var xmlDefaults: Map<String, String>
    private lateinit var templateParameters: Map<String, ParameterDefinition>
    private lateinit var groupDescriptions: Map<String, String>

    data class ParameterDefinition(
        val description: String?,
        val valueType: String?,
        val defaultValue: String?
    )

    @BeforeEach
    fun setUp() {
        val rootDir = resolveProjectRoot()
        val xmlFile = File(rootDir, "data/firebase/src/main/res/xml/remote_config_defaults.xml")
        val jsonFile = File(rootDir, "firebase/remoteconfig.template.json")

        assertTrue(xmlFile.exists(), "XML defaults file not found: ${xmlFile.absolutePath}")
        assertTrue(jsonFile.exists(), "Template JSON file not found: ${jsonFile.absolutePath}")

        xmlDefaults = parseXmlDefaults(xmlFile)
        val (params, groups) = parseTemplate(jsonFile)
        templateParameters = params
        groupDescriptions = groups
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
            when (param.valueType) {
                "BOOLEAN" -> {
                    assertTrue(
                        value.equals("true", ignoreCase = true) || value.equals("false", ignoreCase = true),
                        "Parameter '$key' is BOOLEAN but value is '$value'"
                    )
                }
                "NUMBER" -> {
                    val isNumber = value?.toDoubleOrNull() != null
                    assertTrue(isNumber, "Parameter '$key' is NUMBER but value '$value' is not numeric")
                }
                "JSON" -> {
                    val isJson = try {
                        JsonParser.parseString(value)
                        true
                    } catch (_: Exception) {
                        false
                    }
                    assertTrue(isJson, "Parameter '$key' is JSON but value is not valid JSON")
                }
                "STRING" -> {
                    assertTrue(value != null, "Parameter '$key' is STRING but value is null")
                }
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
            avatarUrl?.contains("/o/public%2Favatars%2Fdeveloper.webp?alt=media") == true,
            "avatar_url should target /o/public%2Favatars%2Fdeveloper.webp?alt=media"
        )
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

    private fun parseTemplate(jsonFile: File): Pair<Map<String, ParameterDefinition>, Map<String, String>> {
        val jsonElement = JsonParser.parseString(jsonFile.readText(StandardCharsets.UTF_8))
        val rootObj = jsonElement.asJsonObject

        val parameters = mutableMapOf<String, ParameterDefinition>()
        parameters.putAll(extractParameters(rootObj.getAsJsonObject("parameters")))

        val (groupParams, groupDescriptions) = extractGroups(rootObj.getAsJsonObject("parameterGroups"))
        parameters.putAll(groupParams)

        return Pair(parameters, groupDescriptions)
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
        val description = if (obj.has("description") && !obj.get("description").isJsonNull) {
            obj.get("description").asString
        } else {
            null
        }

        val valueType = if (obj.has("valueType") && !obj.get("valueType").isJsonNull) {
            obj.get("valueType").asString
        } else {
            null
        }

        val defaultValue = if (obj.has("defaultValue") && obj.get("defaultValue").isJsonObject) {
            val defaultValObj = obj.getAsJsonObject("defaultValue")
            if (defaultValObj.has("value") && !defaultValObj.get("value").isJsonNull) {
                defaultValObj.get("value").asString
            } else {
                null
            }
        } else {
            null
        }

        return ParameterDefinition(description, valueType, defaultValue)
    }
}
