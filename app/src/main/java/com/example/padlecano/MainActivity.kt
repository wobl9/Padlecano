package com.example.padlecano

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.padlecano.session.SessionBootstrapState
import com.example.padlecano.session.SessionViewModel
import com.example.padlecano.ui.navigation.PadlecanoNavHost
import com.example.padlecano.ui.theme.PadlecanoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PadlecanoTheme {
                val sessionViewModel: SessionViewModel = viewModel()
                val bootstrapState: SessionBootstrapState by sessionViewModel.bootstrapState
                    .collectAsStateWithLifecycle()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        when (val state = bootstrapState) {
                            SessionBootstrapState.Loading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center),
                                )
                            }
                            is SessionBootstrapState.Ready -> {
                                PadlecanoNavHost(
                                    isLoggedIn = state.isLoggedIn,
                                    sessionViewModel = sessionViewModel,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
