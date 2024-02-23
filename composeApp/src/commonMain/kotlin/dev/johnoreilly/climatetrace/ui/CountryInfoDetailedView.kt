package dev.johnoreilly.climatetrace.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.remote.CountryAssetEmissionsInfo
import dev.johnoreilly.climatetrace.remote.CountryEmissionsInfo

@Composable
fun CountryInfoDetailedView(
    country: Country?,
    countryEmissionInfo: CountryEmissionsInfo?,
    countryAssetEmissionsList: List<CountryAssetEmissionsInfo>?,
    isLoading: Boolean
) {
    when {
        country == null -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .fillMaxHeight()
                    .wrapContentSize(Alignment.Center)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .fillMaxHeight()
                        .wrapContentSize(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "No Country Selected.", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        else -> {
            if (isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .fillMaxHeight()
                        .wrapContentSize(Alignment.Center)
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (countryEmissionInfo != null && countryAssetEmissionsList != null) {
                        Text(
                            text = country.name,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.size(16.dp))

                        val co2 = (countryEmissionInfo.emissions.co2 / 1_000_000).toInt()
                        val percentage = (countryEmissionInfo.emissions.co2 / countryEmissionInfo.worldEmissions.co2).toPercent(2)

                        Text(text = "co2 = $co2 Million Tonnes (2022)")
                        Text(text = "rank = ${countryEmissionInfo.rank} ($percentage)")

                        Spacer(modifier = Modifier.size(16.dp))

                        CountryAssetEmissionsInfoTreeMapChart(countryAssetEmissionsList)
                        Spacer(modifier = Modifier.size(16.dp))

                        SectorEmissionsPieChart(countryAssetEmissionsList)
                    }
                }
            }
        }
    }
}

