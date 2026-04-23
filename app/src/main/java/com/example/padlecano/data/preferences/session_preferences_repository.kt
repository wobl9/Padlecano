package com.example.padlecano.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

class SessionPreferencesRepository(private val applicationContext: Context) {
    private val loggedInKey: Preferences.Key<Boolean> = booleanPreferencesKey("logged_in")
    fun observeLoggedIn(): Flow<Boolean> {
        return applicationContext.sessionDataStore.data.map { preferences ->
            preferences[loggedInKey] ?: false
        }
    }
    suspend fun setLoggedIn(isLoggedIn: Boolean) {
        applicationContext.sessionDataStore.edit { preferences ->
            preferences[loggedInKey] = isLoggedIn
        }
    }
}
