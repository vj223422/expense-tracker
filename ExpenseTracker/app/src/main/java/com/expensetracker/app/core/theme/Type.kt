package com.expensetracker.app.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.unit.sp

// Slightly more compact than the Material 3 default scale so text feels balanced
// across phones while keeping the existing hierarchy and accessibility semantics.
val ExpenseTrackerTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontSize = 55.sp),
        displayMedium = displayMedium.copy(fontSize = 43.sp),
        displaySmall = displaySmall.copy(fontSize = 34.sp),
        headlineLarge = headlineLarge.copy(fontSize = 30.sp),
        headlineMedium = headlineMedium.copy(fontSize = 26.sp),
        headlineSmall = headlineSmall.copy(fontSize = 22.sp),
        titleLarge = titleLarge.copy(fontSize = 20.sp),
        titleMedium = titleMedium.copy(fontSize = 15.sp),
        titleSmall = titleSmall.copy(fontSize = 13.sp),
        bodyLarge = bodyLarge.copy(fontSize = 14.sp),
        bodyMedium = bodyMedium.copy(fontSize = 12.sp),
        bodySmall = bodySmall.copy(fontSize = 11.sp),
        labelLarge = labelLarge.copy(fontSize = 13.sp),
        labelMedium = labelMedium.copy(fontSize = 11.sp),
        labelSmall = labelSmall.copy(fontSize = 10.sp),
    )
}
