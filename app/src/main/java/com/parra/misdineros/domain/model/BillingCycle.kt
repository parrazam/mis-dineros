package com.parra.misdineros.domain.model

enum class BillingCycle {
    MONTHLY,
    ANNUAL;

    fun toMonthlyFactor(): Double = when (this) {
        MONTHLY -> 1.0
        ANNUAL -> 1.0 / 12.0
    }
}
