package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DroidDeckScreen
import com.example.ui.theme.DroidDeckTheme
import com.example.ui.viewmodel.DroidDeckViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            val deckViewModel: DroidDeckViewModel = viewModel(
                factory = DroidDeckViewModel.factory(application)
            )
            val uiState by deckViewModel.uiState.collectAsStateWithLifecycle()

            DroidDeckTheme(accentTheme = uiState.accentTheme) {
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    deckViewModel.onResumeRefresh()
                }

                DroidDeckScreen(
                    uiState = uiState,
                    viewModel = deckViewModel
                )
            }
        }
    }
}
