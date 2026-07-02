package com.parra.misdineros.notifications

import com.parra.misdineros.notifications.NotificationScheduler.Companion.computeInitialDelay
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

class NotificationSchedulerTest {

    private val madrid = ZoneId.of("Europe/Madrid")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int = 0, nano: Int = 0): ZonedDateTime =
        ZonedDateTime.of(year, month, day, hour, minute, second, nano, madrid)

    @Test
    fun `hora futura hoy programa para hoy`() {
        val delay = computeInitialDelay(at(2026, 7, 2, 10, 0), 20, 0)
        assertEquals(Duration.ofHours(10).toMillis(), delay)
    }

    @Test
    fun `hora ya pasada programa para manana`() {
        val delay = computeInitialDelay(at(2026, 7, 2, 21, 30), 20, 0)
        assertEquals(Duration.ofHours(22).plusMinutes(30).toMillis(), delay)
    }

    @Test
    fun `minuto exacto actual programa para manana no para ahora`() {
        val delay = computeInitialDelay(at(2026, 7, 2, 20, 0), 20, 0)
        assertEquals(Duration.ofDays(1).toMillis(), delay)
    }

    @Test
    fun `medianoche como hora objetivo`() {
        val delay = computeInitialDelay(at(2026, 7, 2, 12, 0), 0, 0)
        assertEquals(Duration.ofHours(12).toMillis(), delay)
    }

    @Test
    fun `descarta segundos y nanos del instante actual`() {
        val delay = computeInitialDelay(at(2026, 7, 2, 19, 59, second = 30, nano = 500), 20, 0)
        // 29,9999995 s hasta las 20:00:00.0 → 29 999 ms (ChronoUnit.MILLIS trunca)
        assertEquals(29_999L, delay)
    }

    @Test
    fun `cambio DST de primavera el dia dura 23 horas reales`() {
        // Madrid adelanta el reloj el 29-03-2026 (02:00 → 03:00): de las 20:00 del 28
        // a las 20:00 del 29 pasan 23 h reales, no 24.
        val delay = computeInitialDelay(at(2026, 3, 28, 20, 0), 20, 0)
        assertEquals(Duration.ofHours(23).toMillis(), delay)
    }

    @Test
    fun `cambio DST de otono el dia dura 25 horas reales`() {
        // Madrid atrasa el reloj el 25-10-2026 (03:00 → 02:00): día de 25 h reales.
        val delay = computeInitialDelay(at(2026, 10, 24, 20, 0), 20, 0)
        assertEquals(Duration.ofHours(25).toMillis(), delay)
    }

    @Test
    fun `hora y minuto fuera de rango se acotan en vez de lanzar`() {
        // P. ej. un backup importado con valores corruptos: 25:‑5 se trata como 23:00.
        val delay = computeInitialDelay(at(2026, 7, 2, 10, 0), 25, -5)
        assertEquals(Duration.ofHours(13).toMillis(), delay)
    }
}
