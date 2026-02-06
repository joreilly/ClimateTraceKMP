package dev.johnoreilly.climatetrace.di

import dev.johnoreilly.climatetrace.agent.AgentProvider
import dev.johnoreilly.climatetrace.agent.ClimateTraceAgentProvider
import dev.johnoreilly.climatetrace.data.ClimateTraceRepository
import dev.johnoreilly.climatetrace.remote.ClimateTraceApi
import dev.johnoreilly.climatetrace.remote.PopulationApi
import dev.johnoreilly.climatetrace.viewmodel.AgentViewModel
import dev.johnoreilly.climatetrace.viewmodel.AssetDetailViewModel
import dev.johnoreilly.climatetrace.viewmodel.CountryDetailsViewModel
import dev.johnoreilly.climatetrace.viewmodel.CountryListViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun initKoin(enableNetworkLogs: Boolean = false, appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(commonModule(enableNetworkLogs = enableNetworkLogs))
    }

fun commonModule(enableNetworkLogs: Boolean = false) = module {
    single { createJson() }
    single { createHttpClient(get(), enableNetworkLogs = enableNetworkLogs) }
    single { ClimateTraceApi(get()) }
    single { PopulationApi(get()) }
    single { CountryListViewModel() }
    single { CountryDetailsViewModel() }
    factory { AssetDetailViewModel() }
    single { AgentViewModel(get()) }
    single { ClimateTraceRepository(get(), get(), get()) }
    single<AgentProvider> { ClimateTraceAgentProvider(get()) }
    includes(dataModule())
}

expect fun dataModule(): Module

fun createJson() = Json { isLenient = true; ignoreUnknownKeys = true }


expect fun createHttpClientEngine(): HttpClientEngine

fun createHttpClient(json: Json, enableNetworkLogs: Boolean) = HttpClient(createHttpClientEngine()) {
    install(ContentNegotiation) {
        json(json)
    }
    if (enableNetworkLogs) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.NONE
        }
    }
}
