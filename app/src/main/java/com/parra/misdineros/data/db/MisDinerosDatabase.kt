package com.parra.misdineros.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.parra.misdineros.data.db.dao.CategoryDao
import com.parra.misdineros.data.db.dao.FxRateDao
import com.parra.misdineros.data.db.dao.SubscriptionDao
import com.parra.misdineros.data.db.entity.CategoryEntity
import com.parra.misdineros.data.db.entity.FxRateEntity
import com.parra.misdineros.data.db.entity.SubscriptionEntity

@Database(
    entities = [
        SubscriptionEntity::class,
        CategoryEntity::class,
        FxRateEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class MisDinerosDatabase : RoomDatabase() {

    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun fxRateDao(): FxRateDao

    companion object {
        /**
         * Añade la columna `billingAnchorDay` (día de facturación de anclaje) y la rellena con el
         * día del mes de `nextRenewalDate` existente (formato ISO `yyyy-MM-dd`, día en posición 9-10).
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE subscriptions ADD COLUMN billingAnchorDay INTEGER NOT NULL DEFAULT 1")
                db.execSQL("UPDATE subscriptions SET billingAnchorDay = CAST(substr(nextRenewalDate, 9, 2) AS INTEGER)")
            }
        }

        /** Añade `sharedWith` (personas que comparten la suscripción); las existentes quedan en 1. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE subscriptions ADD COLUMN sharedWith INTEGER NOT NULL DEFAULT 1")
            }
        }
    }
}
