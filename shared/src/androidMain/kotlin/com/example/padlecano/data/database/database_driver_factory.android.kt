package com.example.padlecano.data.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.padlecano.database.PadlecanoDatabase

actual class DatabaseDriverFactory(
    private val context: Context,
) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = PadlecanoDatabase.Schema,
            context = context,
            name = DATABASE_NAME,
        )
    }
    companion object {
        private const val DATABASE_NAME: String = "padlecano.db"
    }
}
