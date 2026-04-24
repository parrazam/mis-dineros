package com.parra.misdineros.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parra.misdineros.domain.model.Category
import com.parra.misdineros.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CategoryEditorViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun upsert(category: Category) {
        viewModelScope.launch { categoryRepository.upsert(category) }
    }

    fun delete(id: String) {
        viewModelScope.launch { categoryRepository.delete(id) }
    }

    fun newCategory(name: String, iconKey: String, colorArgb: Int): Category = Category(
        id = UUID.randomUUID().toString(),
        name = name.trim(),
        iconKey = iconKey,
        colorArgb = colorArgb,
        isBuiltIn = false,
        sortOrder = Int.MAX_VALUE,
    )
}
