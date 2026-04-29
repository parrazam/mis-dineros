package com.parra.misdineros.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val RENEWAL_WORK_NAME = "renewal_reminder"
        const val SUMMARY_WORK_NAME = "monthly_summary"
    }

    private val workManager get() = WorkManager.getInstance(context)

    fun schedule(hour: Int, minute: Int, enabled: Boolean) {
        if (!enabled) {
            workManager.cancelUniqueWork(RENEWAL_WORK_NAME)
            workManager.cancelUniqueWork(SUMMARY_WORK_NAME)
            return
        }
        val delay = computeInitialDelay(hour, minute)
        workManager.enqueueUniquePeriodicWork(
            RENEWAL_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<RenewalReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build(),
        )
        workManager.enqueueUniquePeriodicWork(
            SUMMARY_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<MonthlySummaryWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build(),
        )
    }

    private fun computeInitialDelay(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return ChronoUnit.MILLIS.between(now, target)
    }
}
