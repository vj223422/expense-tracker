package com.expensetracker.app.data.model

/**
 * Zero Android/Compose deps by design (see android-skills:android-dev, ":core:model" rule) — the
 * icon/color mapping for each entry lives in core/designsystem/CategoryStyle.kt instead.
 */
enum class ExpenseCategory(val displayName: String) {
    FOOD("Food & Dining"),
    TRANSPORT("Transport"),
    SHOPPING("Shopping"),
    BILLS("Bills & Utilities"),
    ENTERTAINMENT("Entertainment"),
    HEALTH("Health"),
    EDUCATION("Education"),
    OTHER("Other"),
}
