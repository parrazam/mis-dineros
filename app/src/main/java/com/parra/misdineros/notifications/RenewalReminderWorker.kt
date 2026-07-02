package com.parra.misdineros.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.parra.misdineros.MainActivity
import com.parra.misdineros.R
import com.parra.misdineros.core.money.MoneyFormatter
import com.parra.misdineros.domain.model.Subscription
import com.parra.misdineros.domain.repository.SettingsRepository
import com.parra.misdineros.domain.repository.SubscriptionRepository
import com.parra.misdineros.domain.usecase.AdvanceDueRenewalsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@HiltWorker
class RenewalReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val subscriptionRepository: SubscriptionRepository,
    private val settingsRepository: SettingsRepository,
    private val advanceDueRenewals: AdvanceDueRenewalsUseCase,
    private val scheduler: NotificationScheduler,
) : CoroutineWorker(appContext, workerParams) {

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        val settings = try {
            // Avanza primero las renovaciones vencidas para evaluar las notificaciones con fechas frescas.
            advanceDueRenewals()
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
            val today = LocalDate.now()
            subscriptionRepository.observeAll().first()
                .filter { !it.isPaused }
                .forEach { sub ->
                    val notifyDays = (sub.notifyDaysBefore ?: settings.defaultNotifyDaysBefore).toLong()
                    val targetDate = sub.nextRenewalDate.minusDays(notifyDays)
                    // Margen de ±1 día para absorber retrasos de WorkManager/Doze
                    val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(targetDate, today)
                    if (daysDiff in 0..1 && !sub.nextRenewalDate.isBefore(today)) {
                        sendNotification(sub)
                    }
                }
            return Result.success()
        } finally {
            // Re-encola la siguiente ejecución diaria como última instrucción: el REPLACE puede
            // cancelar esta instancia aún RUNNING, inocuo porque ya no queda trabajo pendiente.
            // Si nos han parado desde fuera (cancel/REPLACE de Ajustes, o el sistema, que ya
            // re-encola él mismo), no re-programar: pisaríamos la programación nueva con la vieja.
            // No aplica al lanzamiento puntual del botón de prueba, que no debe tocar la cadena.
            if (!isStopped && !inputData.getBoolean(KEY_ONE_SHOT, false)) {
                scheduler.scheduleRenewal(settings.notificationHour, settings.notificationMinute)
            }
        }
    }

    companion object {
        /** Marca un lanzamiento manual (botón de prueba) que no debe re-anclar la cadena diaria. */
        const val KEY_ONE_SHOT = "one_shot"
    }

    @SuppressLint("MissingPermission")
    private fun sendNotification(sub: Subscription) {
        val notifManager = NotificationManagerCompat.from(appContext)
        if (!notifManager.areNotificationsEnabled()) return

        val intent = Intent(Intent.ACTION_VIEW, "misdineros://subscription/${sub.id}".toUri()).apply {
            setClass(appContext, MainActivity::class.java)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            sub.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val dateStr = sub.nextRenewalDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        val amountStr = MoneyFormatter.format(sub.amountMinor, sub.currencyCode)

        val notification = NotificationCompat.Builder(appContext, NotificationChannelFactory.CHANNEL_RENEWALS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(appContext.getString(R.string.notif_renewal_title, sub.name))
            .setContentText(appContext.getString(R.string.notif_renewal_body, dateStr, amountStr))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notifManager.notify(sub.id.hashCode(), notification)
    }
}
