package com.expensetracker.app.upi

data class UpiPaymentResult(
    val status: String,
    val transactionId: String?,
    val approvalReference: String?,
    val rawResponse: String?,
) {
    val isSuccess: Boolean
        get() = status.equals("success", ignoreCase = true)
}
