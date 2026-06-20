package com.example.padlecano.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.padlecano.domain.repository.SavedPlayerNamesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.savedPlayerNamesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "saved_player_names",
)

class SavedPlayerNamesPreferencesRepository(
    private val applicationContext: Context,
) : SavedPlayerNamesRepository {
    private val blobKey: Preferences.Key<String> = stringPreferencesKey("names_blob")

    override fun observeSavedNames(): Flow<List<String>> {
        return applicationContext.savedPlayerNamesDataStore.data.map { preferences: Preferences ->
            parseBlob(raw = preferences[blobKey])
        }
    }

    override suspend fun addNames(names: Collection<String>) {
        applicationContext.savedPlayerNamesDataStore.edit { preferences ->
            val current: MutableList<String> = parseBlob(raw = preferences[blobKey]).toMutableList()
            for (raw: String in names) {
                val trimmed: String = raw.trim().replace("\r", "").replace('\n', ' ')
                if (trimmed.isEmpty()) {
                    continue
                }
                val exists: Boolean = current.any { existing: String ->
                    existing.equals(other = trimmed, ignoreCase = true)
                }
                if (!exists) {
                    current.add(trimmed)
                }
            }
            preferences[blobKey] = current.joinToString(separator = "\n")
        }
    }

    override suspend fun removeName(displayName: String) {
        val target: String = displayName.trim()
        if (target.isEmpty()) {
            return
        }
        applicationContext.savedPlayerNamesDataStore.edit { preferences ->
            val next: List<String> = parseBlob(raw = preferences[blobKey]).filter { existing: String ->
                !existing.equals(other = target, ignoreCase = true)
            }
            preferences[blobKey] = next.joinToString(separator = "\n")
        }
    }

    /**
     * Puts the matching saved name at the end of the list (most recently used last in UI order).
     */
    override suspend fun moveMatchingNameToEnd(displayName: String) {
        val target: String = displayName.trim()
        if (target.isEmpty()) {
            return
        }
        applicationContext.savedPlayerNamesDataStore.edit { preferences ->
            val current: MutableList<String> = parseBlob(raw = preferences[blobKey]).toMutableList()
            val index: Int = current.indexOfFirst { existing: String ->
                existing.equals(other = target, ignoreCase = true)
            }
            if (index < 0) {
                return@edit
            }
            val canonical: String = current.removeAt(index = index)
            current.add(element = canonical)
            preferences[blobKey] = current.joinToString(separator = "\n")
        }
    }

    private fun parseBlob(raw: String?): List<String> {
        if (raw.isNullOrBlank()) {
            return emptyList()
        }
        return raw.lines()
            .map { line: String -> line.trim() }
            .filter { line: String -> line.isNotEmpty() }
    }
}
