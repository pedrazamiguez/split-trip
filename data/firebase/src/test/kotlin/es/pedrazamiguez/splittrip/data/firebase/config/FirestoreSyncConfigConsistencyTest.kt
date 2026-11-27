package es.pedrazamiguez.splittrip.data.firebase.config

import com.google.gson.JsonParser
import java.io.File
import java.nio.charset.StandardCharsets
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FirestoreSyncConfigConsistencyTest {

    private val syncedSubcollections = listOf(
        "expenses",
        "contributions",
        "cash_withdrawals",
        "cash_transfers",
        "subunits",
        "settlements"
    )

    @Test
    fun `firebase json configures firestore rules and indexes files`() {
        val rootDir = resolveProjectRoot()
        val firebaseJsonFile = File(rootDir, "firebase.json")
        val rulesFile = File(rootDir, "firestore.rules")
        val indexesFile = File(rootDir, "firestore.indexes.json")

        assertTrue(firebaseJsonFile.exists())
        assertTrue(rulesFile.exists())
        assertTrue(indexesFile.exists())

        val rootObj = JsonParser.parseString(firebaseJsonFile.readText(StandardCharsets.UTF_8)).asJsonObject
        assertTrue(rootObj.has("firestore"))
        val firestoreObj = rootObj.getAsJsonObject("firestore")
        assertEquals("firestore.rules", firestoreObj.get("rules").asString)
        assertEquals("firestore.indexes.json", firestoreObj.get("indexes").asString)
    }

    @Test
    fun `firestore rules enforces query limit 500 on list for all 6 synced subcollections`() {
        val content = readFileFromRoot("firestore.rules")

        assertTrue(content.contains("function hasValidQueryLimit(maxLimit)"))
        assertTrue(content.contains("request.query.limit != null"))
        assertTrue(content.contains("request.query.limit <= maxLimit"))

        syncedSubcollections.forEach { subcollection ->
            val block = extractSubcollectionMatchBlock(content, subcollection)
            assertTrue(block.contains("allow get: if isAuthenticated();"), "Missing allow get in $subcollection")
            assertTrue(
                block.contains("allow list: if isAuthenticated() && hasValidQueryLimit(500);"),
                "Missing bounded allow list in $subcollection"
            )
            assertFalse(
                block.contains("allow read: if isAuthenticated();"),
                "Unbounded allow read must be removed from $subcollection"
            )
        }
    }

    @Test
    fun `firestore rules enforces sync metadata and payload validation without adding per-doc membership get checks`() {
        val content = readFileFromRoot("firestore.rules")

        assertTrue(content.contains("function isValidOptionalServerTimestamp(fieldName)"))
        assertTrue(content.contains("function hasValidSyncMetadata(maxKeys)"))
        assertTrue(content.contains("request.resource.data[fieldName] == request.time"))
        assertTrue(content.contains("isValidOptionalServerTimestamp('serverUpdatedAt')"))
        assertTrue(content.contains("isValidExpensePayload()"))
        assertTrue(content.contains("isValidContributionPayload()"))
        assertTrue(content.contains("isValidCashWithdrawalPayload()"))
        assertTrue(content.contains("isValidCashTransferPayload()"))
        assertTrue(content.contains("isValidSubunitPayload()"))
        assertTrue(content.contains("isValidSettlementPayload()"))

        syncedSubcollections.forEach { subcollection ->
            val block = extractSubcollectionMatchBlock(content, subcollection)
            assertFalse(
                block.contains("isMemberOrPending"),
                "Must not add per-doc membership get() checks on $subcollection"
            )
        }
    }

    @Test
    fun `firestore indexes configures expireAt TTL policy with empty indexes array for all 6 synced subcollections`() {
        val content = readFileFromRoot("firestore.indexes.json")
        val rootObj = JsonParser.parseString(content).asJsonObject
        val fieldOverrides = rootObj.getAsJsonArray("fieldOverrides")

        val ttlEntriesByCollection = fieldOverrides
            .map { it.asJsonObject }
            .filter { it.get("fieldPath")?.asString == "expireAt" }
            .associateBy { it.get("collectionGroup").asString }

        syncedSubcollections.forEach { collection ->
            val entry = ttlEntriesByCollection[collection]
            assertTrue(entry != null, "Missing expireAt fieldOverride for $collection")
            assertEquals(true, entry!!.get("ttl")?.asBoolean, "ttl must be true for $collection")
            assertEquals(0, entry.getAsJsonArray("indexes").size(), "indexes must be empty for TTL on $collection")
        }
    }

    private fun extractSubcollectionMatchBlock(rulesContent: String, subcollection: String): String {
        val marker = "match /$subcollection/"
        val startIdx = rulesContent.indexOf(marker)
        assertTrue(startIdx >= 0, "Could not find $marker in firestore.rules")
        val spaceBraceIdx = rulesContent.indexOf(" {", startIdx)
        assertTrue(spaceBraceIdx >= 0, "Could not find block brace for $marker")
        val openBraceIdx = spaceBraceIdx + 1
        var depth = 0
        for (i in openBraceIdx until rulesContent.length) {
            when (rulesContent[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return rulesContent.substring(startIdx, i + 1)
                    }
                }
            }
        }
        error("Unclosed block for $marker")
    }

    private fun readFileFromRoot(relativePath: String): String {
        val file = File(resolveProjectRoot(), relativePath)
        assertTrue(file.exists(), "File not found: ${file.absolutePath}")
        return file.readText(StandardCharsets.UTF_8)
    }

    private fun resolveProjectRoot(): File {
        var dir = File(System.getProperty("user.dir") ?: ".")
        while (!File(dir, "settings.gradle.kts").exists()) {
            dir = dir.parentFile ?: error("Could not find project root containing settings.gradle.kts")
        }
        return dir
    }
}
