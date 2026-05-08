package com.parra.misdineros.domain.model

import com.parra.misdineros.designsystem.theme.AppTheme

data class AppSettings(
    val globalCurrencyCode: String = "EUR",
    val notificationsEnabled: Boolean = true,
    val notificationHour: Int = 9,
    val notificationMinute: Int = 0,
    val defaultNotifyDaysBefore: Int = 3,
    val monthlySummaryEnabled: Boolean = true,
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val autoBackupEnabled: Boolean = true,
)
