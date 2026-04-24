package com.parra.misdineros.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.parra.misdineros.R
import com.parra.misdineros.core.money.MoneyFormatter
import com.parra.misdineros.domain.repository.SettingsRepository
import com.parra.misdineros.domain.repository.SubscriptionRepository
import com.parra.misdineros.domain.usecase.CalcMonthlySpendUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate

@HiltWorker
class MonthlySummaryWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val subscriptionRepository: SubscriptionRepository,
    private val settingsRepository: SettingsRepository,
    private val calcMonthlySpend: CalcMonthlySpendUseCase,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.observe().first()
        if (!settings.notificationsEnabled || !settings.monthlySummaryEnabled) return Result.success()
        if (LocalDate.now().dayOfMonth != 1) return Result.success()

        val notifManager = NotificationManagerCompat.from(appContext)
        if (!notifManager.areNotificationsEnabled()) return Result.success()

        val subscriptions = subscriptionRepository.observeAll().first()
        val monthlyTotal = calcMonthlySpend(subscriptions, settings.globalCurrencyCode)
        val activeCount = subscriptions.count { !it.isPaused }

        val notification = NotificationCompat.Builder(appContext, NotificationChannelFactory.CHANNEL_SUMMARY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(appContext.getString(R.string.notif_summary_title))
            .setContentText(
                appContext.getString(
                    R.string.notif_summary_body,
                    MoneyFormatter.format(monthlyTotal, settings.globalCurrencyCode),
                    activeCount,
                )
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notifManager.notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    companion object {
        private const val NOTIFICATION_ID = 1000
    }
}
