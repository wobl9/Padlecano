package com.example.padlecano.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TournamentEntity::class,
        TournamentPlayerEntity::class,
        RoundEntity::class,
        MatchEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class PadlecanoDatabase : RoomDatabase() {
    abstract fun tournamentDao(): TournamentDao
}
