package dev.johnoreilly.climatetrace.ui.agui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import dev.johnoreilly.climatetrace.ui.AgentScreenContent

/**
 * The same chat UI as [dev.johnoreilly.climatetrace.ui.AgentScreen], but talking to the ADK agent
 * over the [AG-UI protocol](https://docs.ag-ui.com) instead of running a Koog agent in-process.
 *
 * Start the endpoint first (see the README):
 *
 *     ./gradlew :mcp-server:run --args="--streamable-http-server 8080"
 *     ./gradlew :agents:startAgUiServer
 *
 * This screen lives in `nonWebMain` rather than `commonMain` because the AG-UI client SDK has no
 * wasmJs artifact.
 */
@Composable
fun AgUiAgentScreen(url: String = defaultAgUiUrl()) {
    val scope = rememberCoroutineScope()
    val controller = remember(url) { AgUiChatController(url, scope) }
    DisposableEffect(controller) { onDispose { controller.close() } }

    val uiState by controller.uiState.collectAsState()

    AgentScreenContent(
        messages = uiState.messages,
        inputText = uiState.inputText,
        isInputEnabled = uiState.isInputEnabled,
        isLoading = uiState.isLoading,
        isChatEnded = uiState.isChatEnded,
        onInputTextChanged = controller::updateInputText,
        onSendClicked = controller::sendMessage,
        onRestartClicked = controller::restart,
    )
}

/**
 * Where the AG-UI server is reachable from this platform. The Android emulator reaches the host
 * through 10.0.2.2; a real device needs the host's LAN address instead.
 */
internal expect fun defaultAgUiUrl(): String
