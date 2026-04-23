package com.example.padlecano.data.repository

import com.example.padlecano.data.local.TournamentDao
import com.example.padlecano.data.local.TournamentEntity
import com.example.padlecano.domain.model.TournamentStatus
import com.example.padlecano.domain.model.TournamentSummary
import com.example.padlecano.domain.model.TournamentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultTournamentRepository(
    private val tournamentDao: TournamentDao,
) : TournamentRepository {
    override fun observeTournamentSummaries(): Flow<List<TournamentSummary>> {
        return tournamentDao.observeTournaments().map { entities: List<TournamentEntity> ->
            entities.map { entity: TournamentEntity -> entity.toSummary() }
        }
    }
}

private fun TournamentEntity.toSummary(): TournamentSummary {
    return TournamentSummary(
        id = id,
        title = title,
        createdAtMillis = createdAtMillis,
        status = status.toTournamentStatus(),
        tournamentType = tournamentType.toTournamentType(),
    )
}

private fun String.toTournamentStatus(): TournamentStatus {
    return try {
        TournamentStatus.valueOf(this)
    } catch (_: IllegalArgumentException) {
        TournamentStatus.DRAFT
    }
}

private fun String.toTournamentType(): TournamentType {
    return try {
        TournamentType.valueOf(this)
    } catch (_: IllegalArgumentException) {
        TournamentType.AMERICANO
    }
}
