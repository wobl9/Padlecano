package com.example.padlecano.ui.create

import com.example.padlecano.composeapp.generated.resources.Res
import com.example.padlecano.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.padlecano.domain.model.EntityId
import com.example.padlecano.domain.model.TournamentType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGameScreen(
    viewModel: CreateGameViewModel,
    onNavigateUp: () -> Unit,
    onTournamentCreated: (EntityId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState: CreateGameUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    val savedNamesHorizontalScroll = rememberScrollState()
    val noEmptySlotMessage: String = stringResource(Res.string.create_game_saved_name_no_empty_slot)
    val playerCount: Int = uiState.playerNames.size
    val playerFocusRequesters: List<FocusRequester> = remember(playerCount) {
        List(size = playerCount) { FocusRequester() }
    }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(key1 = viewModel) {
        launch {
            viewModel.navigationEvents.collect { tournamentId: EntityId ->
                onTournamentCreated(tournamentId)
            }
        }
        launch {
            viewModel.events.collect { event: CreateGameEvent ->
                when (event) {
                    CreateGameEvent.NoEmptySlotForSavedName -> {
                        snackbarHostState.showSnackbar(message = noEmptySlotMessage)
                    }
                }
            }
        }
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(Res.string.create_game_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.content_description_navigate_up),
                        )
                    }
                },
            )
        },
        bottomBar = {
            Button(
                onClick = { viewModel.startTournament() },
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(text = stringResource(Res.string.create_game_start))
            }
        },
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(Res.string.create_game_field_tournament_title)) },
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.maxCombinedScoreInput,
                onValueChange = viewModel::updateMaxCombinedScoreInput,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(Res.string.create_game_field_max_combined_score)) },
                supportingText = { Text(text = stringResource(Res.string.create_game_field_max_combined_score_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                value = tournamentTypeLabel(type = uiState.tournamentType),
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                enabled = false,
                label = { Text(text = stringResource(Res.string.create_game_field_tournament_type)) },
                singleLine = true,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = stringResource(Res.string.create_game_players_section),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(Res.string.create_game_players_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(Res.string.create_game_saved_names_section),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(Res.string.create_game_saved_names_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (uiState.savedPlayerNames.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(state = savedNamesHorizontalScroll),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    for (savedName: String in uiState.savedPlayerNames) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AssistChip(
                                onClick = { viewModel.applySavedNameToNextEmptySlot(displayName = savedName) },
                                label = { Text(text = savedName) },
                            )
                            IconButton(
                                onClick = { viewModel.removeSavedPlayerName(displayName = savedName) },
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(Res.string.content_description_remove_saved_name),
                                )
                            }
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = { viewModel.rememberFilledPlayerNamesFromForm() },
                enabled = uiState.playerNames.any { name: String -> name.isNotBlank() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(Res.string.create_game_save_names_button))
            }
            uiState.playerNames.forEachIndexed { index: Int, name: String ->
                val isLastPlayerField: Boolean = index == uiState.playerNames.lastIndex
                OutlinedTextField(
                    value = name,
                    onValueChange = { next: String -> viewModel.updatePlayerName(index = index, value = next) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester = playerFocusRequesters[index]),
                    label = {
                        Text(text = stringResource(Res.string.create_game_player_label, index + 1))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (!isLastPlayerField) {
                                playerFocusRequesters[index + 1].requestFocus()
                            } else {
                                focusManager.clearFocus()
                            }
                        },
                    ),
                )
            }
            OutlinedButton(
                onClick = { viewModel.addFourPlayers() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(Res.string.create_game_add_four_players))
            }
            OutlinedButton(
                onClick = { viewModel.removeLastFourPlayers() },
                enabled = uiState.playerNames.size > 4,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(Res.string.create_game_remove_four_players))
            }
            val errorMessage: String? = uiState.validationError?.let { err: CreateGameValidationError ->
                validationErrorMessage(error = err)
            }
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(modifier = Modifier.height(height = 8.dp))
        }
    }
}

@Composable
private fun tournamentTypeLabel(type: TournamentType): String {
    return when (type) {
        TournamentType.AMERICANO -> stringResource(Res.string.tournament_type_americano)
    }
}

@Composable
private fun validationErrorMessage(error: CreateGameValidationError): String {
    return when (error) {
        CreateGameValidationError.NEED_AT_LEAST_FOUR_PLAYERS -> {
            stringResource(Res.string.create_game_error_need_four)
        }
        CreateGameValidationError.PLAYER_COUNT_NOT_MULTIPLE_OF_FOUR -> {
            stringResource(Res.string.create_game_error_multiple_of_four)
        }
        CreateGameValidationError.BLANK_PLAYER_NAME -> {
            stringResource(Res.string.create_game_error_blank_name)
        }
        CreateGameValidationError.INVALID_MAX_COMBINED_SCORE -> {
            stringResource(Res.string.create_game_error_invalid_max_combined_score)
        }
    }
}
