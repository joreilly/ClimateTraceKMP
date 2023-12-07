package dev.johnoreilly.climatetrace.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import dev.johnoreilly.climatetrace.remote.Asset
import dev.johnoreilly.climatetrace.remote.ClimateTraceApi


@Composable
fun ClimateTraceSreen() {
    var assetList by remember { mutableStateOf(emptyList<Asset>()) }
    var selectedAsset by remember { mutableStateOf<Asset?>(null) }

    val climateTraceApi =  ClimateTraceApi()

    LaunchedEffect(true) {
        assetList = climateTraceApi.fetchAssets().assets
    }

    Row(Modifier.fillMaxSize()) {

        Box(Modifier.width(250.dp).fillMaxHeight().background(color = Color.LightGray)) {
            AssetListView(assetList, selectedAsset) {
                selectedAsset = it
            }
        }

        Spacer(modifier = Modifier.width(1.dp).fillMaxHeight())

        Box(Modifier.fillMaxHeight()) {
            selectedAsset?.let {
                AssetDetailsView(it)
            }
        }
    }
}

@Composable
fun AssetListView(
    assetList: List<Asset>,
    selectedAsset: Asset?,
    assetSelected: (asset: Asset) -> Unit
) {

    // workaround for compose desktop but if LazyColumn is empty
    if (assetList.isNotEmpty()) {
        LazyColumn {
            items(assetList) { asset ->
                AssetRow(asset, selectedAsset, assetSelected)
            }
        }
    }
}




@Composable
fun AssetRow(
    asset: Asset,
    selectedAsset: Asset?,
    assetSelected: (asset: Asset) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = { assetSelected(asset) })
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column {
            Text(
                asset.name,
                style = if (asset.name == selectedAsset?.name) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge
            )

            Text(text = asset.assetType, style = TextStyle(color = Color.DarkGray, fontSize = 14.sp))
        }
    }
}

@Composable
fun AssetDetailsView(asset: Asset) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.size(12.dp))

        Text(
            asset.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp)
        )
    }
}
