package com.parra.misdineros.core.result

sealed interface AppError {
    data class Database(val message: String) : AppError
    data class ImportExport(val message: String) : AppError
    data class Validation(val message: String) : AppError
    data object Unknown : AppError
}
