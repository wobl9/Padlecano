package com.example.padlecano.domain.model

/**
 * Sync fields shared by all persistable entities.
 * Tournament is the sync aggregate; nested rows carry the same fields for future delta export.
 */
data class SyncMetadata(
    val ownerId: EntityId?,
    val updatedAt: Long,
    val version: Int,
    val deletedAt: Long?,
) {
    companion object {
        fun createNew(
            ownerId: EntityId? = null,
            nowMillis: Long = System.currentTimeMillis(),
        ): SyncMetadata {
            return SyncMetadata(
                ownerId = ownerId,
                updatedAt = nowMillis,
                version = INITIAL_VERSION,
                deletedAt = null,
            )
        }
        fun bump(
            previous: SyncMetadata,
            nowMillis: Long = System.currentTimeMillis(),
        ): SyncMetadata {
            return previous.copy(
                updatedAt = nowMillis,
                version = previous.version + VERSION_INCREMENT,
            )
        }
        fun markDeleted(
            previous: SyncMetadata,
            nowMillis: Long = System.currentTimeMillis(),
        ): SyncMetadata {
            return previous.copy(
                updatedAt = nowMillis,
                version = previous.version + VERSION_INCREMENT,
                deletedAt = nowMillis,
            )
        }
        private const val INITIAL_VERSION: Int = 1
        private const val VERSION_INCREMENT: Int = 1
    }
}
