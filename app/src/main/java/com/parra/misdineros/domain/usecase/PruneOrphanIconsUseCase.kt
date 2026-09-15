package com.parra.misdineros.domain.usecase

import com.parra.misdineros.domain.repository.CategoryRepository
import com.parra.misdineros.domain.repository.IconStore
import com.parra.misdineros.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Borra los ficheros de icono que ya no referencia ninguna suscripción ni categoría
 * (ediciones canceladas tras elegir imagen, restos de importaciones, borrados antiguos).
 * Se ejecuta al arrancar la app y tras importar un backup.
 */
class PruneOrphanIconsUseCase @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val categoryRepository: CategoryRepository,
    private val iconStore: IconStore,
) {
    suspend operator fun invoke() {
        val referenced = buildSet {
            subscriptionRepository.observeAll().first().forEach { add(it.iconRef) }
            categoryRepository.observeAll().first().forEach { add(it.iconKey) }
        }
        iconStore.pruneOrphans(referenced)
    }
}
