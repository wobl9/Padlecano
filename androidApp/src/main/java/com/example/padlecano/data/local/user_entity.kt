package com.example.padlecano.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.padlecano.domain.model.EntityId

/** Registered app user; populated after Yandex auth (multiplayer phase). */
@Entity(
    tableName = "users",
    indices = [
        Index(value = ["yandexId"], unique = true),
        Index(value = ["handle"], unique = true),
    ],
)
data class UserEntity(
    @PrimaryKey val id: EntityId,
    val yandexId: String?,
    val handle: String?,
    val email: String?,
    val displayName: String,
    val rating: Double,
    val ownerId: EntityId?,
    val updatedAt: Long,
    val version: Int,
    val deletedAt: Long?,
)
