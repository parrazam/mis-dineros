package com.parra.misdineros.presentation.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parra.misdineros.designsystem.theme.AppTheme
import com.parra.misdineros.domain.model.AppSettings
import com.parra.misdineros.domain.repository.SettingsRepository
import com.parra.misdineros.domain.usecase.ExportDataUseCase
import com.parra.misdineros.domain.usecase.ImportDataUseCase
import com.parra.misdineros.notifications.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BackupState {
    data object Idle : BackupState
    data object Loading : BackupState
    data object ExportSuccess : BackupState
    data object ImportSuccess : BackupState
    data class Error(val message: String) : BackupState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
    private val notificationScheduler: NotificationScheduler,
    private val exportDataUseCase: ExportDataUseCase,
    private val importDataUseCase: ImportDataUseCase,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repo.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    private fun update(block: (AppSettings) -> AppSettings) {
        viewModelScope.launch { repo.update(block(settings.value)) }
    }

    fun setCurrency(code: String) = update { it.copy(globalCurrencyCode = code) }
    fun setTheme(theme: AppTheme) = update { it.copy(appTheme = theme) }

    fun setNotifsEnabled(enabled: Boolean) {
        update { it.copy(notificationsEnabled = enabled) }
        notificationScheduler.schedule(settings.value.notificationHour, enabled)
    }

    fun setNotifHour(hour: Int) {
        update { it.copy(notificationHour = hour) }
        notificationScheduler.schedule(hour, settings.value.notificationsEnabled)
    }

    fun setNotifyDays(days: Int) = update { it.copy(defaultNotifyDaysBefore = days) }
    fun setSummaryEnabled(enabled: Boolean) = update { it.copy(monthlySummaryEnabled = enabled) }

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            _backupState.value = BackupState.Loading
            exportDataUseCase(uri).fold(
                onSuccess = { _backupState.value = BackupState.ExportSuccess },
                onFailure = { _backupState.value = BackupState.Error(it.message ?: "Error desconocido") },
            )
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            _backupState.value = BackupState.Loading
            importDataUseCase(uri).fold(
                onSuccess = { _backupState.value = BackupState.ImportSuccess },
                onFailure = { _backupState.value = BackupState.Error(it.message ?: "Error desconocido") },
            )
        }
    }

    fun clearBackupState() { _backupState.value = BackupState.Idle }
}
