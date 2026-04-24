package com.parra.misdineros.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parra.misdineros.domain.model.FxRate
import com.parra.misdineros.domain.repository.FxRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FxRatesEditorViewModel @Inject constructor(
    private val fxRepository: FxRepository,
) : ViewModel() {

    val rates: StateFlow<List<FxRate>> = fxRepository.observeAll()
        .map { rates ->
            rates.filter { it.base == "EUR" && it.quote != "EUR" }.sortedBy { it.quote }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveRate(quote: String, newRate: Double) {
        if (newRate <= 0.0) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            fxRepository.upsert(FxRate("EUR", quote, newRate, now))
            fxRepository.upsert(FxRate(quote, "EUR", 1.0 / newRate, now))
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            fxRepository.resetToDefaults()
        }
    }
}
