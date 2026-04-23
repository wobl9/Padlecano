package com.example.padlecano.data.repository

import com.example.padlecano.domain.model.TournamentSummary
import kotlinx.coroutines.flow.Flow

interface TournamentRepository {
    fun observeTournamentSummaries(): Flow<List<TournamentSummary>>
    suspend fun createAmericanoTournament(title: String, playerDisplayNames: List<String>): Long
}
