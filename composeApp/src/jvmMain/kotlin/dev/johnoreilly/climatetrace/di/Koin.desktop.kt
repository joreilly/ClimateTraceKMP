package dev.johnoreilly.climatetrace.di

import dev.johnoreilly.climatetrace.remote.Country
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import kotlinx.io.files.Path
import net.harawata.appdirs.AppDirsFactory
import org.koin.core.module.Module
import org.koin.dsl.module

private const val PACKAGE_NAME = "dev.johnoreilly.climatetrace"
private const val VERSION = "1.0.0"
private const val AUTHOR = "johnoreilly"

actual fun dataModule(): Module = module {
    single<KStore<List<Country>>> {
        val filesDir: String = AppDirsFactory.getInstance()
            .getUserDataDir(PACKAGE_NAME, VERSION, AUTHOR)
        storeOf(file = Path(path = "$filesDir/countries.json"), default = emptyList())
    }
}
