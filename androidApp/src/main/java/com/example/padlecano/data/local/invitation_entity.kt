package com.example.padlecano.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.padlecano.domain.model.EntityId

/** Tournament invite by share code or targeted user (multiplayer phase). */
@Entity(
    tableName = "invitations",
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
            childColumns = ["invitedUserId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["tournamentId"]),
        Index(value = ["code"], unique = true),
        Index(value = ["invitedUserId"]),
    ],
)
data class InvitationEntity(
    @PrimaryKey val id: EntityId,
    val tournamentId: EntityId,
    val code: String,
    val invitedUserId: EntityId?,
    val status: String,
    val ownerId: EntityId?,
    val updatedAt: Long,
    val version: Int,
    val deletedAt: Long?,
)
