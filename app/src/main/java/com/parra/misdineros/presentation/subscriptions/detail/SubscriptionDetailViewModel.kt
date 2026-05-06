package com.parra.misdineros.presentation.subscriptions.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.parra.misdineros.domain.model.Category
import com.parra.misdineros.domain.model.Subscription
import com.parra.misdineros.domain.repository.CategoryRepository
import com.parra.misdineros.domain.repository.SubscriptionRepository
import com.parra.misdineros.domain.usecase.DeleteSubscriptionUseCase
import com.parra.misdineros.domain.usecase.ToggleSubscriptionPauseUseCase
import com.parra.misdineros.presentation.navigation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriptionDetailUiState(
    val isLoading: Boolean = true,
    val subscription: Subscription? = null,
    val category: Category? = null,
)

sealed interface SubscriptionDetailUiEvent {
    data object Deleted : SubscriptionDetailUiEvent
}

@HiltViewModel
class SubscriptionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val subscriptionRepository: SubscriptionRepository,
    private val categoryRepository: CategoryRepository,
    private val togglePauseUseCase: ToggleSubscriptionPauseUseCase,
    private val deleteUseCase: DeleteSubscriptionUseCase,
) : ViewModel() {

    private val subscriptionId: String = savedStateHandle.toRoute<Destination.SubscriptionDetail>().id

    private val _events = Channel<SubscriptionDetailUiEvent>(Channel.BUFFERED)
    val events: Flow<SubscriptionDetailUiEvent> = _events.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = subscriptionRepository.observeById(subscriptionId)
        .flatMapLatest { sub ->
            val categoryFlow = sub?.categoryId?.let { categoryRepository.observeById(it) } ?: flowOf(null)
            combine(flowOf(sub), categoryFlow) { s, c ->
                SubscriptionDetailUiState(isLoading = false, subscription = s, category = c)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SubscriptionDetailUiState(),
        )

    fun togglePause() {
        viewModelScope.launch { togglePauseUseCase(subscriptionId) }
    }

    fun delete() {
        viewModelScope.launch {
            deleteUseCase(subscriptionId)
            _events.send(SubscriptionDetailUiEvent.Deleted)
        }
    }
}
