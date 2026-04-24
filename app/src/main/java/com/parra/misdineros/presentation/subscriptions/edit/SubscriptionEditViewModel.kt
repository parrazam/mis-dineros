package com.parra.misdineros.presentation.subscriptions.edit

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parra.misdineros.domain.model.BillingCycle
import com.parra.misdineros.domain.model.Category
import com.parra.misdineros.domain.model.Subscription
import com.parra.misdineros.domain.repository.CategoryRepository
import com.parra.misdineros.domain.repository.SubscriptionRepository
import com.parra.misdineros.domain.usecase.UpsertSubscriptionUseCase
import com.parra.misdineros.presentation.navigation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
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
    val categoryId: String = "builtin_otros",
    val categories: List<Category> = emptyList(),
    val notifyDaysBefore: Int? = null,
    val notes: String = "",
    val originalCreatedAt: Long = System.currentTimeMillis(),
)

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
) : ViewModel() {

    private val subscriptionId: String? = savedStateHandle[Destination.SubscriptionEdit.ARG_ID]

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
                            cats.firstOrNull()?.id ?: "builtin_otros"
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
                        originalCreatedAt = sub.createdAt,
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
    fun onRenewalDateChange(value: LocalDate) = _uiState.update { it.copy(nextRenewalDate = value) }
    fun onCategoryChange(value: String) = _uiState.update { it.copy(categoryId = value) }
    fun onNotifyDaysChange(value: Int?) = _uiState.update { it.copy(notifyDaysBefore = value) }
    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }

    fun onImagePicked(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val iconsDir = File(context.filesDir, "icons").also { it.mkdirs() }
                val destFile = File(iconsDir, "${UUID.randomUUID()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
                _uiState.update { it.copy(iconRef = "file:${destFile.absolutePath}") }
            }
        }
    }

    fun save() {
        val state = _uiState.value
        var hasError = false

        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "El nombre es obligatorio") }
            hasError = true
        }

        val amountMinor = parseAmountMinor(state.amountText, state.currencyCode)
        if (amountMinor == null || amountMinor <= 0) {
            _uiState.update { it.copy(amountError = "Introduce un importe válido") }
            hasError = true
        }

        if (hasError) return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val subscription = Subscription(
                id = subscriptionId ?: UUID.randomUUID().toString(),
                name = state.name.trim(),
                iconRef = state.iconRef,
                amountMinor = amountMinor!!,
                currencyCode = state.currencyCode,
                billingCycle = state.billingCycle,
                nextRenewalDate = state.nextRenewalDate,
                categoryId = state.categoryId,
                isPaused = false,
                notifyDaysBefore = state.notifyDaysBefore,
                notes = state.notes.takeIf { it.isNotBlank() },
                createdAt = state.originalCreatedAt,
                updatedAt = now,
            )
            upsertSubscription(subscription)
            _events.send(SubscriptionEditUiEvent.Saved)
        }
    }

    private fun parseAmountMinor(text: String, currencyCode: String): Long? = runCatching {
        val fractionDigits = java.util.Currency.getInstance(currencyCode).defaultFractionDigits
        val cleaned = text.replace(",", ".").replace("[^0-9.]".toRegex(), "")
        val amount = cleaned.toDouble()
        if (fractionDigits > 0) (amount * Math.pow(10.0, fractionDigits.toDouble())).toLong()
        else amount.toLong()
    }.getOrNull()
}
