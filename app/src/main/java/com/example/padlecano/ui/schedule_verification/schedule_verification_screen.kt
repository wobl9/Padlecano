package com.example.padlecano.ui.schedule_verification

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
import com.example.padlecano.domain.model.OtherPlayerScheduleCheck
import com.example.padlecano.domain.model.PlayerScheduleVerification
import com.example.padlecano.domain.model.ScheduleVerificationReport

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleVerificationScreen(
    viewModel: ScheduleVerificationViewModel,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState: ScheduleVerificationUiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.verification_title)) },
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
        val report: ScheduleVerificationReport? = uiState.report
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
            uiState.loadFailed -> {
                Text(
                    text = stringResource(R.string.summary_load_error),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                )
            }
            report == null -> {
                Text(
                    text = stringResource(R.string.summary_load_error),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                )
            }
            else -> {
                ScheduleVerificationBody(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    report = report,
                )
            }
        }
    }
}

@Composable
private fun ScheduleVerificationBody(
    report: ScheduleVerificationReport,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.verification_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            val statusText: String = if (report.allOk) {
                stringResource(R.string.verification_status_ok)
            } else {
                stringResource(R.string.verification_status_fail)
            }
            val statusColor = if (report.allOk) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = statusColor,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (report.byPlayer.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.verification_invalid_roster),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            items(
                items = report.byPlayer,
                key = { row: PlayerScheduleVerification -> row.playerIndex },
            ) { row: PlayerScheduleVerification ->
                PlayerVerificationCard(
                    playerName = report.playerNames[row.playerIndex],
                    checks = row.withEachOtherPlayer,
                    nameResolver = { index: Int -> report.playerNames[index] },
                )
            }
        }
    }
}

@Composable
private fun PlayerVerificationCard(
    playerName: String,
    checks: List<OtherPlayerScheduleCheck>,
    nameResolver: (Int) -> String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = playerName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            checks.forEach { check: OtherPlayerScheduleCheck ->
                val otherName: String = nameResolver(check.otherPlayerIndex)
                val mark: String = stringResource(
                    if (check.partnerOk && check.opponentOk) {
                        R.string.verification_mark_ok
                    } else {
                        R.string.verification_mark_bad
                    },
                )
                Text(
                    text = stringResource(
                        R.string.verification_row_with_other,
                        otherName,
                        check.timesAsPartner,
                        check.expectedTimesAsPartner,
                        check.timesAsOpponent,
                        check.expectedTimesAsOpponent,
                        mark,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }
}
