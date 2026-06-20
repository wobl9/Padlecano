package com.example.padlecano.data.repository

import com.example.padlecano.domain.model.ActiveTournamentState
import com.example.padlecano.domain.model.EntityId
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
    ): EntityId
    fun observeActiveTournament(tournamentId: EntityId): Flow<ActiveTournamentState?>
    suspend fun saveMatchScores(scores: List<MatchScoreUpdate>)
    suspend fun finishTournament(tournamentId: EntityId)
    suspend fun deleteTournament(tournamentId: EntityId)
    suspend fun deleteAllTournaments()
    suspend fun loadTournamentResultsPayload(tournamentId: EntityId): TournamentResultsPayload?
    suspend fun loadMatchValidityAudit(tournamentId: EntityId): MatchValidityAudit?
}
