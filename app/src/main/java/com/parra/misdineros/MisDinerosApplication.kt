package com.parra.misdineros

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.Configuration
import com.parra.misdineros.backup.MisDinerosBackupAgent
import com.parra.misdineros.domain.repository.SettingsRepository
import com.parra.misdineros.domain.usecase.AdvanceDueRenewalsUseCase
import com.parra.misdineros.notifications.NotificationChannelFactory
import com.parra.misdineros.notifications.NotificationScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineExceptionHandler
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
    @Inject lateinit var advanceDueRenewals: AdvanceDueRenewalsUseCase

    /**
     * Sin handler, una excepción en el arranque en segundo plano (migración de Room fallida,
     * base de datos ilegible…) llega al handler por defecto del hilo y cierra la app en cada
     * inicio. Aquí solo se registra: la app arranca aunque este paso falle y el usuario puede
     * al menos exportar sus datos o corregir el problema.
     */
    private val startupExceptionHandler = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "Fallo en el arranque en segundo plano", e)
    }

    override fun onCreate() {
        super.onCreate()
        NotificationChannelFactory.createChannels(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO + startupExceptionHandler).launch {
            // Avanza las renovaciones vencidas antes de programar las notificaciones, de modo que
            // las fechas estén actualizadas aunque las notificaciones estén desactivadas (en ese
            // caso el worker periódico no se programa).
            advanceDueRenewals()
            val settings = settingsRepository.observe().first()
            // Asegura que el mirror SharedPreferences que lee MisDinerosBackupAgent
            // existe desde el primer arranque, aunque el usuario no haya tocado ajustes.
            getSharedPreferences(MisDinerosBackupAgent.PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(MisDinerosBackupAgent.KEY_ENABLED, settings.autoBackupEnabled)
                .apply()
            notificationScheduler.schedule(
                settings.notificationHour,
                settings.notificationMinute,
                settings.notificationsEnabled
            )
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private companion object {
        const val TAG = "MisDinerosApplication"
    }
}
