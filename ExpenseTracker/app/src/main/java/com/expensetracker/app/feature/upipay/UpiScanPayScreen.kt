package com.expensetracker.app.feature.upipay

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.core.util.parseAmountToMinorUnits
import com.expensetracker.app.data.model.ExpenseCategory
import com.expensetracker.app.feature.addexpense.AddExpenseEffect
import com.expensetracker.app.feature.addexpense.AddExpenseViewModel
import com.expensetracker.app.upi.UpiLauncher
import com.expensetracker.app.upi.UpiPaymentParser
import com.expensetracker.app.upi.UpiQrParser
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private enum class RecipientMode { SCAN, UPLOAD, PHONE }

@Composable
fun UpiScanPayScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddExpenseViewModel = koinViewModel(parameters = { parametersOf(null) }),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var mode by remember { mutableStateOf(RecipientMode.SCAN) }
    var payeeVpa by remember { mutableStateOf<String?>(null) }
    var payeeName by remember { mutableStateOf<String?>(null) }
    var amountText by remember { mutableStateOf("") }
    var recipientError by remember { mutableStateOf<String?>(null) }
    var paymentStarted by remember { mutableStateOf(false) }

    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) recipientError = "Camera permission is required to scan a QR code"
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) decodeQrFromUri(context, uri) { payload, error ->
            if (payload == null) recipientError = error
            else {
                payeeVpa = payload.vpa
                payeeName = payload.payeeName
                if (amountText.isBlank() && payload.amountMinor != null) amountText = payload.amountMinor.toAmountText()
                recipientError = null
            }
        } }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AddExpenseEffect.NavigateBack -> onNavigateBack()
                is AddExpenseEffect.ShowMessage -> { /* navigation is enough after a successful payment */ }
            }
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onNavigateBack) { Text("Close") }
                Text("Scan & Pay", style = MaterialTheme.typography.titleLarge)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(mode == RecipientMode.SCAN, { mode = RecipientMode.SCAN; recipientError = null }, label = { Text("Scan QR") })
                FilterChip(mode == RecipientMode.UPLOAD, { mode = RecipientMode.UPLOAD; recipientError = null }, label = { Text("Upload QR") })
                FilterChip(mode == RecipientMode.PHONE, { mode = RecipientMode.PHONE; recipientError = null }, label = { Text("Phone") })
            }

            when (mode) {
                RecipientMode.SCAN -> {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        Button(onClick = { cameraPermission.launch(Manifest.permission.CAMERA) }) { Text("Allow camera") }
                    } else {
                        CameraQrScanner(
                            modifier = Modifier.fillMaxWidth().height(260.dp),
                            onPayload = { payload ->
                                payeeVpa = payload.vpa
                                payeeName = payload.payeeName
                                if (amountText.isBlank() && payload.amountMinor != null) amountText = payload.amountMinor.toAmountText()
                                recipientError = null
                            },
                        )
                    }
                }
                RecipientMode.UPLOAD -> {
                    Button(onClick = { galleryLauncher.launch("image/*") }) { Text("Choose QR image") }
                }
                RecipientMode.PHONE -> {
                    OutlinedTextField(
                        value = payeeVpa.orEmpty(),
                        onValueChange = { payeeVpa = it.filter(Char::isDigit).take(10); recipientError = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("10-digit phone number") },
                        singleLine = true,
                    )
                    Text(
                        "Phone-number payments depend on the selected UPI app supporting UPI Number routing. No universal phone→VPA suffix is assumed.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (recipientError != null) Text(recipientError!!, color = MaterialTheme.colorScheme.error)

            payeeVpa?.takeIf { it.isNotBlank() }?.let { vpa ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(payeeName ?: "Recipient", style = MaterialTheme.typography.titleMedium)
                        Text(vpa, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' }.take(12) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Amount") },
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Note (optional)") },
                singleLine = true,
            )

            Text("Category", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ExpenseCategory.entries.take(4).forEach { category ->
                    FilterChip(
                        selected = uiState.selectedCategory == category,
                        onClick = { viewModel.onCategoryChange(category) },
                        label = { Text(category.displayName) },
                    )
                }
            }
            Spacer(Modifier.weight(1f))

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !paymentStarted,
                onClick = {
                    val minor = amountText.parseAmountToMinorUnits()
                    if (minor == null || minor <= 0L) {
                        recipientError = "Enter a valid amount"
                        return@Button
                    }
                    val vpa = payeeVpa?.trim().orEmpty()
                    if (vpa.isBlank()) {
                        recipientError = "Scan/upload a UPI QR or enter a phone number"
                        return@Button
                    }
                    if (vpa.all(Char::isDigit) && vpa.length == 10) {
                        // Best-effort UPI Number form. Apps that do not support it will reject it.
                        payeeVpa = "$vpa@upi"
                    }
                    val intent = UpiLauncher.buildIntent(vpa, minor, payeeName, uiState.note)
                    paymentStarted = UpiLauncher.launch(context.findActivity(), intent)
                    if (!paymentStarted) recipientError = "No compatible UPI app is installed"
                },
            ) {
                if (paymentStarted) CircularProgressIndicator() else Text("Pay with UPI")
            }
        }
    }
}

private fun Long.toAmountText(): String = "%.2f".format(java.util.Locale.US, this / 100.0)

private fun decodeQrFromUri(context: Context, uri: Uri, onResult: (com.expensetracker.app.upi.UpiQrPayload?, String?) -> Unit) {
    val scanner = BarcodeScanning.getClient()
    val image = runCatching { InputImage.fromFilePath(context, uri) }.getOrNull()
    if (image == null) {
        onResult(null, "Unable to read the selected image")
        scanner.close()
        return
    }
    scanner.process(image)
        .addOnSuccessListener { barcodes ->
            val payload = barcodes.firstNotNullOfOrNull { it.rawValue?.let(UpiQrParser::parse) }
            onResult(payload, if (payload == null) "No valid UPI QR found in this image" else null)
        }
        .addOnFailureListener { onResult(null, "Unable to scan the QR image") }
        .addOnCompleteListener { scanner.close() }
}

@Composable
private fun CameraQrScanner(modifier: Modifier, onPayload: (com.expensetracker.app.upi.UpiQrPayload) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }
    val delivered = remember { AtomicBoolean(false) }

    DisposableEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor) { proxy ->
                val mediaImage = proxy.image
                if (mediaImage == null || delivered.get()) {
                    proxy.close()
                    return@setAnalyzer
                }
                val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        val payload = barcodes.firstNotNullOfOrNull { it.rawValue?.let(UpiQrParser::parse) }
                        if (payload != null && delivered.compareAndSet(false, true)) onPayload(payload)
                    }
                    .addOnCompleteListener { proxy.close() }
            }
            runCatching {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
            scanner.close()
            executor.shutdown()
        }
    }
    AndroidView(factory = { previewView }, modifier = modifier)
}

private fun Context.findActivity(): android.app.Activity {
    var current = this
    while (current is android.content.ContextWrapper) {
        if (current is android.app.Activity) return current
        current = current.baseContext
    }
    error("Context is not an Activity")
}
