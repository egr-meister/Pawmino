package com.pawmino.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.pawmino.app.ui.PawminoApp
import com.pawmino.app.ui.theme.PawminoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Edge-to-edge with visible system bars; Compose handles safe insets via Scaffold.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PawminoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PawminoApp()
                }
            }
        }
    }
}
