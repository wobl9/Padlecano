package com.example.padlecano.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.padlecano.R
import com.example.padlecano.domain.model.PlayerStandingRow
import com.example.padlecano.domain.model.SummarySortMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    viewModel: SummaryViewModel,
    onNavigateUp: () -> Unit,
    onNavigateToMatchValidity: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState: SummaryUiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.summary_title)) },
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
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = stringResource(R.string.summary_loading))
                }
            }
            uiState.loadFailed -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.summary_load_error),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            else -> {
                SummaryContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    uiState = uiState,
                    onSortModeChange = viewModel::setSortMode,
                    onNavigateToMatchValidity = onNavigateToMatchValidity,
                )
            }
        }
    }
}

@Composable
private fun SummaryContent(
    uiState: SummaryUiState,
    onSortModeChange: (SummarySortMode) -> Unit,
    onNavigateToMatchValidity: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (uiState.tournamentTitle.isNotBlank()) {
            Text(
                text = uiState.tournamentTitle,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = uiState.sortMode == SummarySortMode.POINTS_DESC,
                onClick = { onSortModeChange(SummarySortMode.POINTS_DESC) },
                label = { Text(text = stringResource(R.string.summary_sort_points)) },
            )
            FilterChip(
                selected = uiState.sortMode == SummarySortMode.MATCHES_DESC,
                onClick = { onSortModeChange(SummarySortMode.MATCHES_DESC) },
                label = { Text(text = stringResource(R.string.summary_sort_matches)) },
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        StandingsTable(rows = uiState.rows)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onNavigateToMatchValidity,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Filled.VerifiedUser,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.summary_open_match_validity))
        }
    }
}

@Composable
private fun StandingsTable(rows: List<PlayerStandingRow>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item {
            StandingsHeaderRow()
            HorizontalDivider()
        }
        itemsIndexed(
            items = rows,
            key = { _: Int, row: PlayerStandingRow -> row.playerIndex },
        ) { index: Int, row: PlayerStandingRow ->
            StandingsDataRow(rank = index + 1, row = row)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun StandingsHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.summary_column_rank),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(0.35f),
        )
        Text(
            text = stringResource(R.string.summary_column_player),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1.2f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.summary_column_points),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(0.55f),
        )
        Text(
            text = stringResource(R.string.summary_column_games),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(0.55f),
        )
        Text(
            text = stringResource(R.string.summary_column_wins),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(0.45f),
        )
    }
}

@Composable
private fun StandingsDataRow(rank: Int, row: PlayerStandingRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(0.35f),
        )
        Text(
            text = row.displayName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1.2f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = row.totalPoints.toString(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(0.55f),
        )
        Text(
            text = row.matchesPlayed.toString(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(0.55f),
        )
        Text(
            text = row.wins.toString(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(0.45f),
        )
    }
}
