package com.parra.misdineros.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Migra bases de datos reales creadas con los esquemas exportados en `app/schemas/`. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MisDinerosDatabase::class.java,
    )

    @Test
    fun migrate2To3ConservaLosDatosYDejaSinCompartir() {
        helper.createDatabase(dbName, 2).use { db ->
            db.execSQL(
                "INSERT INTO categories (id, name, iconKey, colorArgb, isBuiltIn, sortOrder) " +
                    "VALUES ('builtin_otros', 'Otros', 'category', 0, 1, 0)"
            )
            db.execSQL(
                "INSERT INTO subscriptions (id, name, iconRef, amountMinor, currencyCode, billingCycle, " +
                    "nextRenewalDate, billingAnchorDay, categoryId, isPaused, notifyDaysBefore, notes, " +
                    "createdAt, updatedAt) VALUES ('s1', 'Spotify', 'bundled:spotify', 1799, 'EUR', " +
                    "'MONTHLY', '2030-01-31', 31, 'builtin_otros', 1, 3, 'nota', 10, 20)"
            )
        }

        // validateDroppedTables + comprobación del esquema resultante contra 3.json.
        helper.runMigrationsAndValidate(dbName, 3, true, MisDinerosDatabase.MIGRATION_2_3).use { db ->
            db.query("SELECT name, amountMinor, billingAnchorDay, isPaused, notes, sharedWith FROM subscriptions").use { c ->
                assertEquals(1, c.count)
                c.moveToFirst()
                assertEquals("Spotify", c.getString(0))
                assertEquals(1799L, c.getLong(1))
                assertEquals(31, c.getInt(2))
                assertEquals(1, c.getInt(3))
                assertEquals("nota", c.getString(4))
                assertEquals(1, c.getInt(5))
            }
        }
    }

    @Test
    fun migrate1To3EncadenaAmbasMigraciones() {
        helper.createDatabase(dbName, 1).close()
        helper.runMigrationsAndValidate(
            dbName, 3, true, MisDinerosDatabase.MIGRATION_1_2, MisDinerosDatabase.MIGRATION_2_3,
        ).close()
    }
}
