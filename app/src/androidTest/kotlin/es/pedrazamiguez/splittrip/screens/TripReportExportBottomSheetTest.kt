package es.pedrazamiguez.splittrip.screens

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.WindowInsets as WindowInsetsOverride
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.core.graphics.Insets as AndroidXInsets
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.domain.model.report.TripReportFormat
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.component.export.TripReportExportBottomSheet
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SpendingPeriodPreset
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.TripReportExportSheetState
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val COMPACT_VIEWPORT_SIZE = DpSize(360.dp, 640.dp)
private const val MIN_ACTION_BUTTON_HEIGHT_DP = 56f
private const val BOUNDS_TOLERANCE_DP = 0.5f

@RunWith(AndroidJUnit4::class)
class TripReportExportBottomSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun freeTier_compactViewport_rendersAllActionButtonsAtFullHeightAndAccessible() {
        val exportShareLabel = context.getString(R.string.balances_export_confirm_button)
        val saveToDeviceLabel = context.getString(R.string.balances_export_save_button)
        val cancelLabel = context.getString(R.string.balances_export_cancel_button)
        var exportClicks = 0
        var saveClicks = 0
        var dismissClicks = 0

        composeRule.setContent {
            SplitTripTheme {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.ForcedSize(COMPACT_VIEWPORT_SIZE)
                ) {
                    TripReportExportBottomSheet(
                        sheetState = TripReportExportSheetState(
                            isVisible = true,
                            selectedPreset = SpendingPeriodPreset.ALL_TIME,
                            selectedFormat = TripReportFormat.PDF,
                            isCsvLocked = true,
                            includesItemisedDetails = false,
                            remainingFreeExports = 1,
                            monthlyFreeExportLimit = 1
                        ),
                        onSelectPreset = {},
                        onSelectCustomStartDate = {},
                        onSelectCustomEndDate = {},
                        onSelectFormat = {},
                        onConfirmExport = { exportClicks++ },
                        onSaveToDevice = { saveClicks++ },
                        onDismiss = { dismissClicks++ }
                    )
                }
            }
        }

        composeRule.waitForIdle()

        val exportNode = composeRule.onNodeWithText(exportShareLabel)
        val saveNode = composeRule.onNodeWithText(saveToDeviceLabel)
        val cancelNode = composeRule.onNodeWithText(cancelLabel)

        exportNode.assertIsDisplayed()
        saveNode.assertIsDisplayed()
        cancelNode.assertIsDisplayed()

        val exportBounds = exportNode.getUnclippedBoundsInRoot()
        val saveBounds = saveNode.getUnclippedBoundsInRoot()
        assertTrue(
            "Export & share height (${exportBounds.height.value}) should be >= $MIN_ACTION_BUTTON_HEIGHT_DP",
            exportBounds.height.value >= MIN_ACTION_BUTTON_HEIGHT_DP - BOUNDS_TOLERANCE_DP
        )
        assertTrue(
            "Save to device height (${saveBounds.height.value}) should be >= $MIN_ACTION_BUTTON_HEIGHT_DP",
            saveBounds.height.value >= MIN_ACTION_BUTTON_HEIGHT_DP - BOUNDS_TOLERANCE_DP
        )

        exportNode.performClick()
        saveNode.performClick()
        cancelNode.performClick()
        composeRule.waitForIdle()

        assertEquals(1, exportClicks)
        assertEquals(1, saveClicks)
        assertEquals(1, dismissClicks)
    }

    @Test
    fun proCustomPeriod_compactViewport_scrollsFormAndKeepsButtonsUnclipped() {
        val titleLabel = context.getString(R.string.balances_export_sheet_title)
        val periodLabel = context.getString(R.string.balances_export_period_label)
        val formatLabel = context.getString(R.string.balances_export_format_label)
        val exportShareLabel = context.getString(R.string.balances_export_confirm_button)
        val saveToDeviceLabel = context.getString(R.string.balances_export_save_button)
        val cancelLabel = context.getString(R.string.balances_export_cancel_button)

        composeRule.setContent {
            SplitTripTheme {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.ForcedSize(COMPACT_VIEWPORT_SIZE) then
                        DeviceConfigurationOverride.FontScale(1.3f)
                ) {
                    TripReportExportBottomSheet(
                        sheetState = TripReportExportSheetState(
                            isVisible = true,
                            selectedPreset = SpendingPeriodPreset.CUSTOM,
                            customStartDate = LocalDate.of(2026, 11, 1),
                            customEndDate = LocalDate.of(2026, 11, 20),
                            formattedCustomStartDate = "1 Nov",
                            formattedCustomEndDate = "20 Nov",
                            selectedFormat = TripReportFormat.CSV,
                            isCsvLocked = false,
                            includesItemisedDetails = true
                        ),
                        onSelectPreset = {},
                        onSelectCustomStartDate = {},
                        onSelectCustomEndDate = {},
                        onSelectFormat = {},
                        onConfirmExport = {},
                        onSaveToDevice = {},
                        onDismiss = {}
                    )
                }
            }
        }

        composeRule.waitForIdle()

        val exportNode = composeRule.onNodeWithText(exportShareLabel)
        val saveNode = composeRule.onNodeWithText(saveToDeviceLabel)
        val cancelNode = composeRule.onNodeWithText(cancelLabel)

        exportNode.assertIsDisplayed()
        saveNode.assertIsDisplayed()
        cancelNode.assertIsDisplayed()

        val exportBounds = exportNode.getUnclippedBoundsInRoot()
        val saveBounds = saveNode.getUnclippedBoundsInRoot()
        assertTrue(
            "Export & share height (${exportBounds.height.value}) should be >= $MIN_ACTION_BUTTON_HEIGHT_DP",
            exportBounds.height.value >= MIN_ACTION_BUTTON_HEIGHT_DP - BOUNDS_TOLERANCE_DP
        )
        assertTrue(
            "Save to device height (${saveBounds.height.value}) should be >= $MIN_ACTION_BUTTON_HEIGHT_DP",
            saveBounds.height.value >= MIN_ACTION_BUTTON_HEIGHT_DP - BOUNDS_TOLERANCE_DP
        )

        composeRule.onNodeWithText(titleLabel).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(periodLabel).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(formatLabel).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun expandedSheet_withStatusBarInsets_titleClearsStatusBar() {
        val statusBarTopPx = 96
        val navBarBottomPx = 72
        val simulatedInsets = WindowInsetsCompat.Builder()
            .setInsets(
                WindowInsetsCompat.Type.statusBars(),
                AndroidXInsets.of(0, statusBarTopPx, 0, 0)
            )
            .setInsets(
                WindowInsetsCompat.Type.navigationBars(),
                AndroidXInsets.of(0, 0, 0, navBarBottomPx)
            )
            .build()
        val titleLabel = context.getString(R.string.balances_export_sheet_title)
        var density = 1f

        composeRule.setContent {
            density = LocalDensity.current.density
            SplitTripTheme {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.ForcedSize(COMPACT_VIEWPORT_SIZE) then
                        DeviceConfigurationOverride.WindowInsetsOverride(simulatedInsets)
                ) {
                    TripReportExportBottomSheet(
                        sheetState = TripReportExportSheetState(
                            isVisible = true,
                            selectedPreset = SpendingPeriodPreset.ALL_TIME,
                            selectedFormat = TripReportFormat.PDF,
                            isCsvLocked = true,
                            includesItemisedDetails = false,
                            remainingFreeExports = 1,
                            monthlyFreeExportLimit = 1
                        ),
                        onSelectPreset = {},
                        onSelectCustomStartDate = {},
                        onSelectCustomEndDate = {},
                        onSelectFormat = {},
                        onConfirmExport = {},
                        onSaveToDevice = {},
                        onDismiss = {}
                    )
                }
            }
        }

        composeRule.waitForIdle()

        val titleBounds = composeRule.onNodeWithText(titleLabel).getUnclippedBoundsInRoot()
        val statusBarTopDp = statusBarTopPx / density
        assertTrue(
            "Title top (${titleBounds.top.value}) should clear status bar ($statusBarTopDp)",
            titleBounds.top.value >= statusBarTopDp - BOUNDS_TOLERANCE_DP
        )
    }
}
