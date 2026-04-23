package com.example.padlecano.ui.games

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.padlecano.R
import com.example.padlecano.domain.model.TournamentStatus
import com.example.padlecano.domain.model.TournamentSummary
import com.example.padlecano.domain.model.TournamentType
import com.example.padlecano.ui.theme.PadlecanoTheme
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesListScreen(
    uiState: GamesListUiState,
    onCreateGameClick: () -> Unit,
    onDeleteAllGamesClick: () -> Unit,
    onTournamentClick: (TournamentSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteAllDialog: Boolean by remember { mutableStateOf(value = false) }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.games_title)) },
                actions = {
                    TextButton(
                        onClick = { showDeleteAllDialog = true },
                        enabled = uiState.tournaments.isNotEmpty(),
                    ) {
                        Text(text = stringResource(R.string.games_delete_all))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateGameClick) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.games_fab_new_game),
                )
            }
        },
    ) { innerPadding ->
        if (showDeleteAllDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAllDialog = false },
                title = { Text(text = stringResource(R.string.games_delete_all_confirm_title)) },
                text = { Text(text = stringResource(R.string.games_delete_all_confirm_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteAllDialog = false
                            onDeleteAllGamesClick()
                        },
                    ) {
                        Text(text = stringResource(R.string.games_delete_all_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllDialog = false }) {
                        Text(text = stringResource(R.string.games_delete_all_cancel))
                    }
                },
            )
        }
        if (uiState.tournaments.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.games_empty_hint),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = uiState.tournaments,
                    key = { item: TournamentSummary -> item.id },
                ) { tournament: TournamentSummary ->
                    TournamentRow(
                        tournament = tournament,
                        onClick = { onTournamentClick(tournament) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TournamentRow(
    tournament: TournamentSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleText: String = if (tournament.title.isBlank()) {
        stringResource(R.string.tournament_untitled)
    } else {
        tournament.title
    }
    val formattedDate: String = remember(tournament.createdAtMillis) {
        DateFormat.getDateTimeInstance(
            DateFormat.SHORT,
            DateFormat.SHORT,
            Locale.getDefault(),
        ).format(Date(tournament.createdAtMillis))
    }
    val statusLabel: String = when (tournament.status) {
        TournamentStatus.DRAFT -> stringResource(R.string.tournament_status_draft)
        TournamentStatus.ACTIVE -> stringResource(R.string.tournament_status_active)
        TournamentStatus.FINISHED -> stringResource(R.string.tournament_status_finished)
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = titleText, style = MaterialTheme.typography.titleMedium)
            Text(text = statusLabel, style = MaterialTheme.typography.labelLarge)
            Text(text = formattedDate, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GamesListScreenPreview() {
    PadlecanoTheme {
        GamesListScreen(
            uiState = GamesListUiState(
                tournaments = listOf(
                    TournamentSummary(
                        id = 1L,
                        title = "Morning Americano",
                        createdAtMillis = 1_700_000_000_000L,
                        status = TournamentStatus.ACTIVE,
                        tournamentType = TournamentType.AMERICANO,
                    ),
                ),
            ),
            onCreateGameClick = {},
            onDeleteAllGamesClick = {},
            onTournamentClick = {},
        )
    }
}
