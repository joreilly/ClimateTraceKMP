import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import dev.johnoreilly.climatetrace.di.commonModule
import dev.johnoreilly.climatetrace.ui.ClimateTraceScreen
import org.koin.compose.KoinApplication


@Composable
fun App() {
    KoinApplication(application = {
        modules(commonModule())
    }) {
        MaterialTheme {
            Navigator(screen = ClimateTraceScreen())
        }
    }
}