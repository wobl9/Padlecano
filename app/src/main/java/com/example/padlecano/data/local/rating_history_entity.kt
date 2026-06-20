package com.example.padlecano.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.padlecano.domain.model.EntityId

/** Elo rating change audit trail (multiplayer phase). */
@Entity(
    tableName = "rating_history",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TournamentEntity::class,
            parentColumns = ["id"],
            childColumns = ["tournamentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["tournamentId"]),
    ],
)
data class RatingHistoryEntity(
    @PrimaryKey val id: EntityId,
    val userId: EntityId,
    val tournamentId: EntityId,
    val ratingBefore: Double,
    val ratingAfter: Double,
    val createdAtMillis: Long,
    val ownerId: EntityId?,
    val updatedAt: Long,
    val version: Int,
    val deletedAt: Long?,
)
