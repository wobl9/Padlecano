package com.example.padlecano.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.padlecano.PadlecanoApplication
import com.example.padlecano.data.preferences.SavedPlayerNamesPreferencesRepository
import com.example.padlecano.data.repository.TournamentRepository
import com.example.padlecano.domain.model.TournamentType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CreateGameValidationError {
    NEED_AT_LEAST_FOUR_PLAYERS,
    PLAYER_COUNT_NOT_MULTIPLE_OF_FOUR,
    BLANK_PLAYER_NAME,
    INVALID_MAX_COMBINED_SCORE,
}

sealed interface CreateGameEvent {
    data object NoEmptySlotForSavedName : CreateGameEvent
}

data class CreateGameUiState(
    val title: String = "",
    val maxCombinedScoreInput: String = "6",
    val playerNames: List<String> = (1..4).map { "" },
    val tournamentType: TournamentType = TournamentType.AMERICANO,
    val validationError: CreateGameValidationError? = null,
    val isSaving: Boolean = false,
    val savedPlayerNames: List<String> = emptyList(),
)

class CreateGameViewModel(
    private val tournamentRepository: TournamentRepository,
    private val savedPlayerNamesRepository: SavedPlayerNamesPreferencesRepository,
) : ViewModel() {
    private val formState: MutableStateFlow<CreateGameUiState> = MutableStateFlow(CreateGameUiState())
    private val savedNamesState: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    private val navigationChannel: Channel<Long> = Channel(capacity = Channel.BUFFERED)
    private val eventChannel: Channel<CreateGameEvent> = Channel(capacity = Channel.BUFFERED)

    val navigationEvents: Flow<Long> = navigationChannel.receiveAsFlow()
    val events: Flow<CreateGameEvent> = eventChannel.receiveAsFlow()

    val uiState: StateFlow<CreateGameUiState> = combine(
        formState,
        savedNamesState,
    ) { form: CreateGameUiState, saved: List<String> ->
        form.copy(savedPlayerNames = saved)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
        initialValue = CreateGameUiState(),
    )

    init {
        viewModelScope.launch {
            savedPlayerNamesRepository.observeSavedNames().collect { names: List<String> ->
                savedNamesState.update { names }
            }
        }
    }

    fun updateTitle(value: String) {
        formState.update { current: CreateGameUiState ->
            current.copy(title = value, validationError = null)
        }
    }

    fun updateMaxCombinedScoreInput(value: String) {
        val filtered: String = value.filter { it.isDigit() }.take(3)
        formState.update { current: CreateGameUiState ->
            current.copy(maxCombinedScoreInput = filtered, validationError = null)
        }
    }

    fun updatePlayerName(index: Int, value: String) {
        formState.update { current: CreateGameUiState ->
            val nextNames: MutableList<String> = current.playerNames.toMutableList()
            if (index in nextNames.indices) {
                nextNames[index] = value
            }
            current.copy(playerNames = nextNames, validationError = null)
        }
    }

    fun addFourPlayers() {
        formState.update { current: CreateGameUiState ->
            current.copy(
                playerNames = current.playerNames + (1..4).map { "" },
                validationError = null,
            )
        }
    }

    fun removeLastFourPlayers() {
        formState.update { current: CreateGameUiState ->
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

    fun applySavedNameToNextEmptySlot(displayName: String) {
        val trimmed: String = displayName.trim()
        if (trimmed.isEmpty()) {
            return
        }
        val current: CreateGameUiState = formState.value
        val emptyIndex: Int = current.playerNames.indexOfFirst { name: String -> name.isBlank() }
        if (emptyIndex < 0) {
            viewModelScope.launch {
                eventChannel.send(element = CreateGameEvent.NoEmptySlotForSavedName)
            }
            return
        }
        formState.update { state: CreateGameUiState ->
            val slots: MutableList<String> = state.playerNames.toMutableList()
            if (emptyIndex in slots.indices) {
                slots[emptyIndex] = trimmed
            }
            state.copy(playerNames = slots, validationError = null)
        }
        viewModelScope.launch {
            savedPlayerNamesRepository.moveMatchingNameToEnd(displayName = trimmed)
        }
    }

    fun rememberFilledPlayerNamesFromForm() {
        viewModelScope.launch {
            val toSave: List<String> = formState.value.playerNames
                .map { name: String -> name.trim() }
                .filter { name: String -> name.isNotEmpty() }
            if (toSave.isEmpty()) {
                return@launch
            }
            savedPlayerNamesRepository.addNames(names = toSave)
        }
    }

    fun removeSavedPlayerName(displayName: String) {
        viewModelScope.launch {
            savedPlayerNamesRepository.removeName(displayName = displayName)
        }
    }

    fun startTournament() {
        viewModelScope.launch {
            val error: CreateGameValidationError? = validate(state = formState.value)
            if (error != null) {
                formState.update { it.copy(validationError = error, isSaving = false) }
                return@launch
            }
            formState.update { it.copy(validationError = null, isSaving = true) }
            try {
                val snapshot: CreateGameUiState = formState.value
                val trimmedNames: List<String> = snapshot.playerNames.map { name: String -> name.trim() }
                val maxCombined: Int = checkNotNull(snapshot.maxCombinedScoreInput.toIntOrNull())
                val tournamentId: Long = tournamentRepository.createAmericanoTournament(
                    title = snapshot.title.trim(),
                    playerDisplayNames = trimmedNames,
                    maxCombinedMatchScore = maxCombined,
                )
                formState.update { it.copy(isSaving = false) }
                navigationChannel.send(element = tournamentId)
            } catch (_: Exception) {
                formState.update { it.copy(isSaving = false) }
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
        val maxCombined: Int? = state.maxCombinedScoreInput.toIntOrNull()
        if (maxCombined == null || state.maxCombinedScoreInput.isBlank()) {
            return CreateGameValidationError.INVALID_MAX_COMBINED_SCORE
        }
        if (maxCombined < MIN_MAX_COMBINED_SCORE || maxCombined > MAX_MAX_COMBINED_SCORE) {
            return CreateGameValidationError.INVALID_MAX_COMBINED_SCORE
        }
        return null
    }

    companion object {
        private const val MIN_MAX_COMBINED_SCORE: Int = 1
        private const val MAX_MAX_COMBINED_SCORE: Int = 999
        fun createFactory(): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    val application: PadlecanoApplication = checkNotNull(
                        extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY],
                    ) as PadlecanoApplication
                    if (modelClass.isAssignableFrom(CreateGameViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return CreateGameViewModel(
                            tournamentRepository = application.tournamentRepository,
                            savedPlayerNamesRepository = application.savedPlayerNamesRepository,
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel type $modelClass")
                }
            }
        }
    }
}
