package com.expensetracker.app.feature.scanpay

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun ScanPayScreen(
    viewModel: ScanPayViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val coroutineScope = rememberCoroutineScope()

    val paymentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.onPaymentActivityResult(
            result.resultCode,
            result.data?.getStringExtra("response"),
        )
    }

    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    ScanPayEffect.LaunchUpiApp -> {
                        val upiIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("upi://pay"),
                        )

                        if (upiIntent.resolveActivity(context.packageManager) == null) {
                            viewModel.onPaymentActivityResult(
                                Activity.RESULT_CANCELED,
                                null,
                            )
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("No UPI app is installed.")
                            }
                        } else {
                            try {
                                paymentLauncher.launch(
                                    Intent.createChooser(
                                        upiIntent,
                                        "Pay with UPI",
                                    ),
                                )
                            } catch (_: ActivityNotFoundException) {
                                viewModel.onPaymentActivityResult(
                                    Activity.RESULT_CANCELED,
                                    null,
                                )
                                snackbarHostState.showSnackbar("No UPI app is installed.")
                            }
                        }
                    }
                    is ScanPayEffect.ShowMessage -> {
                        snackbarHostState.showSnackbar(effect.message)
                    }
                }
            }
        }
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
            Text(
                text = "Pay with UPI",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = "Choose the expense details here. The amount and payment details are entered entirely in your selected UPI app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = "Category",
                style = MaterialTheme.typography.titleMedium,
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = com.expensetracker.app.data.model.ExpenseCategory.entries,
                    key = { it.name },
                ) { category ->
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

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "The amount will be entered after you choose a UPI app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = viewModel::onPayClick,
                enabled = uiState.canPay,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Pay with UPI")
            }
        }
    }
}
