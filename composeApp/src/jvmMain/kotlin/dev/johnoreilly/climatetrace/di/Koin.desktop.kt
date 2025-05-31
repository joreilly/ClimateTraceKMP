package dev.johnoreilly.climatetrace.di

import dev.johnoreilly.climatetrace.remote.Country
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import io.github.xxfast.kstore.file.utils.FILE_SYSTEM
import net.harawata.appdirs.AppDirsFactory
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module

private const val PACKAGE_NAME = "dev.johnoreilly.climatetrace"
private const val VERSION = "1.0.0"
private const val AUTHOR = "johnoreilly"

actual fun dataModule(): Module = module {
    single<KStore<List<Country>>> {
        val filesDir: String = AppDirsFactory.getInstance()
            .getUserDataDir(PACKAGE_NAME, VERSION, AUTHOR)

        FILE_SYSTEM.createDirectories(filesDir.toPath())

        storeOf(file = "$filesDir/countries.json".toPath(), default = emptyList())
    }
}
