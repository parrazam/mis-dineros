package com.parra.misdineros.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parra.misdineros.domain.model.Category
import com.parra.misdineros.domain.model.Subscription
import com.parra.misdineros.domain.repository.CategoryRepository
import com.parra.misdineros.domain.repository.SettingsRepository
import com.parra.misdineros.domain.usecase.CalcAnnualEquivalentUseCase
import com.parra.misdineros.domain.usecase.CalcMonthlySpendUseCase
import com.parra.misdineros.domain.usecase.CalcTopExpensiveUseCase
import com.parra.misdineros.domain.usecase.GetAllSubscriptionsUseCase
import com.parra.misdineros.domain.usecase.RankedSubscription
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class UpcomingRenewal(
    val subscription: Subscription,
    val daysUntil: Long,
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val globalCurrency: String = "EUR",
    val monthlyTotalMinor: Long = 0L,
    val annualEquivalentMinor: Long = 0L,
    val activeCount: Int = 0,
    val pausedCount: Int = 0,
    val upcomingRenewals: List<UpcomingRenewal> = emptyList(),
    val top5: List<RankedSubscription> = emptyList(),
    val categories: Map<String, Category> = emptyMap(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    getAllSubscriptions: GetAllSubscriptionsUseCase,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val calcMonthlySpend: CalcMonthlySpendUseCase,
    private val calcAnnualEquivalent: CalcAnnualEquivalentUseCase,
    private val calcTopExpensive: CalcTopExpensiveUseCase,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        getAllSubscriptions(),
        settingsRepository.observe(),
        categoryRepository.observeAll(),
    ) { subscriptions, settings, categories ->
        val currency = settings.globalCurrencyCode
        val monthly = calcMonthlySpend(subscriptions, currency)
        val annual = calcAnnualEquivalent(subscriptions, currency)
        val top5 = calcTopExpensive(subscriptions, currency, limit = 5)

        val now = LocalDate.now()
        val upcoming = subscriptions
            .filter { !it.isPaused }
            .mapNotNull { sub ->
                val days = sub.nextRenewalDate.toEpochDay() - now.toEpochDay()
                if (days in 0..7) UpcomingRenewal(sub, days) else null
            }
            .sortedBy { it.daysUntil }

        HomeUiState(
            isLoading = false,
            globalCurrency = currency,
            monthlyTotalMinor = monthly,
            annualEquivalentMinor = annual,
            activeCount = subscriptions.count { !it.isPaused },
            pausedCount = subscriptions.count { it.isPaused },
            upcomingRenewals = upcoming,
            top5 = top5,
            categories = categories.associateBy { it.id },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )
}
