package com.example.padlecano.data.repository

import com.example.padlecano.data.local.MatchEntity
import com.example.padlecano.data.local.RoundEntity
import com.example.padlecano.data.local.RoundWithMatches
import com.example.padlecano.data.local.TournamentDao
import com.example.padlecano.data.local.TournamentEntity
import com.example.padlecano.data.local.TournamentPlayerEntity
import com.example.padlecano.domain.model.ActiveTournamentState
import com.example.padlecano.domain.model.MatchScoreUpdate
import com.example.padlecano.domain.model.MatchState
import com.example.padlecano.domain.model.RoundState
import com.example.padlecano.domain.model.TournamentStatus
import com.example.padlecano.domain.model.TournamentSummary
import com.example.padlecano.domain.model.TournamentType
import com.example.padlecano.domain.usecase.AmericanoScheduleGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class DefaultTournamentRepository(
    private val tournamentDao: TournamentDao,
) : TournamentRepository {

    override fun observeTournamentSummaries(): Flow<List<TournamentSummary>> {
        return tournamentDao.observeTournaments().map { entities: List<TournamentEntity> ->
            entities.map { entity: TournamentEntity -> entity.toSummary() }
        }
    }

    override suspend fun createAmericanoTournament(
        title: String,
        playerDisplayNames: List<String>,
        maxCombinedMatchScore: Int,
    ): Long {
        val tournament = TournamentEntity(
            title = title,
            createdAtMillis = System.currentTimeMillis(),
            status = TournamentStatus.ACTIVE.name,
            tournamentType = TournamentType.AMERICANO.name,
            maxCombinedMatchScore = maxCombinedMatchScore,
        )
        val players: List<TournamentPlayerEntity> = playerDisplayNames.mapIndexed { index: Int, name: String ->
            TournamentPlayerEntity(tournamentId = 0L, displayName = name, sortOrder = index)
        }
        val tournamentId: Long = tournamentDao.insertTournamentWithPlayers(tournament, players)
        val schedule: List<List<AmericanoScheduleGenerator.MatchSetup>> =
            AmericanoScheduleGenerator.generate(playerDisplayNames.size)
        val roundIds: List<Long> = tournamentDao.insertRounds(
            schedule.mapIndexed { index: Int, _ ->
                RoundEntity(tournamentId = tournamentId, roundNumber = index + 1)
            },
        )
        val allMatches: List<MatchEntity> = schedule.flatMapIndexed { roundIndex: Int, setups: List<AmericanoScheduleGenerator.MatchSetup> ->
            setups.map { setup: AmericanoScheduleGenerator.MatchSetup ->
                MatchEntity(
                    tournamentId = tournamentId,
                    roundId = roundIds[roundIndex],
                    playerA1Index = setup.playerA1,
                    playerA2Index = setup.playerA2,
                    playerB1Index = setup.playerB1,
                    playerB2Index = setup.playerB2,
                )
            }
        }
        if (allMatches.isNotEmpty()) {
            tournamentDao.insertMatches(allMatches)
        }
        return tournamentId
    }

    override fun observeActiveTournament(tournamentId: Long): Flow<ActiveTournamentState?> = flow {
        val tournamentEntity = tournamentDao.getTournamentById(tournamentId)
        if (tournamentEntity == null) {
            emit(null)
            return@flow
        }
        val players: List<String> = tournamentDao.getPlayersByTournamentId(tournamentId)
            .sortedBy { it.sortOrder }
            .map { it.displayName }
        tournamentDao.observeRoundsWithMatches(tournamentId).collect { roundsWithMatches: List<RoundWithMatches> ->
            emit(
                ActiveTournamentState(
                    tournamentId = tournamentEntity.id,
                    title = tournamentEntity.title,
                    players = players,
                    maxCombinedMatchScore = tournamentEntity.maxCombinedMatchScore,
                    rounds = roundsWithMatches.map { rwm: RoundWithMatches ->
                        RoundState(
                            roundId = rwm.round.id,
                            roundNumber = rwm.round.roundNumber,
                            matches = rwm.matches.sortedBy { it.id }.map { match ->
                                MatchState(
                                    matchId = match.id,
                                    playerA1Index = match.playerA1Index,
                                    playerA2Index = match.playerA2Index,
                                    playerB1Index = match.playerB1Index,
                                    playerB2Index = match.playerB2Index,
                                    scoreA = match.scoreA,
                                    scoreB = match.scoreB,
                                    isScoreSet = match.isScoreSet,
                                )
                            },
                        )
                    },
                ),
            )
        }
    }

    override suspend fun saveMatchScores(scores: List<MatchScoreUpdate>) {
        scores.forEach { update: MatchScoreUpdate ->
            tournamentDao.updateMatchScore(
                matchId = update.matchId,
                scoreA = update.scoreA,
                scoreB = update.scoreB,
            )
        }
    }

    override suspend fun finishTournament(tournamentId: Long) {
        tournamentDao.updateTournamentStatus(
            tournamentId = tournamentId,
            status = TournamentStatus.FINISHED.name,
        )
    }

    override suspend fun deleteAllTournaments() {
        tournamentDao.deleteAllTournaments()
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
