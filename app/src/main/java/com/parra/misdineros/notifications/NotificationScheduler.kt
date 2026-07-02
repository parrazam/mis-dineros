package com.parra.misdineros.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Programa los avisos diarios como trabajo one-time re-encolado cada día (por el propio worker
 * al terminar y en cada arranque de la app), NO como PeriodicWorkRequest: el trabajo periódico
 * se re-ancla a la hora en que corrió de verdad la última ejecución (Doze la aplaza), así que
 * deriva de forma permanente lejos de la hora configurada.
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val RENEWAL_WORK_NAME = "renewal_reminder"
        const val SUMMARY_WORK_NAME = "monthly_summary"

        /**
         * Milisegundos reales desde [now] hasta la próxima ocurrencia de hh:mm (mañana si ya pasó
         * hoy). Se calcula sobre ZonedDateTime para que el retardo sea tiempo transcurrido y no
         * reloj de pared (los días de cambio DST duran 23/25 h). Hora y minuto se acotan porque
         * pueden venir de un backup importado sin validar y withHour/withMinute lanzarían.
         */
        fun computeInitialDelay(now: ZonedDateTime, hour: Int, minute: Int): Long {
            var target = now
                .withHour(hour.coerceIn(0, 23))
                .withMinute(minute.coerceIn(0, 59))
                .withSecond(0)
                .withNano(0)
            if (!target.isAfter(now)) target = target.plusDays(1)
            return ChronoUnit.MILLIS.between(now, target)
        }
    }

    private val workManager get() = WorkManager.getInstance(context)

    fun schedule(hour: Int, minute: Int, enabled: Boolean) {
        if (!enabled) {
            workManager.cancelUniqueWork(RENEWAL_WORK_NAME)
            workManager.cancelUniqueWork(SUMMARY_WORK_NAME)
            return
        }
        scheduleRenewal(hour, minute)
        scheduleSummary(hour, minute)
    }

    fun scheduleRenewal(hour: Int, minute: Int) =
        enqueueNext<RenewalReminderWorker>(RENEWAL_WORK_NAME, hour, minute)

    fun scheduleSummary(hour: Int, minute: Int) =
        enqueueNext<MonthlySummaryWorker>(SUMMARY_WORK_NAME, hour, minute)

    private inline fun <reified W : ListenableWorker> enqueueNext(name: String, hour: Int, minute: Int) {
        workManager.enqueueUniqueWork(
            name,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<W>()
                .setInitialDelay(computeInitialDelay(ZonedDateTime.now(), hour, minute), TimeUnit.MILLISECONDS)
                .build(),
        )
    }
}
