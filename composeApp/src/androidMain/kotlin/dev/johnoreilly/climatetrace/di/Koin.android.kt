package dev.johnoreilly.climatetrace.di

import android.content.Context
import dev.johnoreilly.climatetrace.agent.A2uiRenderer
import dev.johnoreilly.climatetrace.agent.ComposeA2uiRenderer
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import dev.johnoreilly.climatetrace.remote.Country
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import kotlinx.io.files.Path
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

fun initKoin(context: Context) = initKoin(enableNetworkLogs = false) {
    androidContext(context)
}

internal actual fun dataModule(): Module = module {
    single<A2uiRenderer> { ComposeA2uiRenderer(A2uiBasicCatalogV1.Functions(androidContext())) }
    single<KStore<List<Country>>> {
        val filesDir: String = androidContext().filesDir.path
        storeOf(file = Path(path = "$filesDir/countries.json"), default = emptyList())
    }
}
