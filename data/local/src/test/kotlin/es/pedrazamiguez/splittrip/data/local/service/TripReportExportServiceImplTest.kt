package es.pedrazamiguez.splittrip.data.local.service

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import es.pedrazamiguez.splittrip.data.local.service.export.TripReportCsvFormatter
import es.pedrazamiguez.splittrip.data.local.service.export.TripReportPdfRenderer
import es.pedrazamiguez.splittrip.domain.model.report.TripReportBrandFooter
import es.pedrazamiguez.splittrip.domain.model.report.TripReportCell
import es.pedrazamiguez.splittrip.domain.model.report.TripReportCsvSection
import es.pedrazamiguez.splittrip.domain.model.report.TripReportExportPayload
import es.pedrazamiguez.splittrip.domain.model.report.TripReportFormat
import es.pedrazamiguez.splittrip.domain.model.report.TripReportMetricCard
import es.pedrazamiguez.splittrip.domain.model.report.TripReportTableSection
import es.pedrazamiguez.splittrip.domain.model.report.TripReportTone
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TripReportExportServiceImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var tempCacheDir: File
    private lateinit var contentResolver: ContentResolver
    private lateinit var context: Context
    private lateinit var pdfRenderer: TripReportPdfRenderer
    private lateinit var sourceUri: Uri
    private lateinit var destinationUri: Uri
    private lateinit var service: TripReportExportServiceImpl

    @BeforeEach
    fun setUp() {
        tempCacheDir = Files.createTempDirectory("export-service-test").toFile()
        contentResolver = mockk()
        sourceUri = mockk()
        destinationUri = mockk()
        context = mockk {
            every { cacheDir } returns tempCacheDir
            every { packageName } returns "es.pedrazamiguez.splittrip"
            every { contentResolver } returns this@TripReportExportServiceImplTest.contentResolver
        }
        pdfRenderer = mockk(relaxed = true)
        service = TripReportExportServiceImpl(
            context = context,
            csvFormatter = TripReportCsvFormatter(),
            pdfRenderer = pdfRenderer,
            ioDispatcher = testDispatcher,
            uriResolver = { _, file ->
                "content://es.pedrazamiguez.splittrip.fileprovider/exports/${file.name}"
            },
            uriParser = { raw ->
                when (raw) {
                    SOURCE_URI_STRING -> sourceUri
                    DEST_URI_STRING -> destinationUri
                    else -> mockk()
                }
            }
        )
    }

    @AfterEach
    fun tearDown() {
        tempCacheDir.deleteRecursively()
    }

    @Test
    fun `export with CSV format writes UTF-8 csv file to cache exports directory and returns FileProvider URI`() =
        runTest(testDispatcher) {
            val payload = createSamplePayload(expenseRowCount = 2)

            val result = service.export(payload, TripReportFormat.CSV)

            val expectedFile = File(File(tempCacheDir, "exports"), "splittrip-summary-flat-2026-11-20.csv")
            assertTrue(expectedFile.exists())
            val content = expectedFile.readText(Charsets.UTF_8)
            assertTrue(content.contains("ReportTitle,Group summary & itemised ledger\r\n"))
            assertTrue(content.contains("Coffee,3.50,Item 1\r\n"))
            assertEquals("text/csv", result.mimeType)
            assertEquals(expectedFile.name, result.fileName)
            assertEquals(
                "content://es.pedrazamiguez.splittrip.fileprovider/exports/${expectedFile.name}",
                result.contentUri
            )
        }

    @Test
    fun `export with PDF format invokes pdfRenderer and returns application pdf FileProvider URI`() =
        runTest(testDispatcher) {
            val payload = createSamplePayload(expenseRowCount = 1)
            every { pdfRenderer.render(payload, any()) } answers {
                val file = secondArg<File>()
                file.writeBytes(byteArrayOf(0x25, 0x50, 0x44, 0x46))
            }

            val result = service.export(payload, TripReportFormat.PDF)

            val expectedFile = File(File(tempCacheDir, "exports"), "splittrip-summary-flat-2026-11-20.pdf")
            verify(exactly = 1) { pdfRenderer.render(payload, expectedFile) }
            assertEquals("application/pdf", result.mimeType)
            assertEquals(expectedFile.name, result.fileName)
            assertEquals(
                "content://es.pedrazamiguez.splittrip.fileprovider/exports/${expectedFile.name}",
                result.contentUri
            )
        }

    @Test
    fun `saveToUri copies bytes from sourceContentUri to destinationUri and flushes stream`() =
        runTest(testDispatcher) {
            val expectedBytes = "pdf-or-csv-content".toByteArray(Charsets.UTF_8)
            val outputStream = ByteArrayOutputStream()
            every { contentResolver.openInputStream(sourceUri) } returns ByteArrayInputStream(expectedBytes)
            every { contentResolver.openOutputStream(destinationUri) } returns outputStream

            service.saveToUri(SOURCE_URI_STRING, DEST_URI_STRING)

            assertArrayEquals(expectedBytes, outputStream.toByteArray())
        }

    @Test
    fun `saveToUri throws IOException when source stream is null`() =
        runTest(testDispatcher) {
            every { contentResolver.openInputStream(sourceUri) } returns null

            try {
                service.saveToUri(SOURCE_URI_STRING, DEST_URI_STRING)
                fail("Expected IOException")
            } catch (_: IOException) {
                // Expected
            }
            verify(exactly = 0) { contentResolver.openOutputStream(any()) }
        }

    @Test
    fun `saveToUri throws IOException when destination stream is null`() =
        runTest(testDispatcher) {
            every { contentResolver.openInputStream(sourceUri) } returns ByteArrayInputStream(byteArrayOf(1, 2, 3))
            every { contentResolver.openOutputStream(destinationUri) } returns null

            try {
                service.saveToUri(SOURCE_URI_STRING, DEST_URI_STRING)
                fail("Expected IOException")
            } catch (_: IOException) {
                // Expected
            }
        }

    private fun createSamplePayload(expenseRowCount: Int): TripReportExportPayload {
        val expenseRows = (1..expenseRowCount).map { index ->
            listOf("Coffee", "3.50", "Item $index")
        }
        val expenseCells = expenseRows.map { row -> row.map { TripReportCell(it) } }
        val emptyTable = TripReportTableSection(title = "Empty", headers = listOf("Col"), rows = emptyList())
        return TripReportExportPayload(
            fileBaseName = "splittrip-summary-flat-2026-11-20",
            reportTitle = "Group summary & itemised ledger",
            groupName = "Shared Flat",
            groupDescription = "Utilities & groceries",
            periodLabel = "All history",
            generatedAtLabel = "Generated 20 Nov 2026",
            baseCurrency = "EUR",
            summarySectionTitle = "Group overview",
            summaryMetrics = listOf(TripReportMetricCard("Total contributed", "€100.00", TripReportTone.POSITIVE)),
            memberBalancesSection = emptyTable,
            subunitBalancesSection = null,
            settlementsSection = emptyTable,
            expensesSection = TripReportTableSection(
                title = "Itemised expenses",
                headers = listOf("Title", "Amount", "Note"),
                rows = expenseCells
            ),
            contributionsSection = emptyTable,
            withdrawalsSection = emptyTable,
            proUpsellCallout = null,
            brandFooter = TripReportBrandFooter(
                brandTagline = "Split expenses without borders",
                ctaHeadline = "Share expenses fairly. Zero stress.",
                ctaSubtitle = "Offline-first multi-currency group pockets.",
                websiteDisplay = "splittrip.eu",
                qrUrl = "https://splittrip.eu",
                qrCaption = "Scan to get SplitTrip"
            ),
            emptySectionLabel = "No activity recorded in this period",
            pageFooterTemplate = "Page %1\$d of %2\$d",
            csvSections = listOf(
                TripReportCsvSection(
                    title = "Itemised expenses",
                    headers = listOf("Title", "Amount", "Note"),
                    rows = expenseRows
                )
            )
        )
    }

    private companion object {
        const val SOURCE_URI_STRING = "content://es.pedrazamiguez.splittrip.fileprovider/exports/summary.pdf"
        const val DEST_URI_STRING = "content://com.android.externalstorage.documents/document/primary%3Asummary.pdf"
    }
}
