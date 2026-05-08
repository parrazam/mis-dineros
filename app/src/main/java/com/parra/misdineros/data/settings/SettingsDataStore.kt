package com.parra.misdineros.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.parra.misdineros.backup.MisDinerosBackupAgent
import com.parra.misdineros.designsystem.theme.AppTheme
import com.parra.misdineros.domain.model.AppSettings
import com.parra.misdineros.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    private object Keys {
        val CURRENCY = stringPreferencesKey("global_currency")
        val NOTIFS_ENABLED = booleanPreferencesKey("notifs_enabled")
        val NOTIF_HOUR = intPreferencesKey("notif_hour")
        val NOTIF_MINUTE = intPreferencesKey("notif_minute")
        val NOTIFY_DAYS = intPreferencesKey("notify_days")
        val SUMMARY_ENABLED = booleanPreferencesKey("summary_enabled")
        val THEME = stringPreferencesKey("app_theme")
        val AUTO_BACKUP = booleanPreferencesKey("auto_backup_enabled")
    }

    // SharedPreferences mirror para MisDinerosBackupAgent (acceso síncrono desde el agente).
    private val backupPrefs by lazy {
        context.getSharedPreferences(MisDinerosBackupAgent.PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun observe(): Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            globalCurrencyCode = prefs[Keys.CURRENCY] ?: "EUR",
            notificationsEnabled = prefs[Keys.NOTIFS_ENABLED] ?: true,
            notificationHour = prefs[Keys.NOTIF_HOUR] ?: 9,
            notificationMinute = prefs[Keys.NOTIF_MINUTE] ?: 0,
            defaultNotifyDaysBefore = prefs[Keys.NOTIFY_DAYS] ?: 3,
            monthlySummaryEnabled = prefs[Keys.SUMMARY_ENABLED] ?: true,
            appTheme = prefs[Keys.THEME]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() }
                ?: AppTheme.SYSTEM,
            autoBackupEnabled = prefs[Keys.AUTO_BACKUP] ?: true,
        )
    }

    override suspend fun update(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CURRENCY] = settings.globalCurrencyCode
            prefs[Keys.NOTIFS_ENABLED] = settings.notificationsEnabled
            prefs[Keys.NOTIF_HOUR] = settings.notificationHour
            prefs[Keys.NOTIF_MINUTE] = settings.notificationMinute
            prefs[Keys.NOTIFY_DAYS] = settings.defaultNotifyDaysBefore
            prefs[Keys.SUMMARY_ENABLED] = settings.monthlySummaryEnabled
            prefs[Keys.THEME] = settings.appTheme.name
            prefs[Keys.AUTO_BACKUP] = settings.autoBackupEnabled
        }
        // Mirror síncrono para que BackupAgent lo lea sin corrutinas.
        backupPrefs.edit()
            .putBoolean(MisDinerosBackupAgent.KEY_ENABLED, settings.autoBackupEnabled)
            .apply()
    }
}
