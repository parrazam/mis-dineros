package com.parra.misdineros.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parra.misdineros.designsystem.theme.AppTheme
import com.parra.misdineros.domain.model.AppSettings
import com.parra.misdineros.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repo.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private fun update(block: (AppSettings) -> AppSettings) {
        viewModelScope.launch { repo.update(block(settings.value)) }
    }

    fun setCurrency(code: String) = update { it.copy(globalCurrencyCode = code) }
    fun setTheme(theme: AppTheme) = update { it.copy(appTheme = theme) }
    fun setNotifsEnabled(enabled: Boolean) = update { it.copy(notificationsEnabled = enabled) }
    fun setNotifHour(hour: Int) = update { it.copy(notificationHour = hour) }
    fun setNotifyDays(days: Int) = update { it.copy(defaultNotifyDaysBefore = days) }
    fun setSummaryEnabled(enabled: Boolean) = update { it.copy(monthlySummaryEnabled = enabled) }
}
