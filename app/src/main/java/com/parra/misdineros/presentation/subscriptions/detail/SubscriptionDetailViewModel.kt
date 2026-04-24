package com.parra.misdineros.presentation.subscriptions.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
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

    private val subscriptionId: String = checkNotNull(savedStateHandle[Destination.SubscriptionDetail.ARG_ID])

    private val _uiState = MutableStateFlow(SubscriptionDetailUiState())
    val uiState: StateFlow<SubscriptionDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<SubscriptionDetailUiEvent>(Channel.BUFFERED)
    val events: Flow<SubscriptionDetailUiEvent> = _events.receiveAsFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val subscription = subscriptionRepository.getById(subscriptionId)
            val category = subscription?.categoryId?.let { categoryRepository.getById(it) }
            _uiState.update { it.copy(isLoading = false, subscription = subscription, category = category) }
        }
    }

    fun togglePause() {
        viewModelScope.launch {
            togglePauseUseCase(subscriptionId)
            load()
        }
    }

    fun delete() {
        viewModelScope.launch {
            deleteUseCase(subscriptionId)
            _events.send(SubscriptionDetailUiEvent.Deleted)
        }
    }
}
