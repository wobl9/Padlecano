package com.example.padlecano.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.padlecano.data.preferences.SessionPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface SessionBootstrapState {
    data object Loading : SessionBootstrapState
    data class Ready(val isLoggedIn: Boolean) : SessionBootstrapState
}

class SessionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SessionPreferencesRepository = SessionPreferencesRepository(
        applicationContext = application.applicationContext,
    )
    private val _bootstrapState: MutableStateFlow<SessionBootstrapState> =
        MutableStateFlow(SessionBootstrapState.Loading)
    val bootstrapState: StateFlow<SessionBootstrapState> = _bootstrapState.asStateFlow()
    init {
        viewModelScope.launch {
            val isLoggedIn: Boolean = repository.observeLoggedIn().first()
            _bootstrapState.value = SessionBootstrapState.Ready(isLoggedIn = isLoggedIn)
        }
    }
    suspend fun persistLoggedIn() {
        repository.setLoggedIn(isLoggedIn = true)
        _bootstrapState.value = SessionBootstrapState.Ready(isLoggedIn = true)
    }
}
