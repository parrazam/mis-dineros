package com.parra.misdineros.domain.repository

import com.parra.misdineros.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observe(): Flow<AppSettings>
    suspend fun update(settings: AppSettings)
}
