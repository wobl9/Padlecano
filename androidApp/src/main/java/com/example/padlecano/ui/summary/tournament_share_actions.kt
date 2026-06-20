package com.example.padlecano.ui.summary

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.example.padlecano.R
import com.example.padlecano.domain.model.TournamentShareTextLabels

fun buildShareTextLabels(context: Context): TournamentShareTextLabels {
    return TournamentShareTextLabels(
        untitledTournament = context.getString(R.string.tournament_untitled),
        tournamentTypeAmericano = context.getString(R.string.tournament_type_americano),
        appFooter = context.getString(R.string.app_name),
        standingsSectionTitle = context.getString(R.string.summary_share_section_standings),
        matchesSectionTitle = context.getString(R.string.summary_share_section_matches),
        rankColumn = context.getString(R.string.summary_column_rank),
        playerColumn = context.getString(R.string.summary_column_player),
        pointsColumn = context.getString(R.string.summary_column_points),
        gamesColumn = context.getString(R.string.summary_column_games),
        winsColumn = context.getString(R.string.summary_column_wins),
        roundHeaderFormat = context.getString(R.string.match_validity_round_header),
        courtLabelFormat = context.getString(R.string.match_validity_court_label),
        teamVsFormat = context.getString(R.string.match_validity_team_vs),
        scoreLineFormat = context.getString(R.string.match_validity_score_line),
        scoreNotEntered = context.getString(R.string.match_validity_score_not_entered),
        teamPairSeparator = context.getString(R.string.summary_share_team_pair_separator),
    )
}

fun copyTextToClipboard(context: Context, text: String) {
    val clipboardManager: ClipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData: ClipData = ClipData.newPlainText("tournament_results", text)
    clipboardManager.setPrimaryClip(clipData)
}

fun sharePlainText(context: Context, chooserTitle: String, text: String) {
    val sendIntent: Intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooserIntent: Intent = Intent.createChooser(sendIntent, chooserTitle)
    context.startActivity(chooserIntent)
}
