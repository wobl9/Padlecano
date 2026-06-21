package com.example.padlecano.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.padlecano.data.local.toEntityFields
import com.example.padlecano.data.local.syncMetadata
import com.example.padlecano.database.Matches
import com.example.padlecano.database.PadlecanoDatabase
import com.example.padlecano.database.Rounds
import com.example.padlecano.database.Tournaments
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
import com.example.padlecano.domain.repository.TournamentRepository
import com.example.padlecano.domain.usecase.AmericanoScheduleGenerator
import com.example.padlecano.domain.usecase.MatchValidityAuditBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class SqlDelightTournamentRepository(
    private val database: PadlecanoDatabase,
) : TournamentRepository {
    override fun observeTournamentSummaries(): Flow<List<TournamentSummary>> {
        return database.tournamentsQueries.selectAllActive()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows: List<Tournaments> ->
                rows.map { row: Tournaments -> row.toSummary() }
            }
    }
    override suspend fun createAmericanoTournament(
        title: String,
        playerDisplayNames: List<String>,
        maxCombinedMatchScore: Int,
    ): EntityId {
        val nowMillis: Long = SyncMetadata.currentTimeMillis()
        val syncMetadata: SyncMetadata = SyncMetadata.createNew(nowMillis = nowMillis)
        val syncFields = syncMetadata.toEntityFields()
        val tournamentId: EntityId = newEntityId()
        database.transaction {
            database.tournamentsQueries.insertTournament(
                id = tournamentId,
                title = title,
                createdAtMillis = nowMillis,
                status = TournamentStatus.ACTIVE.name,
                tournamentType = TournamentType.AMERICANO.name,
                maxCombinedMatchScore = maxCombinedMatchScore.toLong(),
                ownerId = syncFields.ownerId,
                updatedAt = syncFields.updatedAt,
                version = syncFields.version.toLong(),
                deletedAt = syncFields.deletedAt,
            )
            playerDisplayNames.forEachIndexed { index: Int, name: String ->
                database.tournament_playersQueries.insertPlayer(
                    id = newEntityId(),
                    tournamentId = tournamentId,
                    displayName = name,
                    sortOrder = index.toLong(),
                    ownerId = syncFields.ownerId,
                    updatedAt = syncFields.updatedAt,
                    version = syncFields.version.toLong(),
                    deletedAt = syncFields.deletedAt,
                )
            }
        }
        val schedule: List<List<AmericanoScheduleGenerator.MatchSetup>> =
            AmericanoScheduleGenerator.generate(playerDisplayNames.size)
        val roundEntities: List<RoundInsertRow> = schedule.mapIndexed { index: Int, _ ->
            RoundInsertRow(
                id = newEntityId(),
                roundNumber = index + 1,
            )
        }
        database.transaction {
            roundEntities.forEach { round: RoundInsertRow ->
                database.roundsQueries.insertRound(
                    id = round.id,
                    tournamentId = tournamentId,
                    roundNumber = round.roundNumber.toLong(),
                    ownerId = syncFields.ownerId,
                    updatedAt = syncFields.updatedAt,
                    version = syncFields.version.toLong(),
                    deletedAt = syncFields.deletedAt,
                )
            }
            val roundIdsByNumber: Map<Int, EntityId> = roundEntities.associate { row ->
                row.roundNumber to row.id
            }
            schedule.forEachIndexed { roundIndex: Int, setups: List<AmericanoScheduleGenerator.MatchSetup> ->
                val roundId: EntityId = roundIdsByNumber.getValue(roundIndex + 1)
                setups.forEach { setup: AmericanoScheduleGenerator.MatchSetup ->
                    database.matchesQueries.insertMatch(
                        id = newEntityId(),
                        tournamentId = tournamentId,
                        roundId = roundId,
                        playerA1Index = setup.playerA1.toLong(),
                        playerA2Index = setup.playerA2.toLong(),
                        playerB1Index = setup.playerB1.toLong(),
                        playerB2Index = setup.playerB2.toLong(),
                        scoreA = 0,
                        scoreB = 0,
                        isScoreSet = 0,
                        ownerId = syncFields.ownerId,
                        updatedAt = syncFields.updatedAt,
                        version = syncFields.version.toLong(),
                        deletedAt = syncFields.deletedAt,
                    )
                }
            }
        }
        return tournamentId
    }
    override fun observeActiveTournament(tournamentId: EntityId): Flow<ActiveTournamentState?> = flow {
        val tournamentRow: Tournaments = database.tournamentsQueries.selectById(tournamentId).executeAsOneOrNull()
            ?: run {
                emit(null)
                return@flow
            }
        val players: List<String> = database.tournament_playersQueries.selectByTournamentId(tournamentId)
            .executeAsList()
            .map { row -> row.displayName }
        val roundsFlow: Flow<List<Rounds>> = database.roundsQueries.observeByTournamentId(tournamentId)
            .asFlow()
            .mapToList(Dispatchers.Default)
        val matchesFlow: Flow<List<Matches>> = database.matchesQueries.observeByTournamentId(tournamentId)
            .asFlow()
            .mapToList(Dispatchers.Default)
        combine(roundsFlow, matchesFlow) { rounds: List<Rounds>, matches: List<Matches> ->
            ActiveTournamentState(
                tournamentId = tournamentRow.id,
                title = tournamentRow.title,
                players = players,
                maxCombinedMatchScore = tournamentRow.maxCombinedMatchScore.toInt(),
                rounds = rounds.map { round: Rounds ->
                    RoundState(
                        roundId = round.id,
                        roundNumber = round.roundNumber.toInt(),
                        matches = matches
                            .filter { match: Matches -> match.roundId == round.id }
                            .sortedBy { match: Matches -> match.id }
                            .map { match: Matches -> match.toMatchState() },
                    )
                },
            )
        }.collect { state: ActiveTournamentState ->
            emit(state)
        }
    }
    override suspend fun saveMatchScores(scores: List<MatchScoreUpdate>) {
        if (scores.isEmpty()) {
            return
        }
        val nowMillis: Long = SyncMetadata.currentTimeMillis()
        val firstMatch: Matches = requireNotNull(
            database.matchesQueries.selectById(scores.first().matchId).executeAsOneOrNull(),
        )
        val tournament: Tournaments = requireNotNull(
            database.tournamentsQueries.selectById(firstMatch.tournamentId).executeAsOneOrNull(),
        )
        database.transaction {
            scores.forEach { update: MatchScoreUpdate ->
                val match: Matches = requireNotNull(
                    database.matchesQueries.selectById(update.matchId).executeAsOneOrNull(),
                )
                val bumpedMatchSync: SyncMetadata = SyncMetadata.bump(
                    previous = match.syncMetadata(),
                    nowMillis = nowMillis,
                )
                database.matchesQueries.updateMatchScore(
                    scoreA = update.scoreA.toLong(),
                    scoreB = update.scoreB.toLong(),
                    updatedAt = bumpedMatchSync.updatedAt,
                    version = bumpedMatchSync.version.toLong(),
                    id = update.matchId,
                )
            }
            val bumpedTournamentSync: SyncMetadata = SyncMetadata.bump(
                previous = tournament.syncMetadata(),
                nowMillis = nowMillis,
            )
            database.tournamentsQueries.updateTournamentStatus(
                status = tournament.status,
                updatedAt = bumpedTournamentSync.updatedAt,
                version = bumpedTournamentSync.version.toLong(),
                id = tournament.id,
            )
        }
    }
    override suspend fun finishTournament(tournamentId: EntityId) {
        val tournament: Tournaments = requireNotNull(
            database.tournamentsQueries.selectById(tournamentId).executeAsOneOrNull(),
        )
        val bumpedSync: SyncMetadata = SyncMetadata.bump(previous = tournament.syncMetadata())
        database.tournamentsQueries.updateTournamentStatus(
            status = TournamentStatus.FINISHED.name,
            updatedAt = bumpedSync.updatedAt,
            version = bumpedSync.version.toLong(),
            id = tournamentId,
        )
    }
    override suspend fun deleteTournament(tournamentId: EntityId) {
        val tournament: Tournaments = requireNotNull(
            database.tournamentsQueries.selectById(tournamentId).executeAsOneOrNull(),
        )
        val deletedSync: SyncMetadata = SyncMetadata.markDeleted(previous = tournament.syncMetadata())
        database.tournamentsQueries.softDeleteTournament(
            deletedAt = requireNotNull(deletedSync.deletedAt),
            updatedAt = deletedSync.updatedAt,
            version = deletedSync.version.toLong(),
            id = tournamentId,
        )
    }
    override suspend fun deleteAllTournaments() {
        database.tournamentsQueries.deleteAllTournaments()
    }
    override suspend fun loadTournamentResultsPayload(tournamentId: EntityId): TournamentResultsPayload? {
        val row: Tournaments = database.tournamentsQueries.selectById(tournamentId).executeAsOneOrNull()
            ?: return null
        val playerNames: List<String> = database.tournament_playersQueries.selectByTournamentId(tournamentId)
            .executeAsList()
            .map { player -> player.displayName }
        val records: List<TournamentMatchRecord> = database.matchesQueries.selectByTournamentId(tournamentId)
            .executeAsList()
            .map { match: Matches -> match.toMatchRecord() }
        val rounds: List<TournamentRoundResults> = buildRoundResults(tournamentId)
        return TournamentResultsPayload(
            tournamentTitle = row.title,
            tournamentType = row.tournamentType.toTournamentType(),
            playerDisplayNames = playerNames,
            matches = records,
            rounds = rounds,
        )
    }
    override suspend fun loadMatchValidityAudit(tournamentId: EntityId): MatchValidityAudit? {
        val row: Tournaments = database.tournamentsQueries.selectById(tournamentId).executeAsOneOrNull()
            ?: return null
        val playerNames: List<String> = database.tournament_playersQueries.selectByTournamentId(tournamentId)
            .executeAsList()
            .map { player -> player.displayName }
        val rounds: List<Rounds> = database.roundsQueries.selectByTournamentId(tournamentId).executeAsList()
        val matches: List<Matches> = database.matchesQueries.selectByTournamentId(tournamentId).executeAsList()
        val roundsData: List<RawRoundForAudit> = rounds.map { round: Rounds ->
            RawRoundForAudit(
                roundNumber = round.roundNumber.toInt(),
                matches = matches
                    .filter { match: Matches -> match.roundId == round.id }
                    .sortedBy { match: Matches -> match.id }
                    .map { match: Matches ->
                        RawMatchForAudit(
                            matchId = match.id,
                            playerA1Index = match.playerA1Index.toInt(),
                            playerA2Index = match.playerA2Index.toInt(),
                            playerB1Index = match.playerB1Index.toInt(),
                            playerB2Index = match.playerB2Index.toInt(),
                            scoreA = match.scoreA.toInt(),
                            scoreB = match.scoreB.toInt(),
                            isScoreSet = match.isScoreSet != 0L,
                        )
                    },
            )
        }
        return MatchValidityAuditBuilder.build(
            tournamentTitle = row.title,
            maxCombinedMatchScore = row.maxCombinedMatchScore.toInt(),
            playerDisplayNames = playerNames,
            rounds = roundsData,
        )
    }
    private suspend fun buildRoundResults(tournamentId: EntityId): List<TournamentRoundResults> {
        val rounds: List<Rounds> = database.roundsQueries.selectByTournamentId(tournamentId).executeAsList()
        val matches: List<Matches> = database.matchesQueries.selectByTournamentId(tournamentId).executeAsList()
        return rounds.map { round: Rounds ->
            TournamentRoundResults(
                roundNumber = round.roundNumber.toInt(),
                matches = matches
                    .filter { match: Matches -> match.roundId == round.id }
                    .sortedBy { match: Matches -> match.id }
                    .map { match: Matches -> match.toMatchRecord() },
            )
        }
    }
}

private data class RoundInsertRow(
    val id: EntityId,
    val roundNumber: Int,
)

private fun Tournaments.toSummary(): TournamentSummary {
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

private fun Matches.toMatchState(): MatchState {
    return MatchState(
        matchId = id,
        playerA1Index = playerA1Index.toInt(),
        playerA2Index = playerA2Index.toInt(),
        playerB1Index = playerB1Index.toInt(),
        playerB2Index = playerB2Index.toInt(),
        scoreA = scoreA.toInt(),
        scoreB = scoreB.toInt(),
        isScoreSet = isScoreSet != 0L,
    )
}

private fun Matches.toMatchRecord(): TournamentMatchRecord {
    return TournamentMatchRecord(
        playerA1Index = playerA1Index.toInt(),
        playerA2Index = playerA2Index.toInt(),
        playerB1Index = playerB1Index.toInt(),
        playerB2Index = playerB2Index.toInt(),
        scoreA = scoreA.toInt(),
        scoreB = scoreB.toInt(),
        isScoreSet = isScoreSet != 0L,
    )
}
