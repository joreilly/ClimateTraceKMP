import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.johnoreilly.climatetrace.data.ClimateTraceRepository
import dev.johnoreilly.climatetrace.di.initKoin

/*
val koin = initKoin(enableNetworkLogs = true).koin

suspend fun main() {
    println("hello")

    val climateTraceRepository = koin.get<ClimateTraceRepository>()

    val agent: ClimateTraceAgent = ClimateTraceAgent(climateTraceRepository)

    //agent.runAgent("What were the emissions for the UK in 2024?")

    agent.runAgent("compare the per-capita emissions of the UK and France in 2024")
}
*/

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "ClimateTraceKMP") {
        App()
    }
}

@Preview
@Composable
fun AppDesktopPreview() {
    App()
}

