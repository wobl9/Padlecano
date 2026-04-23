package com.example.padlecano.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TournamentEntity::class,
        TournamentPlayerEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class PadlecanoDatabase : RoomDatabase() {
    abstract fun tournamentDao(): TournamentDao
}
