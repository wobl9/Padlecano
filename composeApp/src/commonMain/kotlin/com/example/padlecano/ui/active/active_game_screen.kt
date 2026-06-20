package com.example.padlecano.ui.active

import com.example.padlecano.composeapp.generated.resources.Res
import com.example.padlecano.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.padlecano.domain.model.EntityId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveGameScreen(
    viewModel: ActiveGameViewModel,
    onNavigateUp: () -> Unit,
    onNavigateToSummary: (EntityId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState: ActiveGameUiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(key1 = viewModel) {
        viewModel.events.collect { event: ActiveGameEvent ->
            when (event) {
                is ActiveGameEvent.NavigateToSummary -> onNavigateToSummary(event.tournamentId)
            }
        }
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    val title: String = uiState.tournamentTitle.ifBlank {
                        stringResource(Res.string.active_game_title)
                    }
                    Text(text = title)
                },
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
            if (!uiState.isLoading) {
                val buttonLabel: String = if (uiState.currentRoundNumber == uiState.totalRounds) {
                    stringResource(Res.string.active_game_finish_tournament)
                } else {
                    stringResource(Res.string.active_game_confirm_round)
                }
                Button(
                    onClick = { viewModel.confirmRound() },
                    enabled = !uiState.isSaving &&
                        uiState.courts.isNotEmpty() &&
                        uiState.allScoreFieldsFilled &&
                        !uiState.combinedScoreExceedsLimit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(text = buttonLabel)
                }
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.courts.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.active_game_all_rounds_done),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "progress") {
                        RoundProgressHeader(
                            currentRound = uiState.currentRoundNumber,
                            totalRounds = uiState.totalRounds,
                            maxCombinedMatchScore = uiState.maxCombinedMatchScore,
                        )
                    }
                    items(
                        items = uiState.courts,
                        key = { court: CourtUiModel -> court.matchId },
                    ) { court: CourtUiModel ->
                        val draft: ScoreInput = uiState.draftScores[court.matchId] ?: ScoreInput()
                        CourtCard(
                            court = court,
                            scoreInput = draft,
                            onScoreAChange = { value: String -> viewModel.updateScoreA(court.matchId, value) },
                            onScoreBChange = { value: String -> viewModel.updateScoreB(court.matchId, value) },
                        )
                    }
                    if (uiState.combinedScoreExceedsLimit) {
                        item(key = "error_exceeds") {
                            Text(
                                text = stringResource(
                                    Res.string.active_game_error_exceeds_play_to_total,
                                    uiState.maxCombinedMatchScore,
                                ),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                    } else if (uiState.showMissingScoresMessage) {
                        item(key = "error_missing") {
                            Text(
                                text = stringResource(Res.string.active_game_error_missing_scores),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                    }
                    item(key = "spacer") { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun RoundProgressHeader(
    currentRound: Int,
    totalRounds: Int,
    maxCombinedMatchScore: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(Res.string.active_game_round_progress, currentRound, totalRounds),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(Res.string.active_game_play_to_total_hint, maxCombinedMatchScore),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { currentRound.toFloat() / totalRounds.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CourtCard(
    court: CourtUiModel,
    scoreInput: ScoreInput,
    onScoreAChange: (String) -> Unit,
    onScoreBChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(Res.string.active_game_court_number, court.courtNumber),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = court.teamAName1, style = MaterialTheme.typography.bodyMedium)
                    Text(text = court.teamAName2, style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ScoreField(value = scoreInput.scoreA, onValueChange = onScoreAChange)
                    Text(text = ":", style = MaterialTheme.typography.titleLarge)
                    ScoreField(value = scoreInput.scoreB, onValueChange = onScoreBChange)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = court.teamBName1,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.End,
                    )
                    Text(
                        text = court.teamBName2,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val isFocused: Boolean by interactionSource.collectIsFocusedAsState()
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.width(64.dp),
        interactionSource = interactionSource,
        textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        placeholder = if (value.isEmpty() && !isFocused) {
            {
                Text(
                    text = "0",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        } else {
            null
        },
    )
}
