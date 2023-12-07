import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.johnoreilly.climatetrace.ui.ClimateTraceSreen


@Composable
fun App() {
    MaterialTheme {
        Column(Modifier.fillMaxWidth()) {
            ClimateTraceSreen()
        }
    }
}