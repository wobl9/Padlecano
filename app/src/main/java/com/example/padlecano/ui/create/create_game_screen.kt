package com.example.padlecano.ui.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.padlecano.R
import com.example.padlecano.domain.model.TournamentType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGameScreen(
    viewModel: CreateGameViewModel,
    onNavigateUp: () -> Unit,
    onTournamentCreated: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState: CreateGameUiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(key1 = viewModel) {
        viewModel.navigationEvents.collect { tournamentId: Long ->
            onTournamentCreated(tournamentId)
        }
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.create_game_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_navigate_up),
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
                Text(text = stringResource(R.string.create_game_start))
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
                label = { Text(text = stringResource(R.string.create_game_field_tournament_title)) },
                singleLine = true,
            )
            OutlinedTextField(
                value = tournamentTypeLabel(type = uiState.tournamentType),
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                enabled = false,
                label = { Text(text = stringResource(R.string.create_game_field_tournament_type)) },
                singleLine = true,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = stringResource(R.string.create_game_players_section),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.create_game_players_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            uiState.playerNames.forEachIndexed { index: Int, name: String ->
                OutlinedTextField(
                    value = name,
                    onValueChange = { next: String -> viewModel.updatePlayerName(index = index, value = next) },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(text = stringResource(R.string.create_game_player_label, index + 1))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        keyboardType = KeyboardType.Text,
                    ),
                )
            }
            OutlinedButton(
                onClick = { viewModel.addFourPlayers() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.create_game_add_four_players))
            }
            OutlinedButton(
                onClick = { viewModel.removeLastFourPlayers() },
                enabled = uiState.playerNames.size > 4,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.create_game_remove_four_players))
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
        TournamentType.AMERICANO -> stringResource(R.string.tournament_type_americano)
    }
}

@Composable
private fun validationErrorMessage(error: CreateGameValidationError): String {
    return when (error) {
        CreateGameValidationError.NEED_AT_LEAST_FOUR_PLAYERS -> {
            stringResource(R.string.create_game_error_need_four)
        }
        CreateGameValidationError.PLAYER_COUNT_NOT_MULTIPLE_OF_FOUR -> {
            stringResource(R.string.create_game_error_multiple_of_four)
        }
        CreateGameValidationError.BLANK_PLAYER_NAME -> {
            stringResource(R.string.create_game_error_blank_name)
        }
    }
}
