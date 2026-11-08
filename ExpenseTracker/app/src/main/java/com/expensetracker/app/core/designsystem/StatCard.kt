package com.expensetracker.app.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Consistent premium statistic tile shared by dashboard and analytics surfaces. */
@Composable
fun StatCard(
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    valueContent: @Composable () -> Unit,
) {
    Surface(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(7.dp))
            valueContent()
        }
    }
}
