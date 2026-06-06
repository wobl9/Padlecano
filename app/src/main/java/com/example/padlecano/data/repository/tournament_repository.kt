package com.example.padlecano.data.repository

import com.example.padlecano.domain.model.ActiveTournamentState
import com.example.padlecano.domain.model.MatchScoreUpdate
import com.example.padlecano.domain.model.MatchValidityAudit
import com.example.padlecano.domain.model.TournamentResultsPayload
import com.example.padlecano.domain.model.TournamentSummary
import kotlinx.coroutines.flow.Flow

interface TournamentRepository {
    fun observeTournamentSummaries(): Flow<List<TournamentSummary>>
    suspend fun createAmericanoTournament(
        title: String,
        playerDisplayNames: List<String>,
        maxCombinedMatchScore: Int,
    ): Long
    fun observeActiveTournament(tournamentId: Long): Flow<ActiveTournamentState?>
    suspend fun saveMatchScores(scores: List<MatchScoreUpdate>)
    suspend fun finishTournament(tournamentId: Long)
    suspend fun deleteTournament(tournamentId: Long)
    suspend fun deleteAllTournaments()
    suspend fun loadTournamentResultsPayload(tournamentId: Long): TournamentResultsPayload?
    suspend fun loadMatchValidityAudit(tournamentId: Long): MatchValidityAudit?
}
