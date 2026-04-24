package com.parra.misdineros.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parra.misdineros.domain.model.Category
import com.parra.misdineros.domain.repository.CategoryRepository
import com.parra.misdineros.domain.repository.SettingsRepository
import com.parra.misdineros.domain.usecase.CalcAnnualEquivalentUseCase
import com.parra.misdineros.domain.usecase.CalcMonthlySpendUseCase
import com.parra.misdineros.domain.usecase.CalcSpendByCategoryUseCase
import com.parra.misdineros.domain.usecase.CalcTopExpensiveUseCase
import com.parra.misdineros.domain.usecase.GetAllSubscriptionsUseCase
import com.parra.misdineros.domain.usecase.RankedSubscription
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CategorySpendItem(
    val category: Category,
    val monthlyAmountMinor: Long,
    val percentage: Float,
)

data class StatsUiState(
    val isLoading: Boolean = true,
    val globalCurrency: String = "EUR",
    val monthlyTotalMinor: Long = 0L,
    val annualEquivalentMinor: Long = 0L,
    val categoryItems: List<CategorySpendItem> = emptyList(),
    val top5: List<RankedSubscription> = emptyList(),
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    getAllSubscriptions: GetAllSubscriptionsUseCase,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val calcMonthlySpend: CalcMonthlySpendUseCase,
    private val calcAnnualEquivalent: CalcAnnualEquivalentUseCase,
    private val calcSpendByCategory: CalcSpendByCategoryUseCase,
    private val calcTopExpensive: CalcTopExpensiveUseCase,
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        getAllSubscriptions(),
        settingsRepository.observe(),
        categoryRepository.observeAll(),
    ) { subscriptions, settings, categories ->
        val currency = settings.globalCurrencyCode
        val monthly = calcMonthlySpend(subscriptions, currency)
        val annual = calcAnnualEquivalent(subscriptions, currency)
        val top5 = calcTopExpensive(subscriptions, currency, limit = 5)
        val spendByCategory = calcSpendByCategory(subscriptions, currency)

        val categoryMap = categories.associateBy { it.id }
        val categoryItems = spendByCategory.mapNotNull { spend ->
            val category = categoryMap[spend.categoryId] ?: return@mapNotNull null
            CategorySpendItem(
                category = category,
                monthlyAmountMinor = spend.monthlyAmountMinor,
                percentage = if (monthly > 0) spend.monthlyAmountMinor.toFloat() / monthly * 100f else 0f,
            )
        }

        StatsUiState(
            isLoading = false,
            globalCurrency = currency,
            monthlyTotalMinor = monthly,
            annualEquivalentMinor = annual,
            categoryItems = categoryItems,
            top5 = top5,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState(),
    )
}
