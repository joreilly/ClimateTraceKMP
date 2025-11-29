import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import dev.johnoreilly.climatetrace.di.commonModule
import dev.johnoreilly.climatetrace.ui.AgentScreen
import dev.johnoreilly.climatetrace.ui.ClimateTraceScreen
import dev.johnoreilly.climatetrace.ui.theme.ClimateTraceTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinApplication


@Preview
@Composable
fun App() {
    KoinApplication(application = {
        modules(commonModule())
    }) {
        ClimateTraceTheme {
            var selectedIndex by remember { mutableIntStateOf(0) }

            Scaffold(
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = selectedIndex == 0,
                            onClick = { selectedIndex = 0 },
                            icon = { Icon(Icons.Default.Public, contentDescription = "Climate") },
                            label = { Text("Climate") }
                        )
                        NavigationBarItem(
                            selected = selectedIndex == 1,
                            onClick = { selectedIndex = 1 },
                            icon = { Icon(Icons.Default.AccountTree, contentDescription = "Agents") },
                            label = { Text("Agent") }
                        )
                    }
                }
            ) { paddingValues ->
                Column(Modifier.padding(paddingValues)) {
                    when (selectedIndex) {
                        0 -> Navigator(screen = ClimateTraceScreen())
                        else -> AgentScreen()
                    }
                }
            }
        }
    }
}