package com.parra.misdineros.core.time

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object DateUtils {

    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun LocalDate.formatDisplay(locale: Locale = Locale.getDefault()): String =
        this.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

    fun LocalDate.daysUntil(): Long =
        this.toEpochDay() - LocalDate.now().toEpochDay()

    fun LocalDate.toIsoString(): String = this.format(isoFormatter)

    fun String.toLocalDate(): LocalDate = LocalDate.parse(this, isoFormatter)
}
