import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.johnoreilly.climatetrace.remote.ClimateTraceApi


fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "ClimateTraceKMP") {
        App()
    }
}

//suspend fun main() {
//    val climateTraceApi = ClimateTraceApi()
//    val result = climateTraceApi.fetchCountryAssetEmissionsInfo("FRA")
//    println(result)
//}

@Preview
@Composable
fun AppDesktopPreview() {
    App()
}