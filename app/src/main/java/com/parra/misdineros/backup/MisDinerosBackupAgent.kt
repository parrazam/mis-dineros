package com.parra.misdineros.backup

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.database.sqlite.SQLiteDatabase
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File

/**
 * Agente de backup para Mis Dineros.
 *
 * La app declara `android:fullBackupOnly="true"` en el manifest, así que el sistema
 * usa exclusivamente Auto Backup (file-based). Las reglas de qué incluir están en
 * `res/xml/data_extraction_rules.xml` (Android 12+) y `res/xml/backup_rules.xml`
 * (Android <12). Este agente NO duplica esas reglas: delega en `super.onFullBackup()`
 * para que el sistema las aplique.
 *
 * Las únicas responsabilidades de este agente son:
 *   1. Respetar el toggle de usuario (`auto_backup_prefs/enabled`).
 *   2. Hacer un checkpoint del WAL de SQLite antes de la copia, para que el fichero
 *      principal `.db` contenga todas las transacciones confirmadas.
 *
 * Las claves k/v (onBackup/onRestore) no se usan: con `fullBackupOnly=true` el sistema
 * no las invoca en Android 6+.
 */
class MisDinerosBackupAgent : BackupAgent() {

    override fun onBackup(
        oldState: ParcelFileDescriptor?,
        data: BackupDataOutput?,
        newState: ParcelFileDescriptor?,
    ) {
        // No-op: usamos Auto Backup (full-data) vía fullBackupOnly=true.
    }

    override fun onRestore(
        data: BackupDataInput?,
        appVersionCode: Int,
        newState: ParcelFileDescriptor?,
    ) {
        // No-op: la restauración full-data la gestiona el sistema con las reglas XML.
    }

    override fun onFullBackup(data: FullBackupDataOutput) {
        if (!isAutoBackupEnabled()) {
            Log.i(TAG, "onFullBackup: skip (toggle off)")
            return
        }

        // Checkpoint del WAL para que el .db sea consistente cuando el sistema lo copie.
        getDatabasePath(DB_NAME).takeIf { it.exists() }?.let(::checkpointWal)

        // Delega en el sistema: aplica las reglas de data_extraction_rules.xml
        // (cloud-backup / device-transfer) y backup_rules.xml según la versión de Android.
        super.onFullBackup(data)

        Log.i(TAG, "onFullBackup: completado")
    }

    private fun checkpointWal(dbFile: File) {
        try {
            SQLiteDatabase.openDatabase(
                dbFile.path, null,
                SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.NO_LOCALIZED_COLLATORS,
            ).use { db ->
                db.execSQL("PRAGMA wal_checkpoint(FULL)")
                Log.i(TAG, "WAL checkpoint completado")
            }
        } catch (e: Exception) {
            // Si falla, los ficheros -wal y -shm también se incluyen por las reglas XML,
            // así que el backup sigue siendo válido aunque menos compacto.
            Log.w(TAG, "WAL checkpoint fallido: $e")
        }
    }

    private fun isAutoBackupEnabled(): Boolean =
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, true)

    companion object {
        const val PREFS_NAME = "auto_backup_prefs"
        const val KEY_ENABLED = "enabled"
        private const val DB_NAME = "mis_dineros.db"
        private const val TAG = "MisDinerosBackup"
    }
}
