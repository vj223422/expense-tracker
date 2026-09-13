package com.expensetracker.app.upi

object UpiPaymentParser {
    fun parse(response: String?): UpiPaymentResult {
        if (response.isNullOrBlank()) {
            return UpiPaymentResult(
                status = "CANCELLED",
                transactionId = null,
                approvalReference = null,
                rawResponse = response,
            )
        }

        val values = response.split('&')
            .mapNotNull { part ->
                val index = part.indexOf('=')
                if (index <= 0) null
                else part.substring(0, index).trim().lowercase() to part.substring(index + 1).trim()
            }
            .toMap()

        return UpiPaymentResult(
            status = values["status"] ?: "FAILED",
            transactionId = values["txnref"] ?: values["txn id"] ?: values["transactionid"],
            approvalReference = values["approvalrefno"],
            rawResponse = response,
        )
    }
}
