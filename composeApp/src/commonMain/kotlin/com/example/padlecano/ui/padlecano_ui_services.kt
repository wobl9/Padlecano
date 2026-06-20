package com.example.padlecano.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.padlecano.domain.repository.SavedPlayerNamesRepository
import com.example.padlecano.domain.repository.TournamentRepository

data class PadlecanoUiServices(
    val tournamentRepository: TournamentRepository,
    val savedPlayerNamesRepository: SavedPlayerNamesRepository,
)

val LocalPadlecanoUiServices = staticCompositionLocalOf<PadlecanoUiServices> {
    error("PadlecanoUiServices was not provided")
}
