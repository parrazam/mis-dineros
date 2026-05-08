package com.parra.misdineros.backup

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.database.sqlite.SQLiteDatabase
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File

class MisDinerosBackupAgent : BackupAgent() {

    override fun onBackup(
        oldState: ParcelFileDescriptor?,
        data: BackupDataOutput?,
        newState: ParcelFileDescriptor?,
    ) {
        // Key/value backup no utilizado; todo va por Full Backup.
    }

    override fun onRestore(
        data: BackupDataInput?,
        appVersionCode: Int,
        newState: ParcelFileDescriptor?,
    ) {
        // Restauración key/value no utilizada.
    }

    override fun onFullBackup(data: FullBackupDataOutput) {
        val enabled = isAutoBackupEnabled()
        Log.i(TAG, "onFullBackup: autoBackupEnabled=$enabled")
        if (!enabled) return

        // Base de datos Room: checkpoint del WAL antes de copiar para garantizar
        // que el fichero principal tiene todas las transacciones confirmadas.
        getDatabasePath(DB_NAME).also { db ->
            if (db.exists()) {
                checkpointWal(db)
                fullBackupFile(db, data)
                // Incluir WAL y SHM solo si aún existen tras el checkpoint.
                File("${db.path}-shm").takeIf { it.exists() }?.let { fullBackupFile(it, data) }
                File("${db.path}-wal").takeIf { it.exists() }?.let { fullBackupFile(it, data) }
            }
        }

        // DataStore (settings.preferences_pb)
        File(filesDir, "datastore").listFiles()?.forEach { fullBackupFile(it, data) }

        // Iconos de usuario
        File(filesDir, "icons").listFiles()?.forEach { fullBackupFile(it, data) }
        File(filesDir, "category_icons").listFiles()?.forEach { fullBackupFile(it, data) }

        // Mirror de SharedPreferences para el propio toggle
        File(applicationInfo.dataDir, "shared_prefs/$PREFS_NAME.xml")
            .takeIf { it.exists() }?.let { fullBackupFile(it, data) }

        Log.i(TAG, "onFullBackup: completado")
    }

    // Restauración de ficheros individuales delegada al sistema.
    // No bloqueamos restore aunque el usuario haya desactivado el backup.

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
            Log.w(TAG, "WAL checkpoint fallido (se incluirán los ficheros WAL): $e")
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
