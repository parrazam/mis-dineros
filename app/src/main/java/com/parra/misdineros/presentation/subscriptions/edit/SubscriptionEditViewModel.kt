package com.parra.misdineros.presentation.subscriptions.edit

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.parra.misdineros.core.money.MoneyFormatter
import com.parra.misdineros.data.icons.IconStorage
import com.parra.misdineros.domain.model.BillingCycle
import com.parra.misdineros.domain.model.Category
import com.parra.misdineros.domain.model.Subscription
import com.parra.misdineros.domain.repository.CategoryRepository
import com.parra.misdineros.domain.repository.SubscriptionRepository
import com.parra.misdineros.domain.usecase.UpsertSubscriptionUseCase
import com.parra.misdineros.presentation.navigation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

val SUPPORTED_CURRENCIES = listOf(
    "EUR", "USD", "GBP", "JPY", "CHF", "CAD", "AUD", "CNY",
    "MXN", "BRL", "SEK", "NOK", "DKK", "PLN", "CZK", "HUF",
    "RON", "BGN", "HRK", "RUB", "TRY", "INR", "KRW", "SGD",
    "HKD", "NZD",
)

data class SubscriptionEditUiState(
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val name: String = "",
    val nameError: String? = null,
    val iconRef: String = "initial",
    val amountText: String = "",
    val amountError: String? = null,
    val currencyCode: String = "EUR",
    val billingCycle: BillingCycle = BillingCycle.MONTHLY,
    val nextRenewalDate: LocalDate = LocalDate.now().plusMonths(1),
    val dateError: String? = null,
    val categoryId: String = Category.FALLBACK_ID,
    val categories: List<Category> = emptyList(),
    val notifyDaysBefore: Int? = null,
    val notes: String = "",
    /** Suscripción que se está editando, o `null` si se crea una nueva. */
    val original: Subscription? = null,
)

/**
 * Construye la suscripción a persistir a partir del formulario. Es una función pura para poder
 * probar sin Android las reglas de conservación al editar:
 *  - `isPaused` se mantiene (antes `save()` reactivaba en silencio una suscripción pausada).
 *  - `billingAnchorDay` solo se recalcula si el usuario cambió la fecha; si no, una suscripción
 *    anclada al 31 que hoy muestra el 28 de febrero se quedaría anclada al 28 para siempre.
 */
internal fun SubscriptionEditUiState.toSubscription(newId: String, amountMinor: Long, now: Long): Subscription {
    val original = original
    val dateChanged = original == null || nextRenewalDate != original.nextRenewalDate
    return Subscription(
        id = original?.id ?: newId,
        name = name.trim(),
        iconRef = iconRef,
        amountMinor = amountMinor,
        currencyCode = currencyCode,
        billingCycle = billingCycle,
        nextRenewalDate = nextRenewalDate,
        billingAnchorDay = if (dateChanged) nextRenewalDate.dayOfMonth else original.billingAnchorDay,
        categoryId = categoryId,
        isPaused = original?.isPaused ?: false,
        notifyDaysBefore = notifyDaysBefore,
        notes = notes.takeIf { it.isNotBlank() },
        createdAt = original?.createdAt ?: now,
        updatedAt = now,
    )
}

sealed interface SubscriptionEditUiEvent {
    data object Saved : SubscriptionEditUiEvent
}

@HiltViewModel
class SubscriptionEditViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val subscriptionRepository: SubscriptionRepository,
    private val categoryRepository: CategoryRepository,
    private val upsertSubscription: UpsertSubscriptionUseCase,
    private val iconStorage: IconStorage,
) : ViewModel() {

    private val subscriptionId: String? = savedStateHandle.toRoute<Destination.SubscriptionEdit>().id

    private val _uiState = MutableStateFlow(SubscriptionEditUiState())
    val uiState: StateFlow<SubscriptionEditUiState> = _uiState.asStateFlow()

    private val _events = Channel<SubscriptionEditUiEvent>(Channel.BUFFERED)
    val events: Flow<SubscriptionEditUiEvent> = _events.receiveAsFlow()

    init {
        loadCategories()
        if (subscriptionId != null) loadExisting(subscriptionId)
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.observeAll().collect { cats ->
                _uiState.update { state ->
                    state.copy(
                        categories = cats,
                        categoryId = if (state.categoryId.isEmpty() || cats.none { it.id == state.categoryId })
                            cats.firstOrNull()?.id ?: Category.FALLBACK_ID
                        else state.categoryId,
                    )
                }
            }
        }
    }

    private fun loadExisting(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val sub = subscriptionRepository.getById(id)
            if (sub != null) {
                val amountText = run {
                    val fractionDigits = runCatching {
                        java.util.Currency.getInstance(sub.currencyCode).defaultFractionDigits
                    }.getOrDefault(2)
                    if (fractionDigits > 0) {
                        "%.${fractionDigits}f".format(sub.amountMinor.toDouble() / Math.pow(10.0, fractionDigits.toDouble()))
                    } else {
                        sub.amountMinor.toString()
                    }
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isEditing = true,
                        name = sub.name,
                        iconRef = sub.iconRef,
                        amountText = amountText,
                        currencyCode = sub.currencyCode,
                        billingCycle = sub.billingCycle,
                        nextRenewalDate = sub.nextRenewalDate,
                        categoryId = sub.categoryId,
                        notifyDaysBefore = sub.notifyDaysBefore,
                        notes = sub.notes ?: "",
                        original = sub,
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, nameError = null) }
    fun onIconRefChange(value: String) = _uiState.update { it.copy(iconRef = value) }
    fun onAmountChange(value: String) = _uiState.update { it.copy(amountText = value, amountError = null) }
    fun onCurrencyChange(value: String) = _uiState.update { it.copy(currencyCode = value) }
    fun onBillingCycleChange(value: BillingCycle) = _uiState.update { it.copy(billingCycle = value) }
    fun onRenewalDateChange(value: LocalDate) = _uiState.update { it.copy(nextRenewalDate = value, dateError = null) }
    fun onCategoryChange(value: String) = _uiState.update { it.copy(categoryId = value) }
    fun onNotifyDaysChange(value: Int?) = _uiState.update { it.copy(notifyDaysBefore = value) }
    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }

    fun onImagePicked(uri: Uri) {
        viewModelScope.launch {
            val newRef = iconStorage.importFromUri(uri, IconStorage.Kind.SUBSCRIPTION) ?: return@launch
            val previous = _uiState.value.iconRef
            _uiState.update { it.copy(iconRef = newRef) }
            // Una imagen elegida antes en esta misma edición y aún no guardada ya no sirve.
            if (previous != _uiState.value.original?.iconRef) iconStorage.delete(previous)
        }
    }

    fun save() {
        val state = _uiState.value
        var hasError = false

        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "El nombre es obligatorio") }
            hasError = true
        }

        val amountMinor = MoneyFormatter.parseToMinor(state.amountText, state.currencyCode)
        if (amountMinor == null || amountMinor <= 0) {
            _uiState.update { it.copy(amountError = "Introduce un importe válido") }
            hasError = true
        }

        if (state.nextRenewalDate.isBefore(LocalDate.now())) {
            _uiState.update { it.copy(dateError = "La fecha de renovación no puede estar en el pasado") }
            hasError = true
        }

        if (hasError) return

        viewModelScope.launch {
            val subscription = state.toSubscription(
                newId = subscriptionId ?: UUID.randomUUID().toString(),
                amountMinor = amountMinor!!,
                now = System.currentTimeMillis(),
            )
            upsertSubscription(subscription)
            _events.send(SubscriptionEditUiEvent.Saved)
        }
    }
}
