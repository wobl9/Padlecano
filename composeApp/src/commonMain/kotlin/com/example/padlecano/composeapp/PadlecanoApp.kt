package com.example.padlecano.composeapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.padlecano.ui.PadlecanoUiServices
import com.example.padlecano.ui.navigation.PadlecanoNavHost
import com.example.padlecano.ui.theme.PadlecanoTheme

sealed interface SessionBootstrapState {
    data object Loading : SessionBootstrapState
    data class Ready(val isLoggedIn: Boolean) : SessionBootstrapState
}

@Composable
fun PadlecanoApp(
    services: PadlecanoUiServices,
    bootstrapState: SessionBootstrapState,
    onAuthorizeClick: suspend () -> Unit,
    modifier: Modifier = Modifier,
) {
    PadlecanoTheme {
        Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                when (bootstrapState) {
                    SessionBootstrapState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                    is SessionBootstrapState.Ready -> {
                        PadlecanoNavHost(
                            services = services,
                            isLoggedIn = bootstrapState.isLoggedIn,
                            onAuthorizeClick = onAuthorizeClick,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
