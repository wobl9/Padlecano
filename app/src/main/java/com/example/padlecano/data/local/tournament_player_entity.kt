package com.example.padlecano.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tournament_players",
    foreignKeys = [
        ForeignKey(
            entity = TournamentEntity::class,
            parentColumns = ["id"],
            childColumns = ["tournamentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["tournamentId"])],
)
data class TournamentPlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val tournamentId: Long,
    val displayName: String,
    val sortOrder: Int,
)
