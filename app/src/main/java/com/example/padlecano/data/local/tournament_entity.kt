package com.example.padlecano.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tournaments")
data class TournamentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val createdAtMillis: Long,
    val status: String,
    val tournamentType: String,
)
