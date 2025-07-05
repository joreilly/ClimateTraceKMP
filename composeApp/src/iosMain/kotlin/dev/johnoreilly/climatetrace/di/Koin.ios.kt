package dev.johnoreilly.climatetrace.di

import dev.johnoreilly.climatetrace.remote.Country
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import io.github.xxfast.kstore.utils.ExperimentalKStoreApi
import kotlinx.io.files.Path
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

// called by iOS etc
fun initKoin() = initKoin(enableNetworkLogs = false) {}

@OptIn(ExperimentalKStoreApi::class)
actual fun dataModule(): Module = module {
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
