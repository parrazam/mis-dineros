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
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
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
) : CoroutineWorker(appContext, workerParams) {

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        val settings = settingsRepository.observe().first()
        if (!settings.notificationsEnabled) return Result.success()

        val today = LocalDate.now()
        subscriptionRepository.observeAll().first()
            .filter { !it.isPaused }
            .forEach { sub ->
                val notifyDays = (sub.notifyDaysBefore ?: settings.defaultNotifyDaysBefore).toLong()
                if (sub.nextRenewalDate.minusDays(notifyDays) == today) {
                    sendNotification(sub)
                }
            }

        return Result.success()
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
