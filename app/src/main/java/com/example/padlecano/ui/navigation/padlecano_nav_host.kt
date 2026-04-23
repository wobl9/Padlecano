package com.example.padlecano.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.padlecano.domain.model.TournamentStatus
import com.example.padlecano.session.SessionViewModel
import com.example.padlecano.ui.active.ActiveGameScreen
import com.example.padlecano.ui.active.ActiveGameViewModel
import com.example.padlecano.ui.create.CreateGameScreen
import com.example.padlecano.ui.create.CreateGameViewModel
import com.example.padlecano.ui.games.GamesListScreen
import com.example.padlecano.ui.games.GamesListViewModel
import com.example.padlecano.ui.login.LoginScreen
import com.example.padlecano.ui.summary.SummaryPlaceholderScreen
import kotlinx.coroutines.launch

@Composable
fun PadlecanoNavHost(
    isLoggedIn: Boolean,
    sessionViewModel: SessionViewModel,
    modifier: Modifier = Modifier,
) {
    key(isLoggedIn) {
        val navController: NavHostController = rememberNavController()
        val coroutineScope = rememberCoroutineScope()
        val startRoute: String = if (isLoggedIn) {
            NavigationDestination.Games.route
        } else {
            NavigationDestination.Login.route
        }
        NavHost(
            modifier = modifier,
            navController = navController,
            startDestination = startRoute,
        ) {
            composable(route = NavigationDestination.Login.route) {
                LoginScreen(
                    onAuthorizeClick = {
                        coroutineScope.launch {
                            sessionViewModel.persistLoggedIn()
                        }
                    },
                )
            }
            composable(route = NavigationDestination.Games.route) {
                val gamesListViewModel: GamesListViewModel = viewModel(factory = GamesListViewModel.createFactory())
                val gamesUiState by gamesListViewModel.uiState.collectAsStateWithLifecycle()
                GamesListScreen(
                    uiState = gamesUiState,
                    onCreateGameClick = {
                        navController.navigate(route = NavigationDestination.CreateGame.route)
                    },
                    onTournamentClick = { tournament ->
                        when (tournament.status) {
                            TournamentStatus.DRAFT -> {
                                navController.navigate(route = NavigationDestination.CreateGame.route)
                            }
                            TournamentStatus.ACTIVE -> {
                                navController.navigate(route = activeGameRoute(tournamentId = tournament.id))
                            }
                            TournamentStatus.FINISHED -> {
                                navController.navigate(route = summaryRoute(tournamentId = tournament.id))
                            }
                        }
                    },
                )
            }
            composable(route = NavigationDestination.CreateGame.route) {
                val createGameViewModel: CreateGameViewModel = viewModel(factory = CreateGameViewModel.createFactory())
                CreateGameScreen(
                    viewModel = createGameViewModel,
                    onNavigateUp = { navController.navigateUp() },
                    onTournamentCreated = { tournamentId: Long ->
                        navController.navigate(route = activeGameRoute(tournamentId = tournamentId)) {
                            popUpTo(route = NavigationDestination.CreateGame.route) {
                                inclusive = true
                            }
                        }
                    },
                )
            }
            composable(
                route = NavigationDestination.ActiveGame.route,
                arguments = listOf(
                    navArgument(name = "tournamentId") { type = NavType.LongType },
                ),
            ) { backStackEntry ->
                val tournamentId: Long = checkNotNull(backStackEntry.arguments).getLong("tournamentId")
                val activeGameViewModel: ActiveGameViewModel = viewModel(
                    factory = ActiveGameViewModel.createFactory(tournamentId = tournamentId),
                )
                ActiveGameScreen(
                    viewModel = activeGameViewModel,
                    onNavigateUp = { navController.navigateUp() },
                    onNavigateToSummary = { id: Long ->
                        navController.navigate(route = summaryRoute(tournamentId = id)) {
                            popUpTo(route = activeGameRoute(tournamentId = tournamentId)) {
                                inclusive = true
                            }
                        }
                    },
                )
            }
            composable(
                route = NavigationDestination.Summary.route,
                arguments = listOf(
                    navArgument(name = "tournamentId") { type = NavType.LongType },
                ),
            ) { backStackEntry ->
                val tournamentId: Long = checkNotNull(backStackEntry.arguments).getLong("tournamentId")
                SummaryPlaceholderScreen(
                    tournamentId = tournamentId,
                    onNavigateUp = { navController.navigateUp() },
                )
            }
        }
    }
}
