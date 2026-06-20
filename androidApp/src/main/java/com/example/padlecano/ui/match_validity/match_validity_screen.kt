package com.example.padlecano.ui.match_validity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.padlecano.R
import com.example.padlecano.domain.model.AuditedMatchRow
import com.example.padlecano.domain.model.MatchValidityAudit
import com.example.padlecano.domain.model.MatchValidityIssue
import com.example.padlecano.domain.model.RoundValiditySection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchValidityScreen(
    viewModel: MatchValidityViewModel,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState: MatchValidityUiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.match_validity_title)) },
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
        val audit: MatchValidityAudit? = uiState.audit
        when {
            uiState.isLoading -> {
                Text(
                    text = stringResource(R.string.summary_loading),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                )
            }
            uiState.loadFailed || audit == null -> {
                Text(
                    text = stringResource(R.string.summary_load_error),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                )
            }
            else -> {
                MatchValidityBody(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    audit = audit,
                )
            }
        }
    }
}

@Composable
private fun MatchValidityBody(
    audit: MatchValidityAudit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            if (audit.tournamentTitle.isNotBlank()) {
                Text(
                    text = audit.tournamentTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = stringResource(R.string.match_validity_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            val statusText: String = if (audit.allMatchesValid) {
                stringResource(R.string.match_validity_all_ok)
            } else {
                stringResource(R.string.match_validity_some_issues)
            }
            val statusColor = if (audit.allMatchesValid) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = statusColor,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (audit.rounds.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.match_validity_empty),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            audit.rounds.forEach { section: RoundValiditySection ->
                item(key = "round_header_${section.roundNumber}") {
                    Text(
                        text = stringResource(
                            R.string.match_validity_round_header,
                            section.roundNumber,
                            audit.rounds.size,
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                    )
                }
                items(
                    items = section.matches,
                    key = { row: AuditedMatchRow -> row.matchId },
                ) { row: AuditedMatchRow ->
                    MatchValidityCard(
                        row = row,
                        maxCombinedMatchScore = audit.maxCombinedMatchScore,
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchValidityCard(
    row: AuditedMatchRow,
    maxCombinedMatchScore: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.match_validity_court_label, row.courtNumber),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.match_validity_team_vs, row.teamAPairLabel, row.teamBPairLabel),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(6.dp))
            val scoreLine: String = if (row.isScoreEntered) {
                stringResource(R.string.match_validity_score_line, row.scoreA, row.scoreB)
            } else {
                stringResource(R.string.match_validity_score_not_entered)
            }
            Text(
                text = stringResource(R.string.match_validity_score_label, scoreLine),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (row.issues.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                row.issues.forEach { issue: MatchValidityIssue ->
                    Text(
                        text = "• ${issueText(issue = issue, maxCombinedMatchScore = maxCombinedMatchScore)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else if (row.isScoreEntered) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.match_validity_row_ok),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun issueText(issue: MatchValidityIssue, maxCombinedMatchScore: Int): String {
    return when (issue) {
        MatchValidityIssue.INVALID_PLAYER_INDEX -> stringResource(R.string.match_validity_issue_bad_index)
        MatchValidityIssue.DUPLICATE_PLAYERS_ON_COURT -> {
            stringResource(R.string.match_validity_issue_duplicate_court)
        }
        MatchValidityIssue.SAME_PLAYER_TWICE_ON_TEAM_A -> {
            stringResource(R.string.match_validity_issue_team_a_duplicate)
        }
        MatchValidityIssue.SAME_PLAYER_TWICE_ON_TEAM_B -> {
            stringResource(R.string.match_validity_issue_team_b_duplicate)
        }
        MatchValidityIssue.NEGATIVE_SCORE -> stringResource(R.string.match_validity_issue_negative_score)
        MatchValidityIssue.COMBINED_SCORE_EXCEEDS_LIMIT -> {
            stringResource(R.string.match_validity_issue_combined_exceeds, maxCombinedMatchScore)
        }
    }
}
