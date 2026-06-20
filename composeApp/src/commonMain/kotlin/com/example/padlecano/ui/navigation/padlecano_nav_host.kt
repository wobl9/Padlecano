package com.example.padlecano.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
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
import com.example.padlecano.domain.model.EntityId
import com.example.padlecano.domain.model.TournamentStatus
import com.example.padlecano.ui.LocalPadlecanoUiServices
import com.example.padlecano.ui.PadlecanoUiServices
import com.example.padlecano.ui.active.ActiveGameScreen
import com.example.padlecano.ui.active.ActiveGameViewModel
import com.example.padlecano.ui.active.activeGameViewModelFactory
import com.example.padlecano.ui.create.CreateGameScreen
import com.example.padlecano.ui.create.CreateGameViewModel
import com.example.padlecano.ui.create.createGameViewModelFactory
import com.example.padlecano.ui.games.GamesListScreen
import com.example.padlecano.ui.games.GamesListViewModel
import com.example.padlecano.ui.games.gamesListViewModelFactory
import com.example.padlecano.ui.login.LoginScreen
import com.example.padlecano.ui.match_validity.MatchValidityScreen
import com.example.padlecano.ui.match_validity.MatchValidityViewModel
import com.example.padlecano.ui.match_validity.matchValidityViewModelFactory
import com.example.padlecano.ui.summary.SummaryScreen
import com.example.padlecano.ui.summary.SummaryViewModel
import com.example.padlecano.ui.summary.summaryViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun PadlecanoNavHost(
    services: PadlecanoUiServices,
    isLoggedIn: Boolean,
    onAuthorizeClick: suspend () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalPadlecanoUiServices provides services) {
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
                                onAuthorizeClick()
                            }
                        },
                    )
                }
                composable(route = NavigationDestination.Games.route) {
                    val gamesListViewModel: GamesListViewModel = viewModel(
                        factory = remember(services.tournamentRepository) {
                            gamesListViewModelFactory(tournamentRepository = services.tournamentRepository)
                        },
                    )
                    val gamesUiState by gamesListViewModel.uiState.collectAsStateWithLifecycle()
                    GamesListScreen(
                        uiState = gamesUiState,
                        onCreateGameClick = {
                            navController.navigate(route = NavigationDestination.CreateGame.route)
                        },
                        onDeleteAllGamesClick = {
                            gamesListViewModel.deleteAllTournaments()
                        },
                        onDeleteTournamentClick = { tournament ->
                            gamesListViewModel.deleteTournament(tournamentId = tournament.id)
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
                    val createGameViewModel: CreateGameViewModel = viewModel(
                        factory = remember(services) {
                            createGameViewModelFactory(
                                tournamentRepository = services.tournamentRepository,
                                savedPlayerNamesRepository = services.savedPlayerNamesRepository,
                            )
                        },
                    )
                    CreateGameScreen(
                        viewModel = createGameViewModel,
                        onNavigateUp = { navController.navigateUp() },
                        onTournamentCreated = { tournamentId: EntityId ->
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
                        navArgument(name = "tournamentId") { type = NavType.StringType },
                    ),
                ) { backStackEntry ->
                    val tournamentId: EntityId = checkNotNull(backStackEntry.arguments?.getString("tournamentId"))
                    val activeGameViewModel: ActiveGameViewModel = viewModel(
                        key = tournamentId,
                        factory = remember(tournamentId, services.tournamentRepository) {
                            activeGameViewModelFactory(
                                tournamentId = tournamentId,
                                tournamentRepository = services.tournamentRepository,
                            )
                        },
                    )
                    ActiveGameScreen(
                        viewModel = activeGameViewModel,
                        onNavigateUp = { navController.navigateUp() },
                        onNavigateToSummary = { id: EntityId ->
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
                        navArgument(name = "tournamentId") { type = NavType.StringType },
                    ),
                ) { backStackEntry ->
                    val tournamentId: EntityId = checkNotNull(backStackEntry.arguments?.getString("tournamentId"))
                    val summaryViewModel: SummaryViewModel = viewModel(
                        key = tournamentId,
                        factory = remember(tournamentId, services.tournamentRepository) {
                            summaryViewModelFactory(
                                tournamentId = tournamentId,
                                tournamentRepository = services.tournamentRepository,
                            )
                        },
                    )
                    SummaryScreen(
                        viewModel = summaryViewModel,
                        onNavigateUp = { navController.navigateUp() },
                        onNavigateToMatchValidity = {
                            navController.navigate(route = matchValidityRoute(tournamentId = tournamentId))
                        },
                    )
                }
                composable(
                    route = NavigationDestination.MatchValidity.route,
                    arguments = listOf(
                        navArgument(name = "tournamentId") { type = NavType.StringType },
                    ),
                ) { backStackEntry ->
                    val tournamentId: EntityId = checkNotNull(backStackEntry.arguments?.getString("tournamentId"))
                    val matchValidityViewModel: MatchValidityViewModel = viewModel(
                        key = tournamentId,
                        factory = remember(tournamentId, services.tournamentRepository) {
                            matchValidityViewModelFactory(
                                tournamentId = tournamentId,
                                tournamentRepository = services.tournamentRepository,
                            )
                        },
                    )
                    MatchValidityScreen(
                        viewModel = matchValidityViewModel,
                        onNavigateUp = { navController.navigateUp() },
                    )
                }
            }
        }
    }
}
