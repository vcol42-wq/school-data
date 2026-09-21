package com.theprincipal.keygen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.theprincipal.keygen.ui.KeyGenApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val luxuryDarkColors = darkColorScheme(
                primary = Color(0xFFF59E0B),
                secondary = Color(0xFF10B981),
                background = Color(0xFF0B0F19),
                surface = Color(0xFF111827)
            )

            MaterialTheme(colorScheme = luxuryDarkColors) {
                KeyGenApp()
            }
        }
    }
}
