package com.example.padlecano.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TournamentEntity::class,
        TournamentPlayerEntity::class,
        RoundEntity::class,
        MatchEntity::class,
        UserEntity::class,
        TournamentParticipantEntity::class,
        InvitationEntity::class,
        RatingHistoryEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class PadlecanoDatabase : RoomDatabase() {
    abstract fun tournamentDao(): TournamentDao
}
