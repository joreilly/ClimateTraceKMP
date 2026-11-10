package dev.johnoreilly.climatetrace.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.johnoreilly.climatetrace.data.ClimateTraceRepository
import dev.johnoreilly.climatetrace.remote.AssetDetail
import dev.johnoreilly.climatetrace.remote.EmissionMeasurement
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


internal sealed class AssetDetailUIState {
    data object Loading : AssetDetailUIState()
    data class Error(val message: String) : AssetDetailUIState()
    data class Success(
        val assetDetail: AssetDetail,
        // Month-by-month emissions for `monthlyYear`; empty when the API has none.
        val monthlyEmissions: List<EmissionMeasurement> = emptyList(),
        val monthlyYear: Int? = null
    ) : AssetDetailUIState()
}

internal sealed interface AssetDetailEvents {
    data class LoadAsset(val sourceId: Int) : AssetDetailEvents
    data object Retry : AssetDetailEvents
}

internal open class AssetDetailViewModel : ViewModel(), KoinComponent {
    private val climateTraceRepository: ClimateTraceRepository by inject()

    private val events = MutableSharedFlow<AssetDetailEvents>(extraBufferCapacity = 20)

    val viewState: StateFlow<AssetDetailUIState> = viewModelScope.launchMolecule(mode = RecompositionMode.Immediate) {
        AssetDetailPresenter(events)
    }

    fun loadAsset(sourceId: Int) {
        events.tryEmit(AssetDetailEvents.LoadAsset(sourceId))
    }

    fun retry() {
        events.tryEmit(AssetDetailEvents.Retry)
    }

    @Composable
    fun AssetDetailPresenter(events: Flow<AssetDetailEvents>): AssetDetailUIState {
        var uiState by remember { mutableStateOf<AssetDetailUIState>(AssetDetailUIState.Loading) }
        var currentSourceId by remember { mutableStateOf<Int?>(null) }
        var retryCount by remember { mutableIntStateOf(0) }

        LaunchedEffect(Unit) {
            events.collect { event ->
                when (event) {
                    is AssetDetailEvents.LoadAsset -> currentSourceId = event.sourceId
                    AssetDetailEvents.Retry -> retryCount++
                }
            }
        }

        LaunchedEffect(currentSourceId, retryCount) {
            currentSourceId?.let { sourceId ->
                uiState = AssetDetailUIState.Loading
                try {
                    val assetDetail = climateTraceRepository.fetchAssetDetail(sourceId)
                    uiState = AssetDetailUIState.Success(assetDetail)
                    // The monthly breakdown is a bonus: show the details now and add it when
                    // (and if) it arrives.
                    val year = assetDetail.emissions?.mapNotNull { it.year }?.maxOrNull()
                    if (year != null) {
                        val monthly = try {
                            climateTraceRepository.fetchAssetMonthlyEmissions(sourceId, year)
                                .filter { it.month != null && it.emissionsQuantity != null }
                                .sortedBy { it.month }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            emptyList()
                        }
                        uiState = AssetDetailUIState.Success(assetDetail, monthly, year)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    uiState = AssetDetailUIState.Error("Error retrieving asset details: ${e.message}")
                }
            }
        }

        return uiState
    }
}
