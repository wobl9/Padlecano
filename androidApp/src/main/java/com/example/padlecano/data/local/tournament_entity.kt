package com.example.padlecano.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.padlecano.domain.model.EntityId

@Entity(tableName = "tournaments")
data class TournamentEntity(
    @PrimaryKey val id: EntityId,
    val title: String,
    val createdAtMillis: Long,
    val status: String,
    val tournamentType: String,
    /** Maximum allowed scoreA + scoreB for a single match in this tournament. */
    val maxCombinedMatchScore: Int,
    val ownerId: EntityId?,
    val updatedAt: Long,
    val version: Int,
    val deletedAt: Long?,
)
