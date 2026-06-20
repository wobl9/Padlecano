package com.example.padlecano.ui.navigation

import com.example.padlecano.domain.model.EntityId

/**
 * Top-level navigation routes for [androidx.navigation.compose.NavHost].
 */
sealed class NavigationDestination(val route: String) {
    data object Login : NavigationDestination("login")
    data object Games : NavigationDestination("games")
    data object CreateGame : NavigationDestination("create_game")
    data object ActiveGame : NavigationDestination("active_game/{tournamentId}")
    data object Summary : NavigationDestination("summary/{tournamentId}")
    data object MatchValidity : NavigationDestination("match_validity/{tournamentId}")
}

fun activeGameRoute(tournamentId: EntityId): String {
    return "active_game/$tournamentId"
}

fun summaryRoute(tournamentId: EntityId): String {
    return "summary/$tournamentId"
}

fun matchValidityRoute(tournamentId: EntityId): String {
    return "match_validity/$tournamentId"
}
