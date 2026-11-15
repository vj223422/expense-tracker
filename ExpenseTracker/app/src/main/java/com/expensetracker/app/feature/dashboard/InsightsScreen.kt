package com.expensetracker.app.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.core.designsystem.CategoryDonutChart
import com.expensetracker.app.core.designsystem.DonutSegment
import com.expensetracker.app.core.designsystem.color
import com.expensetracker.app.core.util.formatAsCurrency
import org.koin.androidx.compose.koinViewModel

@Composable
fun InsightsScreen(viewModel: DashboardViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Spending Insights", style = MaterialTheme.typography.headlineSmall)
            Text(state.yearMonth.toString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                InsightMetric("Income", state.totalIncomeMinor.formatAsCurrency(), Color(0xFF27833F))
                InsightMetric("Expenses", state.totalSpentMinor.formatAsCurrency(), MaterialTheme.colorScheme.error)
                InsightMetric("Budget", state.effectiveBudgetMinor?.formatAsCurrency() ?: "Not set", MaterialTheme.colorScheme.primary)
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Category breakdown", style = MaterialTheme.typography.titleLarge)
                if (state.categorySpends.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) {
                        Text("Add expenses to see your insights.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    CategoryDonutChart(
                        segments = state.categorySpends.take(8).map { DonutSegment(it.spentMinor.toFloat(), it.category.color(), it.category.displayName) },
                        modifier = Modifier.fillMaxWidth().size(220.dp),
                        strokeWidth = 22.dp,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.totalSpentMinor.formatAsCurrency(), style = MaterialTheme.typography.titleLarge)
                            Text("Total spent", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    state.categorySpends.take(8).forEach { item ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = item.category.color(), modifier = Modifier.size(10.dp)) {}
                            Text(item.category.displayName, Modifier.weight(1f).padding(start = 10.dp), style = MaterialTheme.typography.bodyMedium)
                            Text(item.spentMinor.formatAsCurrency(), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightMetric(label: String, value: String, tint: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, color = tint)
    }
}
