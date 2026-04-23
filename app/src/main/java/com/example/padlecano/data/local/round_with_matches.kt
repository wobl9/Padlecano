package com.example.padlecano.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class RoundWithMatches(
    @Embedded val round: RoundEntity,
    @Relation(parentColumn = "id", entityColumn = "roundId")
    val matches: List<MatchEntity>,
)
