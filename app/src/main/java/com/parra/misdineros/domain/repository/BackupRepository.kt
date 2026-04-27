package com.parra.misdineros.domain.repository

import com.parra.misdineros.domain.model.AppSettings
import com.parra.misdineros.domain.model.Category
import com.parra.misdineros.domain.model.FxRate
import com.parra.misdineros.domain.model.Subscription

data class BackupSnapshot(
    val subscriptions: List<Subscription>,
    val categories: List<Category>,
    val fxRates: List<FxRate>,
    val settings: AppSettings,
)

interface BackupRepository {
    suspend fun snapshot(): BackupSnapshot
    suspend fun restore(snapshot: BackupSnapshot)
}
