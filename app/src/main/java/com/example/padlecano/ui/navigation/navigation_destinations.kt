package com.example.padlecano.ui.navigation

/**
 * Top-level navigation routes for [androidx.navigation.compose.NavHost].
 */
sealed class NavigationDestination(val route: String) {
    data object Login : NavigationDestination("login")
    data object Games : NavigationDestination("games")
    data object CreateGame : NavigationDestination("create_game")
    data object ActiveGame : NavigationDestination("active_game/{tournamentId}")
    data object Summary : NavigationDestination("summary/{tournamentId}")
    data object ScheduleVerification : NavigationDestination("schedule_verification/{tournamentId}")
}

fun activeGameRoute(tournamentId: Long): String {
    return "active_game/$tournamentId"
}

fun summaryRoute(tournamentId: Long): String {
    return "summary/$tournamentId"
}

fun scheduleVerificationRoute(tournamentId: Long): String {
    return "schedule_verification/$tournamentId"
}
