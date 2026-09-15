package com.parra.misdineros.data.settings

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.parra.misdineros.backup.MisDinerosBackupAgent
import com.parra.misdineros.designsystem.theme.AppTheme
import com.parra.misdineros.domain.model.AppSettings
import com.parra.misdineros.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    private object Keys {
        val CURRENCY = stringPreferencesKey("global_currency")
        val NOTIFS_ENABLED = booleanPreferencesKey("notifs_enabled")
        val NOTIF_HOUR = intPreferencesKey("notif_hour")
        val NOTIF_MINUTE = intPreferencesKey("notif_minute")
        val NOTIFY_DAYS = intPreferencesKey("notify_days")
        val SUMMARY_ENABLED = booleanPreferencesKey("summary_enabled")
        val THEME = stringPreferencesKey("app_theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color_enabled")
        val AUTO_BACKUP = booleanPreferencesKey("auto_backup_enabled")
    }

    // SharedPreferences mirror para MisDinerosBackupAgent (acceso síncrono desde el agente).
    private val backupPrefs by lazy {
        context.getSharedPreferences(MisDinerosBackupAgent.PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Un fallo de lectura no debe tumbar a los observadores (MainViewModel colecta con
     * `Eagerly` y la Application lo lee en `onCreate`): se degrada a los valores por defecto.
     * La corrupción del fichero la resuelve antes el [ReplaceFileCorruptionHandler] de [create].
     */
    override fun observe(): Flow<AppSettings> = dataStore.data
        .catch { e ->
            if (e is IOException) {
                Log.e(TAG, "No se pudieron leer los ajustes; se usan los valores por defecto", e)
                emit(emptyPreferences())
            } else {
                throw e
            }
        }
        .map { prefs ->
            AppSettings(
                globalCurrencyCode = prefs[Keys.CURRENCY] ?: "EUR",
                notificationsEnabled = prefs[Keys.NOTIFS_ENABLED] ?: true,
                notificationHour = prefs[Keys.NOTIF_HOUR] ?: 9,
                notificationMinute = prefs[Keys.NOTIF_MINUTE] ?: 0,
                defaultNotifyDaysBefore = prefs[Keys.NOTIFY_DAYS] ?: 3,
                monthlySummaryEnabled = prefs[Keys.SUMMARY_ENABLED] ?: true,
                appTheme = prefs[Keys.THEME]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() }
                    ?: AppTheme.SYSTEM,
                dynamicColorEnabled = prefs[Keys.DYNAMIC_COLOR] ?: false,
                autoBackupEnabled = prefs[Keys.AUTO_BACKUP] ?: true,
            )
        }

    override suspend fun update(settings: AppSettings) {
        dataStore.edit { prefs ->
            prefs[Keys.CURRENCY] = settings.globalCurrencyCode
            prefs[Keys.NOTIFS_ENABLED] = settings.notificationsEnabled
            prefs[Keys.NOTIF_HOUR] = settings.notificationHour
            prefs[Keys.NOTIF_MINUTE] = settings.notificationMinute
            prefs[Keys.NOTIFY_DAYS] = settings.defaultNotifyDaysBefore
            prefs[Keys.SUMMARY_ENABLED] = settings.monthlySummaryEnabled
            prefs[Keys.THEME] = settings.appTheme.name
            prefs[Keys.DYNAMIC_COLOR] = settings.dynamicColorEnabled
            prefs[Keys.AUTO_BACKUP] = settings.autoBackupEnabled
        }
        // Mirror síncrono para que BackupAgent lo lea sin corrutinas.
        backupPrefs.edit()
            .putBoolean(MisDinerosBackupAgent.KEY_ENABLED, settings.autoBackupEnabled)
            .apply()
    }

    companion object {
        private const val TAG = "SettingsDataStore"

        /** Nombre histórico del DataStore; el fichero resultante es `files/datastore/settings.preferences_pb`. */
        const val NAME = "settings"

        /**
         * Crea el DataStore de ajustes sobre [file]. Sin `corruptionHandler`, un fichero
         * corrupto hace que `data` lance `CorruptionException` en cada lectura y la app
         * cerraría en cada arranque sin salida salvo borrar datos. Con él, el fichero se
         * sustituye por preferencias vacías (valores por defecto) y la app sigue arrancando.
         */
        fun create(file: File): DataStore<Preferences> = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { e ->
                Log.e(TAG, "Fichero de ajustes corrupto; se restauran los valores por defecto", e)
                emptyPreferences()
            },
            produceFile = { file },
        )
    }
}
