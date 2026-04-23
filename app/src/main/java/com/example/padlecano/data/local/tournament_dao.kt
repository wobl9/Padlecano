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
    @Query("SELECT * FROM tournaments WHERE id = :tournamentId LIMIT 1")
    suspend fun getTournamentById(tournamentId: Long): TournamentEntity?
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
