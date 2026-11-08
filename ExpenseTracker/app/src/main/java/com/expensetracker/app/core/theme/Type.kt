package com.expensetracker.app.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Refined type scale: clear numeric hierarchy and calm, readable supporting copy.
val ExpenseTrackerTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontSize = 54.sp, fontWeight = FontWeight.Bold),
        displayMedium = displayMedium.copy(fontSize = 42.sp, fontWeight = FontWeight.Bold),
        displaySmall = displaySmall.copy(fontSize = 34.sp, fontWeight = FontWeight.Bold),
        headlineLarge = headlineLarge.copy(fontSize = 30.sp, fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontSize = 25.sp, fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontSize = 21.sp, fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontSize = 19.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(fontSize = 14.sp),
        bodyMedium = bodyMedium.copy(fontSize = 12.sp),
        bodySmall = bodySmall.copy(fontSize = 11.sp),
        labelLarge = labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
        labelMedium = labelMedium.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
        labelSmall = labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
    )
}
