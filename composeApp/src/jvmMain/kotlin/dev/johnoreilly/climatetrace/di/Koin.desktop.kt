package dev.johnoreilly.climatetrace.di

import dev.johnoreilly.climatetrace.agent.A2uiRenderer
import ai.littlepress.a2ui.compat.android.content.Context
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import dev.johnoreilly.climatetrace.agent.ComposeA2uiRenderer
import dev.johnoreilly.climatetrace.remote.Country
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import net.harawata.appdirs.AppDirsFactory
import org.koin.core.module.Module
import org.koin.dsl.module

private const val PACKAGE_NAME = "dev.johnoreilly.climatetrace"
private const val VERSION = "2.0.0"
private const val AUTHOR = "johnoreilly"

internal actual fun dataModule(): Module = module {
    // a2ui-kmp's Context opens URLs with the platform.
    single<A2uiRenderer> { ComposeA2uiRenderer(A2uiBasicCatalogV1.Functions(Context())) }
    single<KStore<List<Country>>> {
        val filesDir: String = AppDirsFactory.getInstance()
            .getUserCacheDir(PACKAGE_NAME, VERSION, AUTHOR)
        val files = Path(filesDir)
        with(SystemFileSystem) { if(!exists(files)) createDirectories(files) }
        storeOf(file = Path(path = "$filesDir/countries.json"), default = emptyList())
    }
}
