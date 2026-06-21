package com.example.padlecano

import android.app.Application
import com.example.padlecano.data.database.DatabaseDriverFactory
import com.example.padlecano.data.preferences.SavedPlayerNamesPreferencesRepository
import com.example.padlecano.data.repository.SqlDelightTournamentRepository
import com.example.padlecano.database.PadlecanoDatabase
import com.example.padlecano.domain.repository.SavedPlayerNamesRepository
import com.example.padlecano.domain.repository.TournamentRepository

class PadlecanoApplication : Application() {
    private val databaseDriverFactory: DatabaseDriverFactory by lazy {
        DatabaseDriverFactory(context = applicationContext)
    }
    private val padlecanoDatabase: PadlecanoDatabase by lazy {
        PadlecanoDatabase(databaseDriverFactory.createDriver())
    }
    val tournamentRepository: TournamentRepository by lazy {
        SqlDelightTournamentRepository(database = padlecanoDatabase)
    }
    val savedPlayerNamesRepository: SavedPlayerNamesRepository by lazy {
        SavedPlayerNamesPreferencesRepository(applicationContext = applicationContext)
    }
}
