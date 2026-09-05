package com.expensetracker.app.feature.addexpense

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.expensetracker.app.core.designsystem.color
import com.expensetracker.app.core.designsystem.icon
import com.expensetracker.app.core.theme.LocalReducedMotion
import com.expensetracker.app.core.theme.MotionDurations
import com.expensetracker.app.core.theme.MotionEasing
import com.expensetracker.app.core.util.toRelativeOrFormatted
import com.expensetracker.app.data.model.ExpenseCategory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.koin.androidx.compose.koinViewModel

@Composable
fun AddExpenseScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddExpenseViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    AddExpenseEffect.NavigateBack -> onNavigateBack()
                    is AddExpenseEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    // A nested Scaffold here hosts only this screen's own close/save bar and its snackbar; it adds
    // no bottomBar/FAB of its own, so it does not duplicate the chrome the parent NavHost Scaffold
    // already provides (that Scaffold hides its bottom bar/FAB for this full-screen route anyway).
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            AddExpenseTopBar(
                canSave = uiState.canSave,
                isSaving = uiState.isSaving,
                onClose = viewModel::onDismiss,
                onSave = viewModel::onSaveClick,
            )
        },
    ) { innerPadding ->
        AddExpenseContent(
            uiState = uiState,
            actions = viewModel,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}

@Composable
private fun AddExpenseTopBar(
    canSave: Boolean,
    isSaving: Boolean,
    onClose: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
        }
        Text(
            text = "Add Expense",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        )
        SaveAction(canSave = canSave, isSaving = isSaving, onClick = onSave)
    }
}

/** The Save action: dims while disabled, swaps for a spinner while saving, and gives a subtle
 * press-scale so the tap feels acknowledged even before the save completes. */
@Composable
private fun SaveAction(
    canSave: Boolean,
    isSaving: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalReducedMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = if (reduceMotion) snap() else tween(MotionDurations.SHORT, easing = MotionEasing.Standard),
        label = "saveScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (canSave) 1f else 0.4f,
        animationSpec = if (reduceMotion) snap() else tween(MotionDurations.SHORT, easing = MotionEasing.Standard),
        label = "saveAlpha",
    )

    Box(modifier = modifier.size(48.dp), contentAlignment = Alignment.Center) {
        if (isSaving) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        } else {
            IconButton(
                onClick = onClick,
                enabled = canSave,
                interactionSource = interactionSource,
                modifier = Modifier.scale(scale),
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Save expense",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                )
            }
        }
    }
}

@Composable
private fun AddExpenseContent(
    uiState: AddExpenseUiState,
    actions: AddExpenseActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        AmountField(
            amountText = uiState.amountText,
            amountError = uiState.amountError,
            onAmountChange = actions::onAmountChange,
        )
        CategoryPicker(
            selected = uiState.selectedCategory,
            onCategoryChange = actions::onCategoryChange,
        )
        OutlinedTextField(
            value = uiState.note,
            onValueChange = actions::onNoteChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Note (optional)") },
            singleLine = true,
        )
        DateSection(
            date = uiState.date,
            onDateChange = actions::onDateChange,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun AmountField(
    amountText: String,
    amountError: String?,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = amountText,
        onValueChange = onAmountChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text("Amount") },
        placeholder = {
            Text(text = "0.00", style = MaterialTheme.typography.displayMedium)
        },
        textStyle = MaterialTheme.typography.displayMedium.copy(textAlign = TextAlign.Center),
        singleLine = true,
        isError = amountError != null,
        supportingText = {
            if (amountError != null) {
                Text(text = amountError)
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

@Composable
private fun CategoryPicker(
    selected: ExpenseCategory,
    onCategoryChange: (ExpenseCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val categories = ExpenseCategory.entries
    val columns = 4
    val tileHeight = 80.dp
    val spacing = 8.dp
    val rows = (categories.size + columns - 1) / columns
    val gridHeight = tileHeight * rows + spacing * (rows - 1)

    Column(modifier = modifier) {
        Text(
            text = "Category",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
            userScrollEnabled = false,
        ) {
            items(items = categories, key = { it.name }) { category ->
                CategoryTile(
                    category = category,
                    selected = category == selected,
                    onClick = { onCategoryChange(category) },
                    modifier = Modifier.height(tileHeight),
                )
            }
        }
    }
}

@Composable
private fun CategoryTile(
    category: ExpenseCategory,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalReducedMotion.current
    val categoryColor = category.color()
    val background by animateColorAsState(
        targetValue = if (selected) categoryColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = if (reduceMotion) snap() else tween(MotionDurations.SHORT, easing = MotionEasing.Standard),
        label = "categoryTileBackground",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) categoryColor else Color.Transparent,
        animationSpec = if (reduceMotion) snap() else tween(MotionDurations.SHORT, easing = MotionEasing.Standard),
        label = "categoryTileBorder",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(background)
            .border(width = 1.5.dp, color = borderColor, shape = MaterialTheme.shapes.medium)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics(mergeDescendants = true) {}
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = category.icon(),
            contentDescription = null,
            tint = categoryColor,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSection(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now() }

    Column(modifier = modifier) {
        Text(
            text = "Date",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = date.toRelativeOrFormatted(today),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = date == today,
                onClick = { onDateChange(today) },
                label = { Text("Today") },
            )
            FilterChip(
                selected = date == today.minusDays(1),
                onClick = { onDateChange(today.minusDays(1)) },
                label = { Text("Yesterday") },
            )
            TextButton(onClick = { showDatePicker = true }) {
                Text("Choose date")
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
