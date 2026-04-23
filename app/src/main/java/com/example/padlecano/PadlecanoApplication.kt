package com.example.padlecano

import android.app.Application
import androidx.room.Room
import com.example.padlecano.data.local.PadlecanoDatabase
import com.example.padlecano.data.repository.DefaultTournamentRepository
import com.example.padlecano.data.repository.TournamentRepository

class PadlecanoApplication : Application() {
    private val database: PadlecanoDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            PadlecanoDatabase::class.java,
            "padlecano.db",
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }
    val tournamentRepository: TournamentRepository by lazy {
        DefaultTournamentRepository(tournamentDao = database.tournamentDao())
    }
}
