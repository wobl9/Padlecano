package com.example.padlecano.domain.model

data class TournamentSummary(
    val id: EntityId,
    val title: String,
    val createdAtMillis: Long,
    val status: TournamentStatus,
    val tournamentType: TournamentType,
)
