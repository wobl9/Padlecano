package com.example.padlecano.ui.summary

import androidx.compose.runtime.Composable
import com.example.padlecano.composeapp.generated.resources.Res
import com.example.padlecano.composeapp.generated.resources.*
import com.example.padlecano.domain.model.TournamentShareTextLabels
import org.jetbrains.compose.resources.stringResource

@Composable
fun rememberShareTextLabels(): TournamentShareTextLabels {
    return TournamentShareTextLabels(
        untitledTournament = stringResource(Res.string.tournament_untitled),
        tournamentTypeAmericano = stringResource(Res.string.tournament_type_americano),
        appFooter = stringResource(Res.string.app_name),
        standingsSectionTitle = stringResource(Res.string.summary_share_section_standings),
        matchesSectionTitle = stringResource(Res.string.summary_share_section_matches),
        rankColumn = stringResource(Res.string.summary_column_rank),
        playerColumn = stringResource(Res.string.summary_column_player),
        pointsColumn = stringResource(Res.string.summary_column_points),
        gamesColumn = stringResource(Res.string.summary_column_games),
        winsColumn = stringResource(Res.string.summary_column_wins),
        roundHeaderFormat = stringResource(Res.string.match_validity_round_header),
        courtLabelFormat = stringResource(Res.string.match_validity_court_label),
        teamVsFormat = stringResource(Res.string.match_validity_team_vs),
        scoreLineFormat = stringResource(Res.string.match_validity_score_line),
        scoreNotEntered = stringResource(Res.string.match_validity_score_not_entered),
        teamPairSeparator = stringResource(Res.string.summary_share_team_pair_separator),
    )
}

expect class PlatformShareHandler {
    fun copyToClipboard(text: String)
    fun sharePlainText(chooserTitle: String, text: String)
}

@Composable
expect fun rememberPlatformShareHandler(): PlatformShareHandler
