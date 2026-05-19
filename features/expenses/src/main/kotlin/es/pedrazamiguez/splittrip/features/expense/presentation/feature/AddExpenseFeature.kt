package es.pedrazamiguez.splittrip.features.expense.presentation.feature

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.AlertTriangle
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Camera
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Cash
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CreditCard
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Inbox
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Photo
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.X
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ActionBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.SheetAction
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TopPillController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.viewmodel.SharedViewModel
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.AddExpenseScreen
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.AddExpenseViewModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.AddExpenseUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseStep
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import java.io.File
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun AddExpenseFeature(
    addExpenseViewModel: AddExpenseViewModel = koinViewModel<AddExpenseViewModel>(),
    sharedViewModel: SharedViewModel = koinViewModel(
        viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner
    ),
    onAddExpenseSuccess: () -> Unit = {}
) {
    val pillController = LocalTopPillController.current
    val context = LocalContext.current
    val navController = LocalTabNavController.current

    val state by addExpenseViewModel.uiState.collectAsStateWithLifecycle()
    val selectedGroupId = sharedViewModel.selectedGroupId.collectAsStateWithLifecycle()

    var conflictResolution by remember {
        mutableStateOf<AddExpenseUiAction.ShowCashConflictResolution?>(null)
    }
    // Non-null while the receipt source selection sheet is visible.
    var showReceiptSourceSheet by remember { mutableStateOf(false) }

    // Camera launcher — requires a pre-created file URI via FileProvider.
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { captured ->
        if (captured) {
            cameraImageUri?.let { uri ->
                addExpenseViewModel.onEvent(AddExpenseUiEvent.ReceiptImageSelected(uri.toString()))
            }
        }
        cameraImageUri = null
    }

    // Gallery launcher — uses the photo picker introduced in Android 13.
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { addExpenseViewModel.onEvent(AddExpenseUiEvent.ReceiptImageSelected(it.toString())) }
    }

    // Document picker — surface PDFs and images in the system file manager.
    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { addExpenseViewModel.onEvent(AddExpenseUiEvent.ReceiptImageSelected(it.toString())) }
    }

    BackHandler { addExpenseViewModel.onEvent(AddExpenseUiEvent.PreviousStep) }

    ObserveAddExpenseActions(
        viewModel = addExpenseViewModel,
        pillController = pillController,
        context = context,
        navController = navController,
        onConflict = { conflictResolution = it }
    )

    conflictResolution?.let { resolution ->
        CashConflictResolutionSheet(
            state = state,
            resolution = resolution,
            viewModel = addExpenseViewModel,
            onDismiss = { conflictResolution = null }
        )
    }

    if (showReceiptSourceSheet) {
        ReceiptSourceSelectionSheet(
            onCameraSelected = {
                showReceiptSourceSheet = false
                cameraImageUri = createCameraUri(context)
                cameraImageUri?.let { cameraLauncher.launch(it) }
            },
            onGallerySelected = {
                showReceiptSourceSheet = false
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onDocumentSelected = {
                showReceiptSourceSheet = false
                documentLauncher.launch(arrayOf("image/*", "application/pdf"))
            },
            onDismiss = { showReceiptSourceSheet = false }
        )
    }

    AddExpenseScreen(
        groupId = selectedGroupId.value,
        uiState = state,
        onEvent = { event ->
            // RequestPickerSource is a pure-UI concern — handle it in the Feature
            // so the ViewModel never touches source selection or launcher APIs.
            if (event is AddExpenseUiEvent.RequestPickerSource) {
                showReceiptSourceSheet = true
            } else {
                addExpenseViewModel.onEvent(event, onAddExpenseSuccess)
            }
        }
    )
}

@Composable
private fun ObserveAddExpenseActions(
    viewModel: AddExpenseViewModel,
    pillController: TopPillController,
    context: Context,
    navController: NavController,
    onConflict: (AddExpenseUiAction.ShowCashConflictResolution) -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.actions.collectLatest { action ->
            when (action) {
                is AddExpenseUiAction.ShowError ->
                    pillController.showPill(message = action.message.asString(context))

                is AddExpenseUiAction.ShowCashConflictResolution -> {
                    // Refresh the tranche preview with the latest Room data immediately,
                    // then surface the guided resolution sheet to the user.
                    viewModel.refreshCashPreview()
                    onConflict(action)
                }

                AddExpenseUiAction.NavigateBack -> navController.popBackStack()

                AddExpenseUiAction.None -> Unit
            }
        }
    }
}

@Composable
private fun CashConflictResolutionSheet(
    state: AddExpenseUiState,
    resolution: AddExpenseUiAction.ShowCashConflictResolution,
    viewModel: AddExpenseViewModel,
    onDismiss: () -> Unit
) {
    val availableAmountDisplay = resolution.availableAmountDisplay
    ActionBottomSheet(
        title = stringResource(R.string.add_expense_cash_conflict_resolution_title),
        icon = TablerIcons.Outline.AlertTriangle,
        actions = buildList {
            if (availableAmountDisplay != null && resolution.availableAmountForInput != null) {
                add(
                    SheetAction(
                        text = stringResource(R.string.add_expense_cash_conflict_use_remaining, availableAmountDisplay),
                        icon = TablerIcons.Outline.Cash,
                        onClick = {
                            viewModel.onEvent(
                                AddExpenseUiEvent.ResolutionAmountSelected(resolution.availableAmountForInput)
                            )
                            onDismiss()
                        }
                    )
                )
            }
            add(
                SheetAction(
                    text = stringResource(R.string.add_expense_cash_conflict_switch_payment),
                    icon = TablerIcons.Outline.CreditCard,
                    onClick = {
                        val idx = state.applicableSteps.indexOf(AddExpenseStep.PAYMENT_METHOD)
                        if (idx >= 0) viewModel.onEvent(AddExpenseUiEvent.JumpToStep(idx))
                        onDismiss()
                    }
                )
            )
            add(
                SheetAction(
                    text = stringResource(R.string.add_expense_cash_conflict_dismiss),
                    icon = TablerIcons.Outline.X,
                    onClick = onDismiss
                )
            )
        },
        onDismiss = onDismiss
    )
}

/**
 * Creates a temporary file inside [filesDir]/receipts/ and returns a [FileProvider] URI
 * that can be passed to [TakePicture]. The file is later replaced by the stable WebP copy
 * produced by [ReceiptStorageServiceImpl] when the [ReceiptImageSelected] event fires.
 */
private fun createCameraUri(context: Context): Uri {
    val receiptsDir = File(context.filesDir, "receipts").also { it.mkdirs() }
    val tempFile = File.createTempFile("camera_", ".jpg", receiptsDir)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
}

@Composable
private fun ReceiptSourceSelectionSheet(
    onCameraSelected: () -> Unit,
    onGallerySelected: () -> Unit,
    onDocumentSelected: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionBottomSheet(
        title = stringResource(R.string.add_expense_receipt_attach),
        icon = TablerIcons.Outline.Camera,
        actions = listOf(
            SheetAction(
                text = stringResource(R.string.add_expense_receipt_attach_camera),
                icon = TablerIcons.Outline.Camera,
                onClick = onCameraSelected
            ),
            SheetAction(
                text = stringResource(R.string.add_expense_receipt_attach_gallery),
                icon = TablerIcons.Outline.Photo,
                onClick = onGallerySelected
            ),
            SheetAction(
                text = stringResource(R.string.add_expense_receipt_attach_document),
                icon = TablerIcons.Outline.Inbox,
                onClick = onDocumentSelected
            )
        ),
        onDismiss = onDismiss
    )
}
