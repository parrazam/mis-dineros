package com.parra.misdineros.notifications

import android.annotation.SuppressLint
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.LocalDate

@HiltWorker
class MonthlySummaryWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val subscriptionRepository: SubscriptionRepository,
    private val settingsRepository: SettingsRepository,
    private val calcMonthlySpend: CalcMonthlySpendUseCase,
    private val scheduler: NotificationScheduler,
) : CoroutineWorker(appContext, workerParams) {

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        val settings = try {
            settingsRepository.observe().first()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Sin settings no se puede re-anclar la cadena; reintenta este mismo trabajo con backoff
            // en vez de fallar (un fallo terminal dejaría de notificar hasta el próximo arranque).
            return Result.retry()
        }
        if (!settings.notificationsEnabled) return Result.success()

        try {
            if (!settings.monthlySummaryEnabled) return Result.success()
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
        } finally {
            // Re-encola la siguiente ejecución diaria como última instrucción: el REPLACE puede
            // cancelar esta instancia aún RUNNING, inocuo porque ya no queda trabajo pendiente.
            // Si nos han parado desde fuera (cancel/REPLACE de Ajustes, o el sistema, que ya
            // re-encola él mismo), no re-programar: pisaríamos la programación nueva con la vieja.
            if (!isStopped) {
                scheduler.scheduleSummary(settings.notificationHour, settings.notificationMinute)
            }
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1000
    }
}
