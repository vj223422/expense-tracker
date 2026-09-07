package com.expensetracker.app.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.expensetracker.app.core.theme.LocalExtendedColors
import com.expensetracker.app.data.model.ExpenseCategory

/** Maps the (Android-free) data-layer enum to Compose visuals — see data/model/ExpenseCategory. */
fun ExpenseCategory.icon(): ImageVector = when (this) {
    ExpenseCategory.FOOD -> Icons.Filled.Restaurant
    ExpenseCategory.TRANSPORT -> Icons.Filled.DirectionsCar
    ExpenseCategory.SHOPPING -> Icons.Filled.ShoppingBag
    ExpenseCategory.BILLS -> Icons.Filled.ReceiptLong
    ExpenseCategory.ENTERTAINMENT -> Icons.Filled.Movie
    ExpenseCategory.HEALTH -> Icons.Filled.LocalHospital
    ExpenseCategory.EDUCATION -> Icons.Filled.School
    ExpenseCategory.OTHER -> Icons.Filled.Category
}

@Composable
@ReadOnlyComposable
fun ExpenseCategory.color(): Color {
    val palette = LocalExtendedColors.current.category
    return when (this) {
        ExpenseCategory.FOOD -> palette.food
        ExpenseCategory.TRANSPORT -> palette.transport
        ExpenseCategory.SHOPPING -> palette.shopping
        ExpenseCategory.BILLS -> palette.bills
        ExpenseCategory.ENTERTAINMENT -> palette.entertainment
        ExpenseCategory.HEALTH -> palette.health
        ExpenseCategory.EDUCATION -> palette.education
        ExpenseCategory.OTHER -> palette.other
    }
}
