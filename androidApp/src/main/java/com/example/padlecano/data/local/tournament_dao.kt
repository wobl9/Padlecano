package com.example.padlecano.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.padlecano.domain.model.EntityId
import kotlinx.coroutines.flow.Flow

@Dao
interface TournamentDao {
    @Query("SELECT * FROM tournaments WHERE deletedAt IS NULL ORDER BY createdAtMillis DESC")
    fun observeTournaments(): Flow<List<TournamentEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTournament(entity: TournamentEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlayers(players: List<TournamentPlayerEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRounds(rounds: List<RoundEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMatches(matches: List<MatchEntity>)

    @Query("SELECT * FROM tournaments WHERE id = :tournamentId AND deletedAt IS NULL LIMIT 1")
    suspend fun getTournamentById(tournamentId: EntityId): TournamentEntity?

    @Query("SELECT * FROM tournament_players WHERE tournamentId = :tournamentId AND deletedAt IS NULL ORDER BY sortOrder ASC")
    suspend fun getPlayersByTournamentId(tournamentId: EntityId): List<TournamentPlayerEntity>

    @Query("SELECT * FROM matches WHERE tournamentId = :tournamentId AND deletedAt IS NULL")
    suspend fun getMatchesByTournamentId(tournamentId: EntityId): List<MatchEntity>

    @Query("SELECT * FROM matches WHERE id = :matchId AND deletedAt IS NULL LIMIT 1")
    suspend fun getMatchById(matchId: EntityId): MatchEntity?

    @Transaction
    @Query("SELECT * FROM rounds WHERE tournamentId = :tournamentId AND deletedAt IS NULL ORDER BY roundNumber ASC")
    fun observeRoundsWithMatches(tournamentId: EntityId): Flow<List<RoundWithMatches>>

    @Transaction
    @Query("SELECT * FROM rounds WHERE tournamentId = :tournamentId AND deletedAt IS NULL ORDER BY roundNumber ASC")
    suspend fun getRoundsWithMatchesOnce(tournamentId: EntityId): List<RoundWithMatches>

    @Query(
        """
        UPDATE matches
        SET scoreA = :scoreA, scoreB = :scoreB, isScoreSet = 1,
            updatedAt = :updatedAt, version = :version
        WHERE id = :matchId
        """,
    )
    suspend fun updateMatchScore(
        matchId: EntityId,
        scoreA: Int,
        scoreB: Int,
        updatedAt: Long,
        version: Int,
    )

    @Query(
        """
        UPDATE tournaments
        SET status = :status, updatedAt = :updatedAt, version = :version
        WHERE id = :tournamentId
        """,
    )
    suspend fun updateTournamentStatus(
        tournamentId: EntityId,
        status: String,
        updatedAt: Long,
        version: Int,
    )

    @Query(
        """
        UPDATE tournaments
        SET deletedAt = :deletedAt, updatedAt = :updatedAt, version = :version
        WHERE id = :tournamentId
        """,
    )
    suspend fun softDeleteTournament(
        tournamentId: EntityId,
        deletedAt: Long,
        updatedAt: Long,
        version: Int,
    )

    @Query("DELETE FROM tournaments")
    suspend fun deleteAllTournaments()

    @Transaction
    suspend fun insertTournamentWithPlayers(
        tournament: TournamentEntity,
        players: List<TournamentPlayerEntity>,
    ): EntityId {
        insertTournament(tournament)
        if (players.isEmpty()) {
            return tournament.id
        }
        val stampedPlayers: List<TournamentPlayerEntity> = players.mapIndexed { index: Int, row: TournamentPlayerEntity ->
            row.copy(tournamentId = tournament.id, sortOrder = index)
        }
        insertPlayers(stampedPlayers)
        return tournament.id
    }
}
