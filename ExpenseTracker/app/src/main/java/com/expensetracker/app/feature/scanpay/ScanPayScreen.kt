package com.expensetracker.app.feature.scanpay

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.expensetracker.app.core.util.UpiPayee
import com.expensetracker.app.data.model.ExpenseCategory
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun ScanPayScreen(
    viewModel: ScanPayViewModel = koinViewModel(),
) {

    val uiState by viewModel
        .uiState
        .collectAsStateWithLifecycle()

    val snackbarHostState =
        remember {
            SnackbarHostState()
        }

    val lifecycle =
        LocalLifecycleOwner.current.lifecycle

    val context =
        LocalContext.current

    var showScannedQrDebug by remember {
        mutableStateOf(false)
    }

    var pendingDebugPaymentUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var resultDebugText by remember {
        mutableStateOf<String?>(null)
    }

    val coroutineScope = rememberCoroutineScope()

    val imageQrScanner =
        remember {
            BarcodeScanning.getClient(
                BarcodeScannerOptions
                    .Builder()
                    .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE,
                    )
                    .build(),
            )
        }

    DisposableEffect(imageQrScanner) {
        onDispose {
            imageQrScanner.close()
        }
    }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent(),
        ) { imageUri ->

            imageUri?.let { selectedImageUri ->

                val image =
                    runCatching {
                        InputImage.fromFilePath(
                            context,
                            selectedImageUri,
                        )
                    }.getOrNull()

                if (image == null) {

                    viewModel.onQrImageReadFailed()

                } else {

                    imageQrScanner
                        .process(image)
                        .addOnSuccessListener { barcodes ->

                            val rawValue =
                                barcodes
                                    .firstOrNull {
                                        it.rawValue != null
                                    }
                                    ?.rawValue

                            if (rawValue == null) {
                                viewModel.onQrImageReadFailed()
                            } else {
                                viewModel.onQrDetected(rawValue)
                            }
                        }
                        .addOnFailureListener {
                            viewModel.onQrImageReadFailed()
                        }
                }
            }
        }

    /*
     * Launches the selected UPI application using
     * the standard `upi://pay` ACTION_VIEW intent.
     */
    val paymentLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->

            resultDebugText = upiResultDebugText(
                resultCode = result.resultCode,
                responseExtra = result.data?.getStringExtra("response"),
            )

            viewModel.onPaymentActivityResult(
                result.resultCode,
                result.data?.getStringExtra(
                    "response",
                ),
            )
        }

    LaunchedEffect(Unit) {

        lifecycle.repeatOnLifecycle(
            Lifecycle.State.STARTED,
        ) {

            viewModel.effects.collect { effect ->

                when (effect) {

                    is ScanPayEffect.ShowMessage -> {

                        snackbarHostState.showSnackbar(
                            effect.message,
                        )
                    }

                    is ScanPayEffect.LaunchUpiApp -> {
                        pendingDebugPaymentUri = effect.uri
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),

        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
            )
        },
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {

            CameraPreview(
                enabled =
                    uiState.stage
                        is ScanPayStage.Scanning,

                onQrDetected =
                    viewModel::onQrDetected,

                modifier =
                    Modifier.fillMaxSize(),
            )

            val stage =
                uiState.stage

            if (stage is ScanPayStage.Scanning) {

                Surface(

                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(16.dp),

                    shape = RoundedCornerShape(24.dp),

                    tonalElevation = 4.dp,
                ) {

                    TextButton(
                        onClick = {
                            imagePickerLauncher.launch("image/*")
                        },
                    ) {
                        Text("Upload QR image")
                    }
                }
            }

            if (
                stage is ScanPayStage.Confirming
            ) {

                ConfirmPaymentSheet(

                    payee =
                        stage.payee,

                    amountText =
                        uiState.amountText,

                    amountError =
                        uiState.amountError,

                    amountPrefilledFromQr =
                        uiState.amountPrefilledFromQr,

                    note =
                        uiState.note,

                    selectedCategory =
                        uiState.selectedCategory,

                    canPay =
                        uiState.canPay,

                    onAmountChange =
                        viewModel::onAmountChange,

                    onNoteChange =
                        viewModel::onNoteChange,

                    onCategoryChange =
                        viewModel::onCategoryChange,

                    onDebugClick = {
                        showScannedQrDebug = true
                    },

                    onPayClick =
                        viewModel::onPayClick,

                    onCancel =
                        viewModel::onCancelConfirm,

                    modifier =
                        Modifier.align(
                            Alignment.BottomCenter,
                        ),
                )
            }
        }
    }

    val debugPayee = when (val stage = uiState.stage) {
        is ScanPayStage.Confirming -> stage.payee
        is ScanPayStage.LaunchingPayment -> stage.payee
        ScanPayStage.Scanning -> null
    }

    if (showScannedQrDebug && debugPayee != null) {
        val debugText = scannedQrDebugText(debugPayee)

        AlertDialog(
            onDismissRequest = {
                showScannedQrDebug = false
            },
            title = {
                Text("UPI Debug — Scanned QR")
            },
            text = {
                Text(
                    text = debugText,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        copyDebugText(context, debugText)
                    },
                ) {
                    Text("Copy")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showScannedQrDebug = false
                    },
                ) {
                    Text("Cancel")
                }
            },
        )
    }

    pendingDebugPaymentUri?.let { finalUri ->
        val payee = debugPayee

        if (payee != null) {
            val debugIntent = Intent(
                Intent.ACTION_VIEW,
                finalUri,
            )

            val resolvedActivity = debugIntent.resolveActivity(
                context.packageManager,
            )

            val availableHandlers = context.packageManager
                .queryIntentActivities(
                    debugIntent,
                    PackageManager.MATCH_DEFAULT_ONLY,
                )
                .map {
                    "${it.activityInfo.packageName}/${it.activityInfo.name}"
                }

            val debugText = buildString {
                appendLine(
                    paymentUriDebugText(
                        payee = payee,
                        amount = uiState.amountText,
                        note = uiState.note,
                        finalUri = finalUri,
                    ),
                )
                appendLine()
                appendLine("Intent action: ${debugIntent.action}")
                appendLine("Intent data: ${debugIntent.dataString}")
                appendLine("Intent type: ${debugIntent.type ?: "null"}")
                appendLine("Intent flags: ${debugIntent.flags}")
                appendLine(
                    "Resolved activity: ${resolvedActivity?.flattenToString() ?: "None"}",
                )
                appendLine("Available UPI handlers:")
                if (availableHandlers.isEmpty()) {
                    appendLine("None")
                } else {
                    availableHandlers.forEach { handler ->
                        appendLine(handler)
                    }
                }
            }

            AlertDialog(
                onDismissRequest = {
                    pendingDebugPaymentUri = null
                    viewModel.onPaymentActivityResult(
                        Activity.RESULT_CANCELED,
                        null,
                    )
                },
                title = {
                    Text("UPI Debug — Payment URI")
                },
                text = {
                    Text(
                        text = debugText,
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            pendingDebugPaymentUri = null

                            try {
                                paymentLauncher.launch(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        finalUri,
                                    ),
                                )
                            } catch (e: ActivityNotFoundException) {
                                viewModel.onPaymentActivityResult(
                                    Activity.RESULT_CANCELED,
                                    null,
                                )

                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        "No UPI app found on this device",
                                    )
                                }
                            }
                        },
                    ) {
                        Text("Continue")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                copyDebugText(context, debugText)
                            },
                        ) {
                            Text("Copy")
                        }
                        TextButton(
                            onClick = {
                                pendingDebugPaymentUri = null
                                viewModel.onPaymentActivityResult(
                                    Activity.RESULT_CANCELED,
                                    null,
                                )
                            },
                        ) {
                            Text("Cancel")
                        }
                    }
                },
            )
        }
    }

    resultDebugText?.let { debugText ->
        AlertDialog(
            onDismissRequest = {
                resultDebugText = null
            },
            title = {
                Text("UPI Result Debug")
            },
            text = {
                Text(
                    text = debugText,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        copyDebugText(context, debugText)
                    },
                ) {
                    Text("Copy")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        resultDebugText = null
                    },
                ) {
                    Text("Close")
                }
            },
        )
    }
}

@Composable
private fun CameraPreview(
    enabled: Boolean,
    onQrDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {

    val context =
        LocalContext.current

    var hasCameraPermission by remember {

        mutableStateOf(

            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    /*
     * Survives tab switches.
     */
    var hasRequestedPermission by
        rememberSaveable {
            mutableStateOf(false)
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->

            hasCameraPermission =
                granted

            hasRequestedPermission =
                true
        }

    LaunchedEffect(Unit) {

        if (
            !hasCameraPermission &&
            !hasRequestedPermission
        ) {

            hasRequestedPermission =
                true

            permissionLauncher.launch(
                Manifest.permission.CAMERA,
            )
        }
    }

    if (!hasCameraPermission) {

        val activity =
            context as? Activity

        val canShowRationale =
            activity != null &&
                ActivityCompat
                    .shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.CAMERA,
                    )

        val permanentlyDenied =
            hasRequestedPermission &&
                !canShowRationale

        PermissionRationale(

            permanentlyDenied =
                permanentlyDenied,

            onRequestPermission = {
                permissionLauncher.launch(
                    Manifest.permission.CAMERA,
                )
            },

            onOpenSettings = {

                context.startActivity(

                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts(
                            "package",
                            context.packageName,
                            null,
                        ),
                    ),
                )
            },

            modifier =
                modifier,
        )

        return
    }

    QrCodeCamera(
        enabled = enabled,
        onQrDetected = onQrDetected,
        modifier = modifier,
    )
}

@Composable
private fun QrCodeCamera(
    enabled: Boolean,
    onQrDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {

    val context =
        LocalContext.current

    val lifecycleOwner =
        LocalLifecycleOwner.current

    val previewView =
        remember {
            PreviewView(context)
        }

    val onQrDetectedState =
        rememberUpdatedState(
            onQrDetected,
        )

    val enabledState =
        rememberUpdatedState(
            enabled,
        )

    var bindingFailed by
        remember {
            mutableStateOf(false)
        }

    DisposableEffect(
        lifecycleOwner,
    ) {

        bindingFailed = false

        val executor =
            Executors.newSingleThreadExecutor(
