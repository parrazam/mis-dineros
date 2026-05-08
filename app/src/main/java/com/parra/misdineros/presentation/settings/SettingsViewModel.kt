package com.parra.misdineros.presentation.settings

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.parra.misdineros.data.backup.PasswordRequiredException
import com.parra.misdineros.data.backup.WrongPasswordException
import com.parra.misdineros.designsystem.theme.AppTheme
import com.parra.misdineros.domain.model.AppSettings
import com.parra.misdineros.domain.repository.SettingsRepository
import com.parra.misdineros.domain.usecase.ExportDataUseCase
import com.parra.misdineros.domain.usecase.ImportDataUseCase
import com.parra.misdineros.notifications.NotificationScheduler
import com.parra.misdineros.notifications.RenewalReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

sealed interface BackupState {
    data object Idle : BackupState
    data object Loading : BackupState
    data object ExportSuccess : BackupState
    data object ImportSuccess : BackupState
    data object PasswordRequired : BackupState
    data object WrongPassword : BackupState
    data class Error(val message: String) : BackupState
}

sealed interface BackupEvent {
    data class Share(val uri: Uri, val mime: String) : BackupEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: SettingsRepository,
    private val notificationScheduler: NotificationScheduler,
    private val exportDataUseCase: ExportDataUseCase,
    private val importDataUseCase: ImportDataUseCase,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repo.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    private val _events = Channel<BackupEvent>(Channel.BUFFERED)
    val events: Flow<BackupEvent> = _events.receiveAsFlow()

    // Contraseña almacenada temporalmente entre el diálogo de export y el callback de SAF.
    private var pendingExportPassword: CharArray? = null

    private fun update(block: (AppSettings) -> AppSettings) {
        viewModelScope.launch { repo.update(block(settings.value)) }
    }

    fun setCurrency(code: String) = update { it.copy(globalCurrencyCode = code) }
    fun setTheme(theme: AppTheme) = update { it.copy(appTheme = theme) }

    fun setNotifsEnabled(enabled: Boolean) {
        update { it.copy(notificationsEnabled = enabled) }
        with(settings.value) { notificationScheduler.schedule(notificationHour, notificationMinute, enabled) }
    }

    fun setNotifTime(hour: Int, minute: Int) {
        update { it.copy(notificationHour = hour, notificationMinute = minute) }
        notificationScheduler.schedule(hour, minute, settings.value.notificationsEnabled)
    }

    fun setNotifyDays(days: Int) = update { it.copy(defaultNotifyDaysBefore = days) }
    fun setSummaryEnabled(enabled: Boolean) = update { it.copy(monthlySummaryEnabled = enabled) }
    fun setAutoBackupEnabled(enabled: Boolean) = update { it.copy(autoBackupEnabled = enabled) }

    fun setPendingExportPassword(password: CharArray?) {
        pendingExportPassword?.fill(' ')
        pendingExportPassword = password
    }

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            _backupState.value = BackupState.Loading
            val password = pendingExportPassword
            pendingExportPassword = null
            exportDataUseCase(uri, password).fold(
                onSuccess = { _backupState.value = BackupState.ExportSuccess },
                onFailure = { _backupState.value = BackupState.Error(it.message ?: "Error desconocido") },
            )
        }
    }

    fun exportAndShare(password: CharArray? = null) {
        viewModelScope.launch {
            _backupState.value = BackupState.Loading
            exportDataUseCase.exportToBytes(password).fold(
                onSuccess = { bytes ->
                    runCatching {
                        val exportsDir = File(context.cacheDir, "exports").apply {
                            deleteRecursively()
                            mkdirs()
                        }
                        val timestamp = LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
                        val ext = if (password != null) "mdb" else "json"
                        val file = File(exportsDir, "mis-dineros-backup-$timestamp.$ext")
                        file.writeBytes(bytes)
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file,
                        )
                        val mime = if (password != null) "application/octet-stream" else "application/json"
                        _events.send(BackupEvent.Share(uri, mime))
                        _backupState.value = BackupState.ExportSuccess
                    }.onFailure {
                        _backupState.value = BackupState.Error(it.message ?: "Error al preparar el archivo")
                    }
                },
                onFailure = { _backupState.value = BackupState.Error(it.message ?: "Error desconocido") },
            )
        }
    }

    fun importData(uri: Uri, password: CharArray? = null) {
        viewModelScope.launch {
            _backupState.value = BackupState.Loading
            importDataUseCase(uri, password).fold(
                onSuccess = { _backupState.value = BackupState.ImportSuccess },
                onFailure = { e ->
                    _backupState.value = when (e) {
                        is PasswordRequiredException -> BackupState.PasswordRequired
                        is WrongPasswordException -> BackupState.WrongPassword
                        else -> BackupState.Error(e.message ?: "Error desconocido")
                    }
                },
            )
        }
    }

    fun clearBackupState() { _backupState.value = BackupState.Idle }

    fun testNotificationNow() {
        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<RenewalReminderWorker>().build()
        )
    }
}
