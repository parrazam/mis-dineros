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
    val billingAnchorDay: Int,
    val categoryId: String,
    val isPaused: Boolean,
    val notifyDaysBefore: Int?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    /** Importe normalizado a un mes. Las anuales se dividen entre 12 con redondeo, no truncado. */
    val monthlyAmountMinor: Long
        get() = when (billingCycle) {
            BillingCycle.MONTHLY -> amountMinor
            BillingCycle.ANNUAL -> Math.round(amountMinor / 12.0)
        }

    /** Importe normalizado a un año, exacto: las anuales no pasan por la división. */
    val annualAmountMinor: Long
        get() = when (billingCycle) {
            BillingCycle.MONTHLY -> amountMinor * 12
            BillingCycle.ANNUAL -> amountMinor
        }
}
