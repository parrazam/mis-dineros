package com.parra.misdineros.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.parra.misdineros.R

object NotificationChannelFactory {
    const val CHANNEL_RENEWALS = "renewals"
    const val CHANNEL_SUMMARY = "monthly_summary"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannels(
                listOf(
                    NotificationChannel(
                        CHANNEL_RENEWALS,
                        context.getString(R.string.notif_channel_renewals),
                        NotificationManager.IMPORTANCE_DEFAULT,
                    ),
                    NotificationChannel(
                        CHANNEL_SUMMARY,
                        context.getString(R.string.notif_channel_summary),
                        NotificationManager.IMPORTANCE_LOW,
                    ),
                ),
            )
        }
    }
}
