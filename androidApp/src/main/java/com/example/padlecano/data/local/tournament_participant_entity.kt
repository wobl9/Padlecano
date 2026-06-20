package com.example.padlecano.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.padlecano.domain.model.EntityId

/**
 * Maps registered users to Americano slot indices.
 * Replaces free-text [TournamentPlayerEntity] once invitations are implemented.
 */
@Entity(
    tableName = "tournament_participants",
    foreignKeys = [
        ForeignKey(
            entity = TournamentEntity::class,
            parentColumns = ["id"],
            childColumns = ["tournamentId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["tournamentId"]),
        Index(value = ["userId"]),
        Index(value = ["tournamentId", "userId"], unique = true),
        Index(value = ["tournamentId", "slotIndex"], unique = true),
    ],
)
data class TournamentParticipantEntity(
    @PrimaryKey val id: EntityId,
    val tournamentId: EntityId,
    val userId: EntityId,
    val slotIndex: Int,
    val role: String,
    val ownerId: EntityId?,
    val updatedAt: Long,
    val version: Int,
    val deletedAt: Long?,
)
