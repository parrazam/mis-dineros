package com.parra.misdineros.presentation.subscriptions.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parra.misdineros.domain.model.BillingCycle
import com.parra.misdineros.domain.model.Category
import com.parra.misdineros.domain.model.Subscription
import com.parra.misdineros.domain.repository.CategoryRepository
import com.parra.misdineros.domain.usecase.DeleteSubscriptionUseCase
import com.parra.misdineros.domain.usecase.GetAllSubscriptionsUseCase
import com.parra.misdineros.domain.usecase.ToggleSubscriptionPauseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriptionListItem(
    val subscription: Subscription,
    val category: Category?,
)

data class SubscriptionListUiState(
    val items: List<SubscriptionListItem> = emptyList(),
    val activeFilter: BillingCycle? = null,
    val hasAnySubscription: Boolean = false,
    val isLoading: Boolean = true,
)

@HiltViewModel
class SubscriptionListViewModel @Inject constructor(
    getAllSubscriptions: GetAllSubscriptionsUseCase,
    private val categoryRepository: CategoryRepository,
    private val togglePauseUseCase: ToggleSubscriptionPauseUseCase,
    private val deleteUseCase: DeleteSubscriptionUseCase,
) : ViewModel() {

    private val _filter = MutableStateFlow<BillingCycle?>(null)

    val uiState: StateFlow<SubscriptionListUiState> = combine(
        getAllSubscriptions(),
        categoryRepository.observeAll(),
        _filter,
    ) { subscriptions, categories, filter ->
        val categoryMap = categories.associateBy { it.id }
        val filtered = if (filter == null) subscriptions else subscriptions.filter { it.billingCycle == filter }
        SubscriptionListUiState(
            items = filtered.map { sub -> SubscriptionListItem(sub, categoryMap[sub.categoryId]) },
            activeFilter = filter,
            hasAnySubscription = subscriptions.isNotEmpty(),
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SubscriptionListUiState(),
    )

    fun setFilter(cycle: BillingCycle?) {
        _filter.value = cycle
    }

    fun togglePause(id: String) {
        viewModelScope.launch { togglePauseUseCase(id) }
    }

    fun delete(id: String) {
        viewModelScope.launch { deleteUseCase(id) }
    }
}
