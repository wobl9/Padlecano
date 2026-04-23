package com.example.padlecano.data.repository

import com.example.padlecano.domain.model.TournamentSummary
import com.example.padlecano.domain.model.TournamentType
import kotlinx.coroutines.flow.Flow

interface TournamentRepository {
    fun observeTournamentSummaries(): Flow<List<TournamentSummary>>
}
