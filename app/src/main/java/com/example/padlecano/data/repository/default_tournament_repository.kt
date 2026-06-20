package com.example.padlecano.data.repository

import com.example.padlecano.data.local.MatchEntity
import com.example.padlecano.data.local.RoundEntity
import com.example.padlecano.data.local.RoundWithMatches
import com.example.padlecano.data.local.TournamentDao
import com.example.padlecano.data.local.TournamentEntity
import com.example.padlecano.data.local.TournamentPlayerEntity
import com.example.padlecano.data.local.syncMetadata
import com.example.padlecano.data.local.toEntityFields
import com.example.padlecano.domain.model.ActiveTournamentState
import com.example.padlecano.domain.model.EntityId
import com.example.padlecano.domain.model.MatchScoreUpdate
import com.example.padlecano.domain.model.MatchState
import com.example.padlecano.domain.model.MatchValidityAudit
import com.example.padlecano.domain.model.RawMatchForAudit
import com.example.padlecano.domain.model.RawRoundForAudit
import com.example.padlecano.domain.model.RoundState
import com.example.padlecano.domain.model.SyncMetadata
import com.example.padlecano.domain.model.TournamentMatchRecord
import com.example.padlecano.domain.model.TournamentResultsPayload
import com.example.padlecano.domain.model.TournamentRoundResults
import com.example.padlecano.domain.model.TournamentStatus
import com.example.padlecano.domain.model.TournamentSummary
import com.example.padlecano.domain.model.TournamentType
import com.example.padlecano.domain.model.newEntityId
import com.example.padlecano.domain.usecase.AmericanoScheduleGenerator
import com.example.padlecano.domain.usecase.MatchValidityAuditBuilder
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
    ): EntityId {
        val nowMillis: Long = System.currentTimeMillis()
        val syncMetadata: SyncMetadata = SyncMetadata.createNew(nowMillis = nowMillis)
        val syncFields = syncMetadata.toEntityFields()
        val tournamentId: EntityId = newEntityId()
        val tournament = TournamentEntity(
            id = tournamentId,
            title = title,
            createdAtMillis = nowMillis,
            status = TournamentStatus.ACTIVE.name,
            tournamentType = TournamentType.AMERICANO.name,
            maxCombinedMatchScore = maxCombinedMatchScore,
            ownerId = syncFields.ownerId,
            updatedAt = syncFields.updatedAt,
            version = syncFields.version,
            deletedAt = syncFields.deletedAt,
        )
        val players: List<TournamentPlayerEntity> = playerDisplayNames.mapIndexed { index: Int, name: String ->
            TournamentPlayerEntity(
                id = newEntityId(),
                tournamentId = tournamentId,
                displayName = name,
                sortOrder = index,
                ownerId = syncFields.ownerId,
                updatedAt = syncFields.updatedAt,
                version = syncFields.version,
                deletedAt = syncFields.deletedAt,
            )
        }
        tournamentDao.insertTournamentWithPlayers(tournament, players)
        val schedule: List<List<AmericanoScheduleGenerator.MatchSetup>> =
            AmericanoScheduleGenerator.generate(playerDisplayNames.size)
        val roundEntities: List<RoundEntity> = schedule.mapIndexed { index: Int, _ ->
            RoundEntity(
                id = newEntityId(),
                tournamentId = tournamentId,
                roundNumber = index + 1,
                ownerId = syncFields.ownerId,
                updatedAt = syncFields.updatedAt,
                version = syncFields.version,
                deletedAt = syncFields.deletedAt,
            )
        }
        tournamentDao.insertRounds(roundEntities)
        val roundIdsByNumber: Map<Int, EntityId> = roundEntities.associate { round ->
            round.roundNumber to round.id
        }
        val allMatches: List<MatchEntity> = schedule.flatMapIndexed { roundIndex: Int, setups: List<AmericanoScheduleGenerator.MatchSetup> ->
            val roundId: EntityId = roundIdsByNumber.getValue(roundIndex + 1)
            setups.map { setup: AmericanoScheduleGenerator.MatchSetup ->
                MatchEntity(
                    id = newEntityId(),
                    tournamentId = tournamentId,
                    roundId = roundId,
                    playerA1Index = setup.playerA1,
                    playerA2Index = setup.playerA2,
                    playerB1Index = setup.playerB1,
                    playerB2Index = setup.playerB2,
                    ownerId = syncFields.ownerId,
                    updatedAt = syncFields.updatedAt,
                    version = syncFields.version,
                    deletedAt = syncFields.deletedAt,
                )
            }
        }
        if (allMatches.isNotEmpty()) {
            tournamentDao.insertMatches(allMatches)
        }
        return tournamentId
    }

    override fun observeActiveTournament(tournamentId: EntityId): Flow<ActiveTournamentState?> = flow {
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
        if (scores.isEmpty()) {
            return
        }
        val nowMillis: Long = System.currentTimeMillis()
        val firstMatch: MatchEntity = requireNotNull(tournamentDao.getMatchById(scores.first().matchId))
        val tournament: TournamentEntity = requireNotNull(
            tournamentDao.getTournamentById(firstMatch.tournamentId),
        )
        scores.forEach { update: MatchScoreUpdate ->
            val match: MatchEntity = requireNotNull(tournamentDao.getMatchById(update.matchId))
            val bumpedMatchSync: SyncMetadata = SyncMetadata.bump(
                previous = match.syncMetadata(),
                nowMillis = nowMillis,
            )
            tournamentDao.updateMatchScore(
                matchId = update.matchId,
                scoreA = update.scoreA,
                scoreB = update.scoreB,
                updatedAt = bumpedMatchSync.updatedAt,
                version = bumpedMatchSync.version,
            )
        }
        val bumpedTournamentSync: SyncMetadata = SyncMetadata.bump(
            previous = tournament.syncMetadata(),
            nowMillis = nowMillis,
        )
        tournamentDao.updateTournamentStatus(
            tournamentId = tournament.id,
            status = tournament.status,
            updatedAt = bumpedTournamentSync.updatedAt,
            version = bumpedTournamentSync.version,
        )
    }

    override suspend fun finishTournament(tournamentId: EntityId) {
        val tournament: TournamentEntity = requireNotNull(tournamentDao.getTournamentById(tournamentId))
        val bumpedSync: SyncMetadata = SyncMetadata.bump(previous = tournament.syncMetadata())
        tournamentDao.updateTournamentStatus(
            tournamentId = tournamentId,
            status = TournamentStatus.FINISHED.name,
            updatedAt = bumpedSync.updatedAt,
            version = bumpedSync.version,
        )
    }

    override suspend fun deleteTournament(tournamentId: EntityId) {
        val tournament: TournamentEntity = requireNotNull(tournamentDao.getTournamentById(tournamentId))
        val deletedSync: SyncMetadata = SyncMetadata.markDeleted(previous = tournament.syncMetadata())
        tournamentDao.softDeleteTournament(
            tournamentId = tournamentId,
            deletedAt = requireNotNull(deletedSync.deletedAt),
            updatedAt = deletedSync.updatedAt,
            version = deletedSync.version,
        )
    }

    override suspend fun deleteAllTournaments() {
        tournamentDao.deleteAllTournaments()
    }

    override suspend fun loadTournamentResultsPayload(tournamentId: EntityId): TournamentResultsPayload? {
        val entity: TournamentEntity = tournamentDao.getTournamentById(tournamentId) ?: return null
        val playerNames: List<String> = tournamentDao.getPlayersByTournamentId(tournamentId)
            .sortedBy { it.sortOrder }
            .map { it.displayName }
        val records: List<TournamentMatchRecord> = tournamentDao.getMatchesByTournamentId(tournamentId)
            .map { match: MatchEntity ->
                match.toMatchRecord()
            }
        val rounds: List<TournamentRoundResults> = tournamentDao.getRoundsWithMatchesOnce(tournamentId)
            .map { rwm: RoundWithMatches ->
                TournamentRoundResults(
                    roundNumber = rwm.round.roundNumber,
                    matches = rwm.matches.sortedBy { it.id }.map { match: MatchEntity ->
                        match.toMatchRecord()
                    },
                )
            }
        return TournamentResultsPayload(
            tournamentTitle = entity.title,
            tournamentType = entity.tournamentType.toTournamentType(),
            playerDisplayNames = playerNames,
            matches = records,
            rounds = rounds,
        )
    }

    override suspend fun loadMatchValidityAudit(tournamentId: EntityId): MatchValidityAudit? {
        val entity: TournamentEntity = tournamentDao.getTournamentById(tournamentId) ?: return null
        val playerNames: List<String> = tournamentDao.getPlayersByTournamentId(tournamentId)
            .sortedBy { it.sortOrder }
            .map { it.displayName }
        val roundsData: List<RawRoundForAudit> = tournamentDao.getRoundsWithMatchesOnce(tournamentId)
            .map { rwm: RoundWithMatches ->
                RawRoundForAudit(
                    roundNumber = rwm.round.roundNumber,
                    matches = rwm.matches.sortedBy { it.id }.map { match: MatchEntity ->
                        RawMatchForAudit(
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
            }
        return MatchValidityAuditBuilder.build(
            tournamentTitle = entity.title,
            maxCombinedMatchScore = entity.maxCombinedMatchScore,
            playerDisplayNames = playerNames,
            rounds = roundsData,
        )
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

private fun MatchEntity.toMatchRecord(): TournamentMatchRecord {
    return TournamentMatchRecord(
        playerA1Index = playerA1Index,
        playerA2Index = playerA2Index,
        playerB1Index = playerB1Index,
        playerB2Index = playerB2Index,
        scoreA = scoreA,
        scoreB = scoreB,
        isScoreSet = isScoreSet,
    )
}
