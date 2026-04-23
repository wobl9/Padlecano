package com.example.padlecano.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "matches",
    foreignKeys = [
        ForeignKey(
            entity = RoundEntity::class,
            parentColumns = ["id"],
            childColumns = ["roundId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TournamentEntity::class,
            parentColumns = ["id"],
            childColumns = ["tournamentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["roundId"]), Index(value = ["tournamentId"])],
)
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val tournamentId: Long,
    val roundId: Long,
    val playerA1Index: Int,
    val playerA2Index: Int,
    val playerB1Index: Int,
    val playerB2Index: Int,
    val scoreA: Int = 0,
    val scoreB: Int = 0,
    val isScoreSet: Boolean = false,
)
