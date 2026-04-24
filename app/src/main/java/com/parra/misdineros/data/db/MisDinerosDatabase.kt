package com.parra.misdineros.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.parra.misdineros.data.db.dao.CategoryDao
import com.parra.misdineros.data.db.dao.FxRateDao
import com.parra.misdineros.data.db.dao.SubscriptionDao
import com.parra.misdineros.data.db.entity.CategoryEntity
import com.parra.misdineros.data.db.entity.FxRateEntity
import com.parra.misdineros.data.db.entity.SubscriptionEntity
import com.parra.misdineros.data.fx.BuiltInCategories
import com.parra.misdineros.data.fx.BundledFxRates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SubscriptionEntity::class,
        CategoryEntity::class,
        FxRateEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class MisDinerosDatabase : RoomDatabase() {

    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun fxRateDao(): FxRateDao

    companion object {
        val seedCallback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // La semilla se ejecuta a través de los DAOs en el primer arranque
                // Usamos una coroutine de IO para no bloquear el hilo principal
                CoroutineScope(Dispatchers.IO).launch {
                    // No tenemos acceso directo a los DAOs aquí; la semilla se ejecuta
                    // en FxRepositoryImpl y CategoryRepositoryImpl al inicializarse (lazy seeding)
                }
            }
        }
    }
}
