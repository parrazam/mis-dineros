package com.parra.misdineros.domain.model

import java.time.LocalDate

/**
 * [amountMinor] es siempre el precio completo del servicio. Si se comparte, [sharedWith] es el
 * número total de personas que lo pagan (contando al usuario) y los totales usan
 * [myMonthlyShareMinor] / [myAnnualShareMinor]; `1` significa que no es compartida.
 */
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
    val sharedWith: Int = 1,
) {
    val isShared: Boolean
        get() = sharedWith > 1

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

    /** Parte del usuario en cada cobro (por ciclo): es [amountMinor] si no se comparte. */
    val myShareMinor: Long
        get() = shareOf(amountMinor, sharedWith)

    /**
     * Parte del usuario en el importe mensual. Una sola división desde [amountMinor] para no
     * encadenar dos redondeos (anual → mensual → parte). Los importes son positivos, así que
     * `Math.round` equivale a HALF_UP.
     */
    val myMonthlyShareMinor: Long
        get() = when (billingCycle) {
            BillingCycle.MONTHLY -> shareOf(amountMinor, sharedWith)
            BillingCycle.ANNUAL -> Math.round(amountMinor / (12.0 * sharedWith))
        }

    /** Parte del usuario en el importe anual, calculada desde el anual exacto. */
    val myAnnualShareMinor: Long
        get() = shareOf(annualAmountMinor, sharedWith)

    companion object {
        /** Máximo de personas con las que se puede compartir; acota también el backup importado. */
        const val MAX_SHARED_WITH = 10

        /** Reparto a partes iguales de [amountMinor] entre [sharedWith] personas (HALF_UP). */
        fun shareOf(amountMinor: Long, sharedWith: Int): Long = Math.round(amountMinor / sharedWith.toDouble())
    }
}
