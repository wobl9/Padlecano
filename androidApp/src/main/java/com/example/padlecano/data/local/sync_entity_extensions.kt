package com.example.padlecano.data.local

import com.example.padlecano.domain.model.EntityId
import com.example.padlecano.domain.model.SyncMetadata

fun TournamentEntity.syncMetadata(): SyncMetadata {
    return SyncMetadata(
        ownerId = ownerId,
        updatedAt = updatedAt,
        version = version,
        deletedAt = deletedAt,
    )
}

fun MatchEntity.syncMetadata(): SyncMetadata {
    return SyncMetadata(
        ownerId = ownerId,
        updatedAt = updatedAt,
        version = version,
        deletedAt = deletedAt,
    )
}

fun SyncMetadata.toEntityFields(): SyncEntityFields {
    return SyncEntityFields(
        ownerId = ownerId,
        updatedAt = updatedAt,
        version = version,
        deletedAt = deletedAt,
    )
}

data class SyncEntityFields(
    val ownerId: EntityId?,
    val updatedAt: Long,
    val version: Int,
    val deletedAt: Long?,
)
