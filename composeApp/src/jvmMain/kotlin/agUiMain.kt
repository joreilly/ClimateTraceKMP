import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.johnoreilly.climatetrace.ui.agui.AgUiAgentScreen
import dev.johnoreilly.climatetrace.ui.theme.ClimateTraceTheme

/**
 * Desktop entry point for the AG-UI agent screen, run with `./gradlew :composeApp:runAgUiDesktop`.
 *
 * It's a separate window from [main] because the main `App()` composable lives in commonMain, which
 * can't reference the AG-UI client (no wasmJs artifact — see the `nonWeb` source set).
 */
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "ClimateTrace — AG-UI Agent") {
        ClimateTraceTheme {
            AgUiAgentScreen()
        }
    }
}
