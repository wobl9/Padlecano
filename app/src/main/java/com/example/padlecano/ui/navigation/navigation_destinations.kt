package com.example.padlecano.ui.navigation

/**
 * Top-level navigation routes. Route arguments are added in later phases.
 */
sealed class NavigationDestination(val route: String) {
    data object Login : NavigationDestination("login")
    data object Games : NavigationDestination("games")
    data object CreateGame : NavigationDestination("create_game")
    data object ActiveGame : NavigationDestination("active_game")
    data object Summary : NavigationDestination("summary")
}
