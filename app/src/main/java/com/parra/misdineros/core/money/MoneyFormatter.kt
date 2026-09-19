package com.parra.misdineros.core.money

import java.math.BigDecimal
import java.math.RoundingMode
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

    /**
     * Convierte el texto tecleado por el usuario a unidades menores (céntimos) de [currencyCode].
     *
     * Reglas:
     *  - Se ignoran todos los caracteres salvo dígitos, `,` y `.`.
     *  - Con un único separador, ese es el decimal ("4,35" y "4.35" → 435).
     *  - Con varios separadores, los grupos intermedios deben ser de 3 dígitos. Si son todos
     *    iguales, todos son de miles ("1.234.567" → 123456700). Si son distintos, el último es
     *    el decimal ("1.234,56" → 123456). Cualquier otra combinación es inválida.
     *  - Se redondea a la fracción de la divisa con HALF_UP, nunca se trunca: la aritmética
     *    en `Double` daba 434 para "4,35" porque 4.35 × 100 = 434.999… .
     *
     * Devuelve `null` si el texto no es un importe válido o desborda un `Long`.
     */
    fun parseToMinor(text: String, currencyCode: String): Long? = runCatching {
        val fractionDigits = Currency.getInstance(currencyCode).defaultFractionDigits
        if (fractionDigits < 0) return null
        val cleaned = text.filter { it.isDigit() || it == ',' || it == '.' }
        if (cleaned.none { it.isDigit() }) return null

        val separators = cleaned.indices.filter { !cleaned[it].isDigit() }
        val normalized = when (separators.size) {
            0 -> cleaned
            1 -> cleaned.replace(',', '.')
            else -> {
                val last = separators.last()
                val groupsOk = separators.zipWithNext().all { (a, b) -> b - a == 4 }
                if (!groupsOk) return null
                val allSame = separators.all { cleaned[it] == cleaned[last] }
                if (allSame) {
                    if (cleaned.length - last - 1 != 3) return null
                    cleaned.filter { it.isDigit() }
                } else {
                    cleaned.substring(0, last).filter { it.isDigit() } + "." + cleaned.substring(last + 1)
                }
            }
        }
        BigDecimal(normalized)
            .movePointRight(fractionDigits)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }.getOrNull()
}
