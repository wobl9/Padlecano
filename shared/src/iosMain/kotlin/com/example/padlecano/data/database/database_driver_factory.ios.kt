package com.example.padlecano.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.example.padlecano.database.PadlecanoDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = PadlecanoDatabase.Schema,
            name = DATABASE_NAME,
        )
    }
    companion object {
        private const val DATABASE_NAME: String = "padlecano.db"
    }
}
