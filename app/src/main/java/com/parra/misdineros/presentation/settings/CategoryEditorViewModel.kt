package com.parra.misdineros.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.parra.misdineros.data.icons.IconStorage
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
    private val iconStorage: IconStorage,
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun upsert(category: Category) {
        viewModelScope.launch { categoryRepository.upsert(category) }
    }

    fun delete(id: String) {
        viewModelScope.launch { categoryRepository.delete(id) }
    }

    /**
     * Importa la imagen reducida y devuelve su `file:`; borra la elegida antes en este mismo
     * diálogo ([current]) si no es la que ya tenía la categoría guardada ([saved]).
     */
    suspend fun importIcon(uri: Uri, current: String, saved: String?): String? {
        val newKey = iconStorage.importFromUri(uri, IconStorage.Kind.CATEGORY) ?: return null
        if (current != saved) iconStorage.delete(current)
        return newKey
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
