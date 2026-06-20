package com.example.padlecano.domain.repository

import kotlinx.coroutines.flow.Flow

interface SavedPlayerNamesRepository {
    fun observeSavedNames(): Flow<List<String>>
    suspend fun addNames(names: Collection<String>)
    suspend fun removeName(displayName: String)
    suspend fun moveMatchingNameToEnd(displayName: String)
}
