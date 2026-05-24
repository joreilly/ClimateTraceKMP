package dev.johnoreilly.climatetrace.di

import android.content.Context
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

actual fun dataModule(): Module = module {
    single<KStore<List<Country>>> {
        val filesDir: String = androidContext().filesDir.path
        storeOf(file = Path(path = "$filesDir/countries.json"), default = emptyList())
    }
}
