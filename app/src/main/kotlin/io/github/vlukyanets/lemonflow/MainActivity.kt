package io.github.vlukyanets.lemonflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import io.github.vlukyanets.lemonflow.ui.main.MainScreen
import io.github.vlukyanets.lemonflow.ui.theme.LemonFlowTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LemonFlowTheme {
                MainScreen()
            }
        }
    }
}
