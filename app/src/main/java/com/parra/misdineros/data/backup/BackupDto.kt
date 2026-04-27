package com.parra.misdineros.data.backup

import com.parra.misdineros.designsystem.theme.AppTheme
import com.parra.misdineros.domain.model.AppSettings
import com.parra.misdineros.domain.model.BillingCycle
import com.parra.misdineros.domain.model.Category
import com.parra.misdineros.domain.model.FxRate
import com.parra.misdineros.domain.model.Subscription
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class BackupJson(
    val version: Int = 1,
    val exportedAt: String,
    val subscriptions: List<SubscriptionDto>,
    val categories: List<CategoryDto>,
    val fxRates: List<FxRateDto>,
    val settings: SettingsDto,
    val assets: Map<String, String> = emptyMap(),
)

@Serializable
data class SubscriptionDto(
    val id: String,
    val name: String,
    val iconRef: String,
    val amountMinor: Long,
    val currencyCode: String,
    val billingCycle: String,
    val nextRenewalDate: String,
    val categoryId: String,
    val isPaused: Boolean,
    val notifyDaysBefore: Int? = null,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val iconKey: String,
    val colorArgb: Int,
    val isBuiltIn: Boolean,
    val sortOrder: Int,
)

@Serializable
data class FxRateDto(
    val base: String,
    val quote: String,
    val rate: Double,
    val updatedAt: Long,
)

@Serializable
data class SettingsDto(
    val globalCurrencyCode: String,
    val notificationsEnabled: Boolean,
    val notificationHour: Int,
    val defaultNotifyDaysBefore: Int,
    val monthlySummaryEnabled: Boolean,
    val appTheme: String,
)

// ─── Domain → DTO ─────────────────────────────────────────────────────────────

fun Subscription.toDto() = SubscriptionDto(
    id = id,
    name = name,
    iconRef = iconRef,
    amountMinor = amountMinor,
    currencyCode = currencyCode,
    billingCycle = billingCycle.name,
    nextRenewalDate = nextRenewalDate.toString(),
    categoryId = categoryId,
    isPaused = isPaused,
    notifyDaysBefore = notifyDaysBefore,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Category.toDto() = CategoryDto(
    id = id,
    name = name,
    iconKey = iconKey,
    colorArgb = colorArgb,
    isBuiltIn = isBuiltIn,
    sortOrder = sortOrder,
)

fun FxRate.toDto() = FxRateDto(base = base, quote = quote, rate = rate, updatedAt = updatedAt)

fun AppSettings.toDto() = SettingsDto(
    globalCurrencyCode = globalCurrencyCode,
    notificationsEnabled = notificationsEnabled,
    notificationHour = notificationHour,
    defaultNotifyDaysBefore = defaultNotifyDaysBefore,
    monthlySummaryEnabled = monthlySummaryEnabled,
    appTheme = appTheme.name,
)

// ─── DTO → Domain ─────────────────────────────────────────────────────────────

fun SubscriptionDto.toDomain(resolvedIconRef: String = iconRef) = Subscription(
    id = id,
    name = name,
    iconRef = resolvedIconRef,
    amountMinor = amountMinor,
    currencyCode = currencyCode,
    billingCycle = BillingCycle.valueOf(billingCycle),
    nextRenewalDate = LocalDate.parse(nextRenewalDate),
    categoryId = categoryId,
    isPaused = isPaused,
    notifyDaysBefore = notifyDaysBefore,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun CategoryDto.toDomain() = Category(
    id = id,
    name = name,
    iconKey = iconKey,
    colorArgb = colorArgb,
    isBuiltIn = isBuiltIn,
    sortOrder = sortOrder,
)

fun FxRateDto.toDomain() = FxRate(base = base, quote = quote, rate = rate, updatedAt = updatedAt)

fun SettingsDto.toDomain() = AppSettings(
    globalCurrencyCode = globalCurrencyCode,
    notificationsEnabled = notificationsEnabled,
    notificationHour = notificationHour,
    defaultNotifyDaysBefore = defaultNotifyDaysBefore,
    monthlySummaryEnabled = monthlySummaryEnabled,
    appTheme = AppTheme.valueOf(appTheme),
)
