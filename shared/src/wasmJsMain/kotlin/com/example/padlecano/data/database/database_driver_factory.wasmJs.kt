package com.example.padlecano.data.database

import app.cash.sqldelight.db.SqlDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        throw UnsupportedOperationException("Local SQLDelight database is not supported on wasmJs yet.")
    }
}
