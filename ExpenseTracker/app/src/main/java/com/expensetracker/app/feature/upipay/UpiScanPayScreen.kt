package com.expensetracker.app.feature.upipay

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.core.util.parseAmountToMinorUnits
import com.expensetracker.app.data.model.ExpenseCategory
import com.expensetracker.app.feature.addexpense.AddExpenseEffect
import com.expensetracker.app.feature.addexpense.AddExpenseViewModel
import com.expensetracker.app.upi.UpiLauncher
import com.expensetracker.app.upi.UpiPaymentParser
import com.expensetracker.app.upi.UpiQrParser
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import java.io.InputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private enum class RecipientMode { HOME, SCAN, UPLOAD, PHONE, DETAILS }

@Composable
fun UpiScanPayScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddExpenseViewModel = koinViewModel(parameters = { parametersOf(null) }),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var mode by remember { mutableStateOf(RecipientMode.HOME) }
    var payeeVpa by remember { mutableStateOf<String?>(null) }
    var payeeName by remember { mutableStateOf<String?>(null) }
    var amountText by remember { mutableStateOf("") }
    var phoneText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var paymentStarted by remember { mutableStateOf(false) }
    var pendingAmountMinor by remember { mutableStateOf<Long?>(null) }

    fun applyPayload(payload: com.expensetracker.app.upi.UpiQrPayload) {
        payeeVpa = payload.vpa
        payeeName = payload.payeeName
        if (amountText.isBlank() && payload.amountMinor != null) {
            amountText = payload.amountMinor.toAmountText()
        }
        errorMessage = null
        mode = RecipientMode.DETAILS
    }

    val paymentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        paymentStarted = false
        val response = result.data?.getStringExtra(UpiLauncher.EXTRA_UPI_RESPONSE)
        val parsed = UpiPaymentParser.parse(response)
        if (parsed.status.equals("success", ignoreCase = true)) {
            pendingAmountMinor?.let { viewModel.savePaidAmount(it, parsed.transactionId ?: parsed.approvalReference) }
        } else {
            errorMessage = "UPI payment was not completed"
        }
    }

    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) errorMessage = "Camera permission is required to scan a QR code"
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            decodeQrFromUri(context, uri) { payload, error ->
                if (payload == null) errorMessage = error
                else applyPayload(payload)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AddExpenseEffect.NavigateBack -> onNavigateBack()
                is AddExpenseEffect.ShowMessage -> Unit
            }
        }
    }

    fun goBack() {
        errorMessage = null
        mode = when (mode) {
            RecipientMode.HOME -> {
                onNavigateBack()
                RecipientMode.HOME
            }
            RecipientMode.SCAN, RecipientMode.UPLOAD, RecipientMode.PHONE -> RecipientMode.HOME
            RecipientMode.DETAILS -> if (payeeVpa.isNullOrBlank()) RecipientMode.HOME else RecipientMode.HOME
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TopBar(
                title = when (mode) {
                    RecipientMode.HOME -> "Scan & Pay"
                    RecipientMode.SCAN -> "Scan QR Code"
                    RecipientMode.UPLOAD -> "Upload QR Image"
                    RecipientMode.PHONE -> "Enter UPI ID or Phone"
                    RecipientMode.DETAILS -> "Payment Details"
                },
                onBack = ::goBack,
            )

            when (mode) {
                RecipientMode.HOME -> {
                    Text(
                        "Pay using any UPI app and we'll automatically add it to your expenses.",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    RecipientOptionCard(
                        icon = { Icon(Icons.Default.QrCodeScanner, null, tint = MaterialTheme.colorScheme.primary) },
                        title = "Scan QR Code",
                        subtitle = "Scan a UPI QR code using your camera",
                        onClick = {
                            errorMessage = null
                            mode = RecipientMode.SCAN
                        },
                    )
                    RecipientOptionCard(
                        icon = { Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.primary) },
                        title = "Upload QR Image",
                        subtitle = "Choose a QR code from your gallery",
                        onClick = {
                            errorMessage = null
                            mode = RecipientMode.UPLOAD
                        },
                    )
                    RecipientOptionCard(
                        icon = { Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.primary) },
                        title = "Enter UPI ID or Phone Number",
                        subtitle = "Pay using a UPI ID or mobile number",
                        onClick = {
                            errorMessage = null
                            mode = RecipientMode.PHONE
                        },
                    )

                    InfoCard()
                }

                RecipientMode.SCAN -> {
                    Text(
                        "Position the QR code inside the frame. You can also choose an image or enter the recipient manually.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        EmptyActionCard(
                            icon = Icons.Default.CameraAlt,
                            title = "Camera permission needed",
                            subtitle = "Allow camera access to scan a UPI QR code.",
                            actionText = "Allow camera",
                            onAction = { cameraPermission.launch(Manifest.permission.CAMERA) },
                        )
                    } else {
                        Card(shape = MaterialTheme.shapes.extraLarge) {
                            Box(Modifier.fillMaxWidth().height(360.dp)) {
                                CameraQrScanner(
                                    modifier = Modifier.fillMaxSize(),
                                    onPayload = ::applyPayload,
                                )
                                Surface(
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                                    shape = MaterialTheme.shapes.large,
                                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f),
                                ) {
                                    Text(
                                        "Position the QR code within the frame",
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                        color = MaterialTheme.colorScheme.inverseOnSurface,
                                    )
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryAction("Gallery", Icons.Default.Image, Modifier.weight(1f)) { mode = RecipientMode.UPLOAD }
                        SecondaryAction("Enter manually", Icons.Default.Edit, Modifier.weight(1f)) { mode = RecipientMode.PHONE }
                    }
                }

                RecipientMode.UPLOAD -> {
                    EmptyActionCard(
                        icon = Icons.Default.Image,
                        title = "Choose a QR image",
                        subtitle = "Select a clear UPI QR code from your gallery.",
                        actionText = "Select image",
                        onAction = { galleryLauncher.launch("image/*") },
                    )
                    Text(
                        "Supported image formats depend on your device gallery. Make sure the QR code is clear and readable.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                RecipientMode.PHONE -> {
                    Text(
                        "Enter the recipient's UPI ID or 10-digit phone number.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = phoneText,
                        onValueChange = {
                            phoneText = it.take(100)
                            errorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("UPI ID or Phone Number") },
                        placeholder = { Text("e.g. name@upi or 9876543210") },
                        leadingIcon = { Icon(Icons.Default.Phone, null) },
                        singleLine = true,
                    )
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "We'll use the entered UPI ID or phone number to create the payment request.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        onClick = {
                            val value = phoneText.trim()
                            val valid = value.contains("@") || value.filter(Char::isDigit).length == 10
                            if (!valid) {
                                errorMessage = "Enter a valid UPI ID or 10-digit phone number"
                            } else {
                                payeeVpa = value
                                payeeName = null
                                errorMessage = null
                                mode = RecipientMode.DETAILS
                            }
                        },
                    ) { Text("Continue", style = MaterialTheme.typography.titleMedium) }
                }

                RecipientMode.DETAILS -> {
                    RecipientSummary(payeeName, payeeVpa)
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text("Edit the amount, category and optional note before paying.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' }.take(12) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Amount") },
                        leadingIcon = { Text("₹", style = MaterialTheme.typography.titleMedium) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = uiState.note,
                        onValueChange = viewModel::onNoteChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Note (optional)") },
                        leadingIcon = { Icon(Icons.Default.ReceiptLong, null) },
                        minLines = 1,
                        maxLines = 3,
                    )
                    Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ExpenseCategory.entries.forEach { category ->
                            CategoryCard(
                                category = category,
                                selected = uiState.selectedCategory == category,
                                onClick = { viewModel.onCategoryChange(category) },
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    Button(
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        enabled = !paymentStarted,
                        onClick = {
                            val minor = amountText.parseAmountToMinorUnits()
                            if (minor == null || minor <= 0L) {
                                errorMessage = "Enter a valid amount"
                                return@Button
                            }
                            var vpa = payeeVpa?.trim().orEmpty()
                            if (vpa.isBlank()) {
                                errorMessage = "Select a payment recipient"
                                return@Button
                            }
                            if (vpa.all(Char::isDigit) && vpa.length == 10) vpa = "$vpa@upi"
                            pendingAmountMinor = minor
                            paymentStarted = true
                            errorMessage = null
                            paymentLauncher.launch(UpiLauncher.buildIntent(vpa, minor, payeeName, uiState.note))
                        },
                    ) {
                        if (paymentStarted) CircularProgressIndicator(modifier = Modifier.size(22.dp))
                        else Text("Pay with UPI", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "You'll be redirected to your selected UPI app.",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            errorMessage?.takeIf { mode != RecipientMode.DETAILS && it.isNotBlank() }?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
        Spacer(Modifier.width(4.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RecipientOptionCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(Modifier.size(54.dp), contentAlignment = Alignment.Center) { icon() }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("How it works", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text("1  Select a payment method", style = MaterialTheme.typography.bodyMedium)
            Text("2  Complete payment in your UPI app", style = MaterialTheme.typography.bodyMedium)
            Text("3  We'll automatically add it to your expenses", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EmptyActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionText: String,
    onAction: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(18.dp).size(34.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onAction) { Text(actionText) }
        }
    }
}

@Composable
private fun SecondaryAction(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier, onClick = onClick, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 13.dp, horizontal = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(7.dp))
            Text(text, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun RecipientSummary(name: String?, vpa: String?) {
    Card(modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    (name ?: vpa ?: "R").take(1).uppercase(),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(name ?: "Recipient", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(vpa.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CategoryCard(category: ExpenseCategory, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(112.dp),
        onClick = onClick,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.ReceiptLong, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(7.dp))
            Text(category.displayName, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

private fun Long.toAmountText(): String = "%.2f".format(java.util.Locale.US, this / 100.0)

private fun decodeQrFromUri(
    context: Context,
    uri: Uri,
    onResult: (com.expensetracker.app.upi.UpiQrPayload?, String?) -> Unit,
) {
    val bitmap = runCatching {
        context.contentResolver.openInputStream(uri)?.use(InputStream::readBytes)?.let { bytes ->
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }.getOrNull()
    val result = bitmap?.let(::decodeQr)
    bitmap?.recycle()
    if (result == null) onResult(null, "No valid UPI QR found in this image")
    else onResult(UpiQrParser.parse(result.text), null)
}

private fun decodeQr(bitmap: Bitmap): Result? {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= 0 || height <= 0) return null
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    val source = RGBLuminanceSource(width, height, pixels)
    return runCatching { MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source))) }.getOrNull()
}

@Composable
private fun CameraQrScanner(
    modifier: Modifier,
    onPayload: (com.expensetracker.app.upi.UpiQrPayload) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val delivered = remember { AtomicBoolean(false) }

    DisposableEffect(lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context.applicationContext)
        future.addListener({
            val provider = runCatching { future.get() }.getOrNull() ?: return@addListener
            runCatching {
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                analysis.setAnalyzer(executor) { image ->
                    if (delivered.get()) {
                        image.close()
                        return@setAnalyzer
                    }
                    val plane = image.planes.firstOrNull()
                    if (plane == null) {
                        image.close()
                        return@setAnalyzer
                    }
                    val buffer = plane.buffer
                    val pixelStride = plane.pixelStride
                    val rowStride = plane.rowStride
                    if (pixelStride <= 0) {
                        image.close()
                        return@setAnalyzer
                    }
                    val rowPadding = rowStride - pixelStride * image.width
                    val bitmap = runCatching {
                        Bitmap.createBitmap(
                            image.width + rowPadding / pixelStride,
                            image.height,
                            Bitmap.Config.ARGB_8888,
                        ).also {
                            buffer.rewind()
                            it.copyPixelsFromBuffer(buffer)
                        }
                    }.getOrNull()
                    val result = bitmap?.let(::decodeQr)
                    bitmap?.recycle()
                    image.close()
                    val payload = result?.text?.let(UpiQrParser::parse)
                    if (payload != null && delivered.compareAndSet(false, true)) {
                        ContextCompat.getMainExecutor(context).execute { onPayload(payload) }
                    }
                }
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            runCatching { future.get().unbindAll() }
            executor.shutdownNow()
        }
    }
    AndroidView(factory = { previewView }, modifier = modifier)
}
