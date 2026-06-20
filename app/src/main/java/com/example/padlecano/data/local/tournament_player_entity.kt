package com.example.padlecano.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.padlecano.domain.model.EntityId

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
    @PrimaryKey val id: EntityId,
    val tournamentId: EntityId,
    val displayName: String,
    val sortOrder: Int,
    val ownerId: EntityId?,
    val updatedAt: Long,
    val version: Int,
    val deletedAt: Long?,
)
