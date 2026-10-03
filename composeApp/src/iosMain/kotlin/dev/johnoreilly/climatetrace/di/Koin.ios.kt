package dev.johnoreilly.climatetrace.di

import dev.johnoreilly.climatetrace.remote.Country
import dev.johnoreilly.climatetrace.viewmodel.CountryListViewModel
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import io.github.xxfast.kstore.utils.ExperimentalKStoreApi
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import kotlinx.io.files.Path
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

// called by iOS etc
fun initKoin() {
    initKoin(enableNetworkLogs = false) {}
}

// called by iOS (avoids exposing Koin types to Swift)
fun countryListViewModel(): CountryListViewModel = KoinPlatform.getKoin().get()

@OptIn(ExperimentalKStoreApi::class)
internal actual fun dataModule(): Module = module {
    single<KStore<List<Country>>> {
        val filesDir: String? = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            appropriateForURL = null,
            create = false,
            inDomain = NSUserDomainMask,
            error = null
        )?.relativePath
        requireNotNull(filesDir) { "Document directory not found" }
        storeOf(file = Path(path = "$filesDir/countries.json"), default = emptyList())
    }
}
