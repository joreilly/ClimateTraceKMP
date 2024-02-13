package dev.johnoreilly.climatetrace

import App
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }
}