package dev.johnoreilly.climatetrace.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import dev.johnoreilly.climatetrace.data.ClimateTraceRepository
import dev.johnoreilly.climatetrace.remote.AssetDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


sealed class AssetDetailUIState {
    data object Loading : AssetDetailUIState()
    data class Error(val message: String) : AssetDetailUIState()
    data class Success(val assetDetail: AssetDetail) : AssetDetailUIState()
}

sealed interface AssetDetailEvents {
    data class LoadAsset(val sourceId: Int) : AssetDetailEvents
}

open class AssetDetailViewModel : ViewModel(), KoinComponent {
    private val climateTraceRepository: ClimateTraceRepository by inject()

    private val events = MutableSharedFlow<AssetDetailEvents>(extraBufferCapacity = 20)

    val viewState: StateFlow<AssetDetailUIState> = viewModelScope.coroutineScope.launchMolecule(mode = RecompositionMode.Immediate) {
        AssetDetailPresenter(events)
    }

    fun loadAsset(sourceId: Int) {
        events.tryEmit(AssetDetailEvents.LoadAsset(sourceId))
    }

    @Composable
    fun AssetDetailPresenter(events: Flow<AssetDetailEvents>): AssetDetailUIState {
        var uiState by remember { mutableStateOf<AssetDetailUIState>(AssetDetailUIState.Loading) }
        var currentSourceId by remember { mutableStateOf<Int?>(null) }

        LaunchedEffect(Unit) {
            events.collect { event ->
                when (event) {
                    is AssetDetailEvents.LoadAsset -> currentSourceId = event.sourceId
                }
            }
        }

        LaunchedEffect(currentSourceId) {
            currentSourceId?.let { sourceId ->
                uiState = AssetDetailUIState.Loading
                try {
                    val assetDetail = climateTraceRepository.fetchAssetDetail(sourceId)
                    uiState = AssetDetailUIState.Success(assetDetail)
                } catch (e: Exception) {
                    uiState = AssetDetailUIState.Error("Error retrieving asset details: ${e.message}")
                }
            }
        }

        return uiState
    }
}
