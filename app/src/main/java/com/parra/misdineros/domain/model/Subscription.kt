package com.parra.misdineros.domain.model

import java.time.LocalDate

data class Subscription(
    val id: String,
    val name: String,
    val iconRef: String,
    val amountMinor: Long,
    val currencyCode: String,
    val billingCycle: BillingCycle,
    val nextRenewalDate: LocalDate,
    val categoryId: String,
    val isPaused: Boolean,
    val notifyDaysBefore: Int?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val monthlyAmountMinor: Long
        get() = when (billingCycle) {
            BillingCycle.MONTHLY -> amountMinor
            BillingCycle.ANNUAL -> amountMinor / 12
        }
}
