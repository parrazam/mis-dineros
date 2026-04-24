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

@HiltViewModel
class MainViewModel @Inject constructor(settingsRepository: SettingsRepository) : ViewModel() {
    val appTheme = settingsRepository.observe()
        .map { it.appTheme }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppTheme.SYSTEM)
}
