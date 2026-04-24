package com.parra.misdineros.presentation.subscriptions.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parra.misdineros.domain.model.Category
import com.parra.misdineros.domain.model.Subscription
import com.parra.misdineros.domain.repository.CategoryRepository
import com.parra.misdineros.domain.usecase.DeleteSubscriptionUseCase
import com.parra.misdineros.domain.usecase.GetAllSubscriptionsUseCase
import com.parra.misdineros.domain.usecase.ToggleSubscriptionPauseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val isLoading: Boolean = true,
)

@HiltViewModel
class SubscriptionListViewModel @Inject constructor(
    getAllSubscriptions: GetAllSubscriptionsUseCase,
    private val categoryRepository: CategoryRepository,
    private val togglePauseUseCase: ToggleSubscriptionPauseUseCase,
    private val deleteUseCase: DeleteSubscriptionUseCase,
) : ViewModel() {

    val uiState: StateFlow<SubscriptionListUiState> = combine(
        getAllSubscriptions(),
        categoryRepository.observeAll(),
    ) { subscriptions, categories ->
        val categoryMap = categories.associateBy { it.id }
        SubscriptionListUiState(
            items = subscriptions.map { sub ->
                SubscriptionListItem(sub, categoryMap[sub.categoryId])
            },
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SubscriptionListUiState(),
    )

    fun togglePause(id: String) {
        viewModelScope.launch { togglePauseUseCase(id) }
    }

    fun delete(id: String) {
        viewModelScope.launch { deleteUseCase(id) }
    }
}
