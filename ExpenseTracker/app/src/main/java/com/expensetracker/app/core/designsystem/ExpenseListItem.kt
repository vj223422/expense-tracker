package com.expensetracker.app.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.expensetracker.app.core.util.formatAsCurrency
import com.expensetracker.app.data.model.Expense
import kotlinx.coroutines.launch

@Composable
fun ExpenseListItem(expense: Expense, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(expense.category.color().copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = expense.category.icon(),
                contentDescription = null,
                tint = expense.category.color(),
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.note.ifBlank { expense.category.displayName },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = expense.category.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = "-" + expense.amountMinor.formatAsCurrency(), style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * Swipe end-to-start to delete, revealing a danger-tinted background with a trash icon.
 *
 * [onDelete] is suspend and returns whether the delete actually succeeded — the swipe gesture
 * commits visually the instant the user releases (returning `true` from confirmValueChange), but
 * the real delete happens afterwards and can fail (e.g. a local storage error). Without awaiting
 * that result, a failed delete would leave the row stuck fully swiped-away on screen while the
 * expense is still sitting in the database. On failure, the swipe is reset back to visible.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteExpenseItem(
    expense: Expense,
    onDelete: suspend (Expense) -> Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    var deleteFailed by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                scope.launch { if (!onDelete(expense)) deleteFailed = true }
                true
            } else {
                false
            }
        },
    )
    LaunchedEffect(deleteFailed) {
        if (deleteFailed) {
            dismissState.reset()
            deleteFailed = false
        }
    }
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete expense",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(end = 24.dp).size(24.dp),
                )
            }
        },
    ) {
        ExpenseListItem(expense = expense, onClick = onClick)
    }
}
