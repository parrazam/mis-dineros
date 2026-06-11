package com.parra.misdineros

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parra.misdineros.designsystem.theme.AppTheme
import com.parra.misdineros.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ThemeConfig(
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val dynamicColor: Boolean = false,
)

@HiltViewModel
class MainViewModel @Inject constructor(settingsRepository: SettingsRepository) : ViewModel() {
    val themeConfig = settingsRepository.observe()
        .map { ThemeConfig(appTheme = it.appTheme, dynamicColor = it.dynamicColorEnabled) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeConfig())
}
