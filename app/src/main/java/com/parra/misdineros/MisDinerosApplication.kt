package com.parra.misdineros

import android.app.Application
import androidx.work.Configuration
import com.parra.misdineros.domain.repository.SettingsRepository
import com.parra.misdineros.notifications.NotificationChannelFactory
import com.parra.misdineros.notifications.NotificationScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MisDinerosApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory
    @Inject lateinit var notificationScheduler: NotificationScheduler
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        NotificationChannelFactory.createChannels(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val settings = settingsRepository.observe().first()
            notificationScheduler.schedule(settings.notificationHour, settings.notificationsEnabled)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
