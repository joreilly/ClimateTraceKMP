package dev.johnoreilly.climatetrace.screen

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class MainScreenTest {

    @Test
    fun myFirstTest() = runComposeUiTest {
        setContent {
            Text("hi")
        }

        onNodeWithText("hi").assertExists()
    }
}

