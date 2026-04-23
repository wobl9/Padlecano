package com.example.padlecano.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TournamentDao {
    @Query("SELECT * FROM tournaments ORDER BY createdAtMillis DESC")
    fun observeTournaments(): Flow<List<TournamentEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTournament(entity: TournamentEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlayers(players: List<TournamentPlayerEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRounds(rounds: List<RoundEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMatches(matches: List<MatchEntity>)

    @Query("SELECT * FROM tournaments WHERE id = :tournamentId LIMIT 1")
    suspend fun getTournamentById(tournamentId: Long): TournamentEntity?

    @Query("SELECT * FROM tournament_players WHERE tournamentId = :tournamentId ORDER BY sortOrder ASC")
    suspend fun getPlayersByTournamentId(tournamentId: Long): List<TournamentPlayerEntity>

    @Transaction
    @Query("SELECT * FROM rounds WHERE tournamentId = :tournamentId ORDER BY roundNumber ASC")
    fun observeRoundsWithMatches(tournamentId: Long): Flow<List<RoundWithMatches>>

    @Query("UPDATE matches SET scoreA = :scoreA, scoreB = :scoreB, isScoreSet = 1 WHERE id = :matchId")
    suspend fun updateMatchScore(matchId: Long, scoreA: Int, scoreB: Int)

    @Query("UPDATE tournaments SET status = :status WHERE id = :tournamentId")
    suspend fun updateTournamentStatus(tournamentId: Long, status: String)

    @Query("DELETE FROM tournaments")
    suspend fun deleteAllTournaments()

    @Transaction
    suspend fun insertTournamentWithPlayers(
        tournament: TournamentEntity,
        players: List<TournamentPlayerEntity>,
    ): Long {
        val newTournamentId: Long = insertTournament(tournament)
        if (players.isEmpty()) {
            return newTournamentId
        }
        val stampedPlayers: List<TournamentPlayerEntity> = players.mapIndexed { index: Int, row: TournamentPlayerEntity ->
            row.copy(tournamentId = newTournamentId, sortOrder = index)
        }
        insertPlayers(stampedPlayers)
        return newTournamentId
    }
}
