package com.expensetracker.app.feature.budgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.core.designsystem.AnimatedAmountText
import com.expensetracker.app.core.designsystem.CategoryProgressRow
import com.expensetracker.app.core.designsystem.EmptyState
import com.expensetracker.app.core.designsystem.SectionHeader
import com.expensetracker.app.core.theme.LocalExtendedColors
import com.expensetracker.app.core.theme.LocalReducedMotion
import com.expensetracker.app.core.theme.MotionDurations
import com.expensetracker.app.core.theme.MotionEasing
import com.expensetracker.app.core.util.formatAsCurrency
import com.expensetracker.app.core.util.parseAmountToMinorUnits
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@Composable
fun BudgetsScreen(viewModel: BudgetsViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BudgetsContent(uiState = uiState, actions = viewModel)
}

@Composable
private fun BudgetsContent(
    uiState: BudgetsUiState,
    actions: BudgetsActions,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item(key = "overall_card") {
                    OverallLimitCard(
                        limitMinor = uiState.overallLimitMinor,
                        spentMinor = uiState.overallSpentMinor,
                        progress = uiState.overallProgress,
                        onClick = actions::onEditOverall,
                    )
                }
                item(key = "category_header") {
                    SectionHeader(
                        title = "Category limits",
                        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                    )
                }
                if (uiState.categorySpends.isEmpty()) {
                    item(key = "empty_categories") {
                        EmptyState(
                            icon = Icons.Filled.Category,
                            title = "No categories yet",
                            message = "Add an expense to start tracking category budgets.",
                        )
                    }
                } else {
                    items(
                        items = uiState.categorySpends,
                        key = { it.category.name },
                        contentType = { "category_row" },
                    ) { categorySpend ->
                        CategoryProgressRow(
                            categorySpend = categorySpend,
                            onClick = { actions.onEditCategory(categorySpend.category) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }

    if (uiState.editingTarget != null) {
        EditLimitDialog(uiState = uiState, actions = actions)
    }
}

/** Mirrors CategoryProgressRow's color language for the one row that has no CategorySpend of
 * its own (the overall monthly limit spans every category). */
@Composable
private fun OverallLimitCard(
    limitMinor: Long?,
    spentMinor: Long,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extended = LocalExtendedColors.current
    val reduceMotion = LocalReducedMotion.current
    val targetProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(
            durationMillis = if (reduceMotion) 0 else MotionDurations.MEDIUM,
            easing = MotionEasing.Standard,
        ),
        label = "overallProgress",
    )
    val barColor = when {
        limitMinor == null -> MaterialTheme.colorScheme.primary
        progress >= 1f -> extended.danger
        progress >= 0.8f -> extended.warning
        else -> extended.safe
    }

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .semantics(mergeDescendants = true) {},
        ) {
            Text(text = "Overall monthly limit", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                AnimatedAmountText(
                    amountMinor = spentMinor,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (limitMinor != null) "of ${limitMinor.formatAsCurrency()}" else "Not set",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.small),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }
    }
}

@Composable
private fun EditLimitDialog(
    uiState: BudgetsUiState,
    actions: BudgetsActions,
) {
    val target = uiState.editingTarget ?: return
    val currentLimitMinor = when (target) {
        EditTarget.Overall -> uiState.overallLimitMinor
        is EditTarget.Category -> {
            val category = target.category
            uiState.categorySpends.firstOrNull { it.category == category }?.limitMinor
        }
    }
    val title = when (target) {
        EditTarget.Overall -> "Overall limit"
        is EditTarget.Category -> target.category.displayName
    }

    var amountText by rememberSaveable(target) {
        mutableStateOf(
            currentLimitMinor?.let { String.format(Locale.US, "%.2f", it / 100.0) } ?: "",
        )
    }

    val parsedMinor = amountText.parseAmountToMinorUnits()
    val isValid = parsedMinor != null && parsedMinor > 0

    AlertDialog(
        onDismissRequest = actions::onDismissEdit,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Monthly limit") },
                singleLine = true,
                isError = amountText.isNotBlank() && !isValid,
                supportingText = {
                    if (amountText.isNotBlank() && !isValid) {
                        Text("Enter an amount greater than zero")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { parsedMinor?.let(actions::onSaveLimit) },
                enabled = isValid,
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (currentLimitMinor != null) {
                    TextButton(onClick = actions::onClearLimit) {
                        Text("Clear limit")
                    }
                }
                TextButton(onClick = actions::onDismissEdit) {
                    Text("Cancel")
                }
            }
        },
    )
}
