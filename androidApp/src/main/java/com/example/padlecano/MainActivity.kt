package com.example.padlecano

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.padlecano.composeapp.PadlecanoApp
import com.example.padlecano.composeapp.SessionBootstrapState as ComposeSessionBootstrapState
import com.example.padlecano.session.SessionBootstrapState
import com.example.padlecano.session.SessionViewModel
import com.example.padlecano.ui.PadlecanoUiServices

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val application: PadlecanoApplication = application as PadlecanoApplication
        val services: PadlecanoUiServices = PadlecanoUiServices(
            tournamentRepository = application.tournamentRepository,
            savedPlayerNamesRepository = application.savedPlayerNamesRepository,
        )
        setContent {
            val sessionViewModel: SessionViewModel = viewModel()
            val bootstrapState: SessionBootstrapState = sessionViewModel.bootstrapState
                .collectAsStateWithLifecycle().value
            val composeBootstrapState: ComposeSessionBootstrapState = when (bootstrapState) {
                SessionBootstrapState.Loading -> ComposeSessionBootstrapState.Loading
                is SessionBootstrapState.Ready -> ComposeSessionBootstrapState.Ready(
                    isLoggedIn = bootstrapState.isLoggedIn,
                )
            }
            PadlecanoApp(
                services = services,
                bootstrapState = composeBootstrapState,
                onAuthorizeClick = { sessionViewModel.persistLoggedIn() },
                modifier = Modifier,
            )
        }
    }
}
