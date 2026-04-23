package com.example.padlecano.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.padlecano.PadlecanoApplication
import com.example.padlecano.data.repository.TournamentRepository
import com.example.padlecano.domain.model.TournamentType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CreateGameValidationError {
    NEED_AT_LEAST_FOUR_PLAYERS,
    PLAYER_COUNT_NOT_MULTIPLE_OF_FOUR,
    BLANK_PLAYER_NAME,
}

data class CreateGameUiState(
    val title: String = "",
    val playerNames: List<String> = (1..4).map { "" },
    val tournamentType: TournamentType = TournamentType.AMERICANO,
    val validationError: CreateGameValidationError? = null,
    val isSaving: Boolean = false,
)

class CreateGameViewModel(
    private val tournamentRepository: TournamentRepository,
) : ViewModel() {
    private val _uiState: MutableStateFlow<CreateGameUiState> = MutableStateFlow(CreateGameUiState())
    val uiState: StateFlow<CreateGameUiState> = _uiState.asStateFlow()
    private val navigationChannel: Channel<Long> = Channel(capacity = Channel.BUFFERED)
    val navigationEvents: Flow<Long> = navigationChannel.receiveAsFlow()
    fun updateTitle(value: String) {
        _uiState.update { current: CreateGameUiState ->
            current.copy(title = value, validationError = null)
        }
    }
    fun updatePlayerName(index: Int, value: String) {
        _uiState.update { current: CreateGameUiState ->
            val nextNames: MutableList<String> = current.playerNames.toMutableList()
            if (index in nextNames.indices) {
                nextNames[index] = value
            }
            current.copy(playerNames = nextNames, validationError = null)
        }
    }
    fun addFourPlayers() {
        _uiState.update { current: CreateGameUiState ->
            current.copy(
                playerNames = current.playerNames + (1..4).map { "" },
                validationError = null,
            )
        }
    }
    fun removeLastFourPlayers() {
        _uiState.update { current: CreateGameUiState ->
            if (current.playerNames.size <= 4) {
                current
            } else {
                current.copy(
                    playerNames = current.playerNames.dropLast(n = 4),
                    validationError = null,
                )
            }
        }
    }
    fun startTournament() {
        viewModelScope.launch {
            val error: CreateGameValidationError? = validate(state = _uiState.value)
            if (error != null) {
                _uiState.update { it.copy(validationError = error, isSaving = false) }
                return@launch
            }
            _uiState.update { it.copy(validationError = null, isSaving = true) }
            try {
                val snapshot: CreateGameUiState = _uiState.value
                val trimmedNames: List<String> = snapshot.playerNames.map { name: String -> name.trim() }
                val tournamentId: Long = tournamentRepository.createAmericanoTournament(
                    title = snapshot.title.trim(),
                    playerDisplayNames = trimmedNames,
                )
                _uiState.update { it.copy(isSaving = false) }
                navigationChannel.send(element = tournamentId)
            } catch (_: Exception) {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
    private fun validate(state: CreateGameUiState): CreateGameValidationError? {
        if (state.playerNames.size < 4) {
            return CreateGameValidationError.NEED_AT_LEAST_FOUR_PLAYERS
        }
        if (state.playerNames.size % 4 != 0) {
            return CreateGameValidationError.PLAYER_COUNT_NOT_MULTIPLE_OF_FOUR
        }
        val hasBlank: Boolean = state.playerNames.any { name: String -> name.isBlank() }
        if (hasBlank) {
            return CreateGameValidationError.BLANK_PLAYER_NAME
        }
        return null
    }
    companion object {
        fun createFactory(): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    val application = checkNotNull(
                        extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY],
                    ) as PadlecanoApplication
                    if (modelClass.isAssignableFrom(CreateGameViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return CreateGameViewModel(application.tournamentRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel type $modelClass")
                }
            }
        }
    }
}
