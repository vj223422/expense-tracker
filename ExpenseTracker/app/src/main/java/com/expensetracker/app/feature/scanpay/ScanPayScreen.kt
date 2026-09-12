package com.expensetracker.app.feature.scanpay

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import org.koin.androidx.compose.koinViewModel
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun ScanPayScreen(
    viewModel: ScanPayViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var showScanner by remember { mutableStateOf(false) }

    val paymentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val response = result.data?.getStringExtra("response")
            ?: result.data?.dataString?.takeIf {
                it.contains("status=", ignoreCase = true) ||
                    it.contains("Status=", ignoreCase = true)
            }
        viewModel.onPaymentActivityResult(result.resultCode, response)
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            scanQrImage(
                context = context,
                uri = uri,
                onResult = { raw ->
                    val payee = parseUpiPayee(raw)
                    if (payee != null) {
                        viewModel.onPayeeVpaChange(payee.first)
                        viewModel.onPayeeNameChange(payee.second)
                    } else {
                        viewModel.showMessage("The selected image does not contain a valid UPI QR code.")
                    }
                },
                onError = { viewModel.showMessage("Couldn't read a UPI QR code from that image.") },
            )
        }
    }

    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is ScanPayEffect.LaunchUpiApp -> {
                        val payeeVpa = normalizePayeeVpa(effect.payeeVpa)
                        val uri = Uri.Builder()
                            .scheme("upi")
                            .authority("pay")
                            .appendQueryParameter("pa", payeeVpa)
                            .appendQueryParameter("pn", effect.payeeName)
                            .appendQueryParameter("tr", effect.transactionRef)
                            .appendQueryParameter("tn", effect.transactionNote)
                            .appendQueryParameter("cu", "INR")
                            .build()
                        val upiIntent = Intent(Intent.ACTION_VIEW, uri)

                        if (upiIntent.resolveActivity(context.packageManager) == null) {
                            viewModel.onPaymentActivityResult(Activity.RESULT_CANCELED, null)
                        } else {
                            try {
                                paymentLauncher.launch(
                                    Intent.createChooser(upiIntent, "Choose UPI app"),
                                )
                            } catch (_: ActivityNotFoundException) {
                                viewModel.onPaymentActivityResult(Activity.RESULT_CANCELED, null)
                            }
                        }
                    }
                    is ScanPayEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    if (showScanner) {
        QrScannerDialog(
            onQrDetected = { raw ->
                val payee = parseUpiPayee(raw)
                showScanner = false
                if (payee != null) {
                    viewModel.onPayeeVpaChange(payee.first)
                    viewModel.onPayeeNameChange(payee.second)
                } else {
                    viewModel.showMessage("This QR code is not a valid UPI payment QR.")
                }
            },
            onDismiss = { showScanner = false },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Pay with UPI", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Choose the person or merchant first. Your UPI app will open with the payee filled in, and you can enter the amount there.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text("Payee", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showScanner = true }) { Text("Scan QR") }
                OutlinedButton(onClick = { imagePicker.launch("image/*") }) { Text("Upload QR") }
            }

            OutlinedTextField(
                value = uiState.payeeVpa,
                onValueChange = viewModel::onPayeeVpaChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("UPI ID or phone number") },
                placeholder = { Text("name@upi or 9876543210") },
                singleLine = true,
                enabled = uiState.stage is ScanPayStage.Ready,
            )
            if (uiState.payeeVpa.isNotBlank() && uiState.payeeName.isNotBlank()) {
                Text("Paying ${uiState.payeeName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            OutlinedTextField(
                value = uiState.payeeName,
                onValueChange = viewModel::onPayeeNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Payee name (optional)") },
                singleLine = true,
                enabled = uiState.stage is ScanPayStage.Ready,
            )

            Text("Category", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(com.expensetracker.app.data.model.ExpenseCategory.entries, key = { it.name }) { category ->
                    FilterChip(
                        selected = category == uiState.selectedCategory,
                        onClick = { viewModel.onCategoryChange(category) },
                        label = { Text(category.displayName) },
                    )
                }
            }

            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Note (optional)") },
                placeholder = { Text("e.g. Dinner") },
                singleLine = true,
                enabled = uiState.stage is ScanPayStage.Ready,
            )

            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = viewModel::onPayClick, enabled = uiState.canPay, modifier = Modifier.fillMaxWidth()) {
                Text("Continue to UPI")
            }
        }
    }
}

@Composable
private fun QrScannerDialog(onQrDetected: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scan UPI QR") },
        text = {
            if (hasCameraPermission) CameraPreview(onQrDetected)
            else Text("Camera permission is required to scan a QR code.")
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreview(onQrDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }
    val handled = remember { AtomicBoolean(false) }

    DisposableEffect(previewView, lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage == null || handled.get()) {
                    imageProxy.close()
                    return@setAnalyzer
                }
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        val raw = barcodes.firstOrNull()?.rawValue
                        if (!raw.isNullOrBlank() && handled.compareAndSet(false, true)) onQrDetected(raw)
                    }
                    .addOnCompleteListener { imageProxy.close() }
            }
            runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
            scanner.close()
            executor.shutdown()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxWidth().height(320.dp),
        factory = { previewView },
    )
}

private fun parseUpiPayee(rawValue: String): Pair<String, String>? {
    val uri = runCatching { Uri.parse(rawValue.trim()) }.getOrNull() ?: return null
    if (!uri.scheme.equals("upi", ignoreCase = true) || !uri.host.equals("pay", ignoreCase = true)) return null
    val vpa = uri.getQueryParameter("pa")?.trim().orEmpty()
    if (vpa.isBlank()) return null
    return vpa to uri.getQueryParameter("pn")?.trim().orEmpty()
}

private fun normalizePayeeVpa(value: String): String {
    val trimmed = value.trim()
    return if (trimmed.matches(Regex("\\d{8,15}"))) "$trimmed@upi" else trimmed
}

private fun scanQrImage(context: Context, uri: Uri, onResult: (String) -> Unit, onError: () -> Unit) {
    val image = runCatching { InputImage.fromFilePath(context, uri) }.getOrNull()
    if (image == null) {
        onError()
        return
    }
    BarcodeScanning.getClient().process(image)
        .addOnSuccessListener { barcodes -> barcodes.firstOrNull()?.rawValue?.let(onResult) ?: onError() }
        .addOnFailureListener { onError() }
}
