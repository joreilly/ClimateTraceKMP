@file:OptIn(ExperimentalMaterial3Api::class)

package dev.johnoreilly.climatetrace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import dev.johnoreilly.climatetrace.di.initKoin
import dev.johnoreilly.climatetrace.ui.CountryListScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initKoin(this)

        setContent {
            MaterialTheme {
                AndroidApp()
            }
        }
    }
}

@Composable
fun AndroidApp() {
    Navigator(screen = CountryListScreen())
}
