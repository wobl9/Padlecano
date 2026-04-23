package com.example.padlecano.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.padlecano.session.SessionViewModel
import com.example.padlecano.ui.games.GamesListScreen
import com.example.padlecano.ui.login.LoginScreen
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
                GamesListScreen()
            }
        }
    }
}
