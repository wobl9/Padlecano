package com.example.padlecano.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.padlecano.domain.model.EntityId

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
    @PrimaryKey val id: EntityId,
    val tournamentId: EntityId,
    val roundId: EntityId,
    val playerA1Index: Int,
    val playerA2Index: Int,
    val playerB1Index: Int,
    val playerB2Index: Int,
    val scoreA: Int = 0,
    val scoreB: Int = 0,
    val isScoreSet: Boolean = false,
    val ownerId: EntityId?,
    val updatedAt: Long,
    val version: Int,
    val deletedAt: Long?,
)
