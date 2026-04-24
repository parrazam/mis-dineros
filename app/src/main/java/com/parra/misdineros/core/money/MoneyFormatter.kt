package com.parra.misdineros.core.money

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object MoneyFormatter {

    fun format(amountMinor: Long, currencyCode: String, locale: Locale = Locale.getDefault()): String {
        return runCatching {
            val currency = Currency.getInstance(currencyCode)
            val fractionDigits = currency.defaultFractionDigits
            val amount = if (fractionDigits > 0) {
                amountMinor.toDouble() / Math.pow(10.0, fractionDigits.toDouble())
            } else {
                amountMinor.toDouble()
            }
            val formatter = NumberFormat.getCurrencyInstance(locale).apply {
                this.currency = currency
            }
            formatter.format(amount)
        }.getOrElse { "$amountMinor $currencyCode" }
    }

    fun parseToMinor(text: String, currencyCode: String): Long? {
        return runCatching {
            val currency = Currency.getInstance(currencyCode)
            val fractionDigits = currency.defaultFractionDigits
            val cleaned = text.replace(",", ".").replace("[^0-9.]".toRegex(), "")
            val amount = cleaned.toDouble()
            if (fractionDigits > 0) {
                (amount * Math.pow(10.0, fractionDigits.toDouble())).toLong()
            } else {
                amount.toLong()
            }
        }.getOrNull()
    }
}
